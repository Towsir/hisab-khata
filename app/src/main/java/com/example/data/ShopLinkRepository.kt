package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.GeneratedLinkCode
import com.example.data.model.LinkedDeviceRecord
import com.example.data.model.ShopLinkRecord
import com.example.data.model.UserProfileRecord
import com.example.data.util.OperationType
import com.example.data.util.ShopLinkCrypto
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Handles zero-PII multi-device shop linking via SHA-256(email:6-digit-code) tokens
 * and manages authorized secondary devices under /users/{ownerId}/linkedDevices/{deviceUid}.
 */
class ShopLinkRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to continue.")
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun shopLinksCol() = db.collection("shopLinks")
    private fun linkedDevicesCol(ownerId: String) = userDoc(ownerId).collection("linkedDevices")

    fun observeLinkedDevices(ownerId: String): Flow<List<LinkedDeviceRecord>> = flow {
        val colRef = linkedDevicesCol(ownerId)
        emitAll(
            colRef.orderBy("linkedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LinkedDeviceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeLinkedDeviceMembership(ownerId: String, deviceUid: String): Flow<LinkedDeviceRecord?> = flow {
        val docRef = linkedDevicesCol(ownerId).document(deviceUid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LinkedDeviceRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    // If permission denied or missing, surface null so ViewModel cleanly recovers.
                    if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        emit(null)
                    } else {
                        throw error
                    }
                }
        )
    }

    /**
     * Generates a fresh 6-digit link code valid for 15 minutes, invalidating any prior active
     * code for this shop owner. Only the SHA-256 hash of (email:code) is persisted in Firestore.
     */
    suspend fun generateOrRotateShopLinkCode(
        shopName: String,
        ownerEmail: String,
        previousTokenHash: String? = null
    ): GeneratedLinkCode {
        val uid = requireUserId()
        val cleanShopName = shopName.trim().take(FirestoreBusinessRepository.MAX_NAME_LENGTH)
            .ifBlank { "Hishab Khata" }
        val cleanEmail = ShopLinkCrypto.normalizeEmail(ownerEmail)
        require(ShopLinkCrypto.isValidEmail(cleanEmail)) {
            "Add a valid email address to your shop profile before generating a link code."
        }

        val code = ShopLinkCrypto.generateSixDigitCode()
        val tokenHash = ShopLinkCrypto.computeTokenHash(cleanEmail, code)
        val expiresAt = System.currentTimeMillis() + ShopLinkCrypto.LINK_CODE_TTL_MILLIS

        val newLinkRef = shopLinksCol().document(tokenHash)
        try {
            if (!previousTokenHash.isNullOrBlank() && previousTokenHash != tokenHash) {
                try {
                    shopLinksCol().document(previousTokenHash).delete().await()
                } catch (_: Exception) {
                    // Best-effort cleanup of old token hash
                }
            }

            newLinkRef.set(
                mapOf(
                    "ownerId" to uid,
                    "shopName" to cleanShopName,
                    "expiresAt" to expiresAt,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            userDoc(uid).update(
                mapOf(
                    "activeLinkTokenHash" to tokenHash,
                    "email" to cleanEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            return GeneratedLinkCode(
                code = code,
                email = cleanEmail,
                tokenHash = tokenHash,
                expiresAt = expiresAt
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, newLinkRef.path, auth)
            throw e
        }
    }

    /**
     * Verifies an existing shop's email + 6-digit code, registers this device under the shop's
     * linkedDevices subcollection, and saves linkedOwnerId on this user's own profile.
     *
     * @return The display name of the linked shop.
     */
    suspend fun linkDeviceByEmailAndCode(
        ownerEmail: String,
        code: String,
        deviceName: String = ShopLinkCrypto.resolveDeviceDisplayName()
    ): String {
        val currentUid = requireUserId()
        val currentEmail = auth.currentUser?.email.orEmpty().trim().take(FirestoreBusinessRepository.MAX_EMAIL_LENGTH)
        val cleanDeviceName = deviceName.trim()
            .take(ShopLinkCrypto.MAX_DEVICE_NAME_LENGTH)
            .ifBlank { "Android Device" }

        val tokenHash = ShopLinkCrypto.computeTokenHash(ownerEmail, code)
        val linkRef = shopLinksCol().document(tokenHash)

        try {
            val linkSnap = linkRef.get().await()
            if (!linkSnap.exists()) {
                throw IllegalArgumentException(
                    "No active shop found for that email and 6-digit code. Check both and try again."
                )
            }
            val linkRecord = linkSnap.toObject(ShopLinkRecord::class.java)
                ?: throw IllegalArgumentException("Invalid shop link record. Please request a fresh code.")

            if (linkRecord.expiresAt <= System.currentTimeMillis()) {
                throw IllegalArgumentException(
                    "This 6-digit code has expired. Ask the shop owner to rotate a new code."
                )
            }
            if (linkRecord.ownerId == currentUid) {
                throw IllegalArgumentException(
                    "This email already belongs to the Google account signed in on this device."
                )
            }

            val ownerId = linkRecord.ownerId
            val now = System.currentTimeMillis()
            val linkedDeviceRef = linkedDevicesCol(ownerId).document(currentUid)

            val existingDeviceSnap = linkedDeviceRef.get().await()
            if (existingDeviceSnap.exists()) {
                linkedDeviceRef.update(
                    mapOf(
                        "deviceName" to cleanDeviceName,
                        "deviceEmail" to currentEmail,
                        "tokenHash" to tokenHash,
                        "linkedAt" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                linkedDeviceRef.set(
                    mapOf(
                        "ownerId" to ownerId,
                        "deviceUid" to currentUid,
                        "deviceName" to cleanDeviceName,
                        "deviceEmail" to currentEmail,
                        "tokenHash" to tokenHash,
                        "linkedAt" to now,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val ownDocRef = userDoc(currentUid)
            val ownSnap = ownDocRef.get().await()
            if (ownSnap.exists()) {
                val previousOwnerId = ownSnap.getString("linkedOwnerId").orEmpty()
                if (previousOwnerId.isNotBlank() && previousOwnerId != ownerId) {
                    try {
                        linkedDevicesCol(previousOwnerId).document(currentUid).delete().await()
                    } catch (_: Exception) {
                        // Best-effort cleanup of previous shop link
                    }
                }
                ownDocRef.update(
                    mapOf(
                        "linkedOwnerId" to ownerId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ownDocRef.set(
                    mapOf(
                        "userId" to currentUid,
                        "displayName" to UserProfileRecord.PLACEHOLDER_LINKED_NAME,
                        "phoneNumber" to auth.currentUser?.phoneNumber.orEmpty()
                            .take(FirestoreBusinessRepository.MAX_PHONE_LENGTH),
                        "email" to currentEmail,
                        "linkedOwnerId" to ownerId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            return linkRecord.shopName.ifBlank { "Existing Shop" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, linkRef.path, auth)
            throw e
        }
    }

    /**
     * Primary owner instantly revokes a linked secondary device.
     */
    suspend fun unlinkDevice(deviceUid: String) {
        val ownerId = requireUserId()
        require(deviceUid.isNotBlank()) { "Device ID must not be blank." }
        val docRef = linkedDevicesCol(ownerId).document(deviceUid)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    /**
     * Secondary device disconnects itself from a linked shop and clears its local pointer.
     */
    suspend fun disconnectFromLinkedShop(linkedOwnerId: String) {
        val currentUid = requireUserId()
        if (linkedOwnerId.isNotBlank()) {
            try {
                linkedDevicesCol(linkedOwnerId).document(currentUid).delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Owner may already have revoked access; proceed to clear local pointer.
            }
        }
        clearLocalLinkedOwnerPointer()
    }

    /**
     * Clears linkedOwnerId on the current user's own profile document.
     */
    suspend fun clearLocalLinkedOwnerPointer() {
        val currentUid = requireUserId()
        val ownDocRef = userDoc(currentUid)
        try {
            val snap = ownDocRef.get().await()
            if (snap.exists() && !snap.getString("linkedOwnerId").isNullOrBlank()) {
                ownDocRef.update(
                    mapOf(
                        "linkedOwnerId" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ownDocRef.path, auth)
            throw e
        }
    }
}
