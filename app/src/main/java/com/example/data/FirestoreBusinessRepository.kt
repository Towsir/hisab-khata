package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.CustomerRecord
import com.example.data.model.DueEntryRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfileRecord
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.ui.components.CurrencyUtils
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
import java.util.Calendar

class FirestoreBusinessRepository(
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
            ?: throw IllegalStateException("Please sign in with Google to sync your ledger.")
    }

    private fun resolveTargetOwnerId(ledgerOwnerId: String?): String {
        val currentUid = requireUserId()
        return ledgerOwnerId?.trim().orEmpty().ifBlank { currentUid }
    }

    private fun validateAmount(amount: Double) {
        require(amount.isFinite() && amount > 0.0) { "Amount must be greater than zero." }
        require(amount <= MAX_AMOUNT) { "Amount cannot exceed ৳1,000,000,000." }
    }

    private fun validateNonNegativeAmount(amount: Double) {
        require(amount.isFinite() && amount >= 0.0) { "Amount cannot be negative." }
        require(amount <= MAX_AMOUNT) { "Amount cannot exceed ৳1,000,000,000." }
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun transactionsCol(userId: String) = userDoc(userId).collection("transactions")
    private fun customersCol(userId: String) = userDoc(userId).collection("customers")
    private fun dueEntriesCol(userId: String, customerId: String) =
        customersCol(userId).document(customerId).collection("dueEntries")

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfileRecord?> = flow {
        val docRef = userDoc(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfileRecord::class.java,
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
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        phoneNumber: String,
        email: String,
        isExistingProfile: Boolean? = null
    ) {
        val uid = requireUserId()
        val cleanName = displayName.trim().take(MAX_NAME_LENGTH)
        val cleanPhone = phoneNumber.trim().take(MAX_PHONE_LENGTH)
        val cleanEmail = email.trim().take(MAX_EMAIL_LENGTH)
        require(cleanName.isNotEmpty()) { "Shop or owner name is required." }

        val docRef = userDoc(uid)
        try {
            val shouldUpdate = isExistingProfile ?: docRef.get().await().exists()
            if (shouldUpdate) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "phoneNumber" to cleanPhone,
                        "email" to cleanEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "phoneNumber" to cleanPhone,
                        "email" to cleanEmail,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    // --- TRANSACTIONS (INCOME & EXPENSE) ---

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        val colRef = transactionsCol(userId)
        emitAll(
            colRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
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

    suspend fun addTransaction(
        type: String,
        category: String,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        ledgerOwnerId: String? = null
    ): String {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanAmount = CurrencyUtils.roundCurrency(amount)
        validateAmount(cleanAmount)
        val cleanCategory = category.trim().take(MAX_CATEGORY_LENGTH)
        val cleanNote = note.trim().take(MAX_NOTE_LENGTH)
        val safeTimestamp = maxOf(0L, timestamp)
        require(cleanCategory.isNotEmpty()) { "Please select a category." }

        val docRef = transactionsCol(uid).document()
        val payload = mapOf(
            "userId" to uid,
            "type" to type,
            "category" to cleanCategory,
            "amount" to cleanAmount,
            "note" to cleanNote,
            "timestamp" to safeTimestamp,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateTransaction(
        transactionId: String,
        type: String,
        category: String,
        amount: Double,
        note: String,
        timestamp: Long,
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        require(transactionId.isNotBlank()) { "Transaction ID must not be blank." }
        val cleanAmount = CurrencyUtils.roundCurrency(amount)
        validateAmount(cleanAmount)
        val cleanCategory = category.trim().take(MAX_CATEGORY_LENGTH)
        val cleanNote = note.trim().take(MAX_NOTE_LENGTH)
        val safeTimestamp = maxOf(0L, timestamp)
        require(cleanCategory.isNotEmpty()) { "Please select a category." }

        val docRef = transactionsCol(uid).document(transactionId)
        try {
            docRef.update(
                mapOf(
                    "type" to type,
                    "category" to cleanCategory,
                    "amount" to cleanAmount,
                    "note" to cleanNote,
                    "timestamp" to safeTimestamp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteTransaction(transactionId: String, ledgerOwnerId: String? = null) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val docRef = transactionsCol(uid).document(transactionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteAllTransactions(typeFilter: String? = null, ledgerOwnerId: String? = null) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val colRef = transactionsCol(uid)
        try {
            val query = if (typeFilter != null) {
                colRef.whereEqualTo("type", typeFilter)
            } else {
                colRef
            }
            val snapshot = query.get().await()
            val docs = snapshot.documents
            for (chunk in docs.chunked(450)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path, auth)
            throw e
        }
    }

    // --- CUSTOMERS & DUE LEDGER (STRICTLY SEPARATE FROM INCOME) ---

    fun observeCustomers(userId: String): Flow<List<CustomerRecord>> = flow {
        val colRef = customersCol(userId)
        emitAll(
            colRef.orderBy("lastUpdated", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomerRecord::class.java,
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

    fun observeCustomerById(userId: String, customerId: String): Flow<CustomerRecord?> = flow {
        val docRef = customersCol(userId).document(customerId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CustomerRecord::class.java,
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
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeEntriesForCustomer(userId: String, customerId: String): Flow<List<DueEntryRecord>> = flow {
        val colRef = dueEntriesCol(userId, customerId)
        emitAll(
            colRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DueEntryRecord::class.java,
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

    suspend fun createCustomer(
        name: String,
        phone: String,
        address: String,
        initialAmount: Double = 0.0,
        initialNote: String = "",
        isInitialAdvance: Boolean = false,
        ledgerOwnerId: String? = null
    ): String {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanName = name.trim().take(MAX_NAME_LENGTH)
        val cleanPhone = phone.trim().take(MAX_PHONE_LENGTH)
        val cleanAddress = address.trim().take(MAX_ADDRESS_LENGTH)
        require(cleanName.isNotEmpty()) { "Customer name is required." }
        val safeAmount = CurrencyUtils.roundCurrency(maxOf(0.0, initialAmount))
        validateNonNegativeAmount(safeAmount)

        val custRef = customersCol(uid).document()
        val now = System.currentTimeMillis()
        val initialCredit = if (isInitialAdvance) 0.0 else safeAmount
        val initialPaid = if (isInitialAdvance) safeAmount else 0.0
        val initialDue = CurrencyUtils.roundCurrency(maxOf(0.0, initialCredit - initialPaid))
        try {
            val batch = db.batch()
            batch.set(
                custRef,
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "phone" to cleanPhone,
                    "address" to cleanAddress,
                    "totalCredit" to initialCredit,
                    "totalPaid" to initialPaid,
                    "currentDue" to initialDue,
                    "lastUpdated" to now,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (safeAmount > 0.0) {
                val entryRef = dueEntriesCol(uid, custRef.id).document()
                val entryType = if (isInitialAdvance) "ADVANCE_PAYMENT" else "GOODS_TAKEN"
                val fallbackDesc = if (isInitialAdvance) "Opening Advance" else "Opening Due Balance"
                val cleanNote = initialNote.trim().take(MAX_NOTE_LENGTH).ifBlank { fallbackDesc }
                batch.set(
                    entryRef,
                    mapOf(
                        "userId" to uid,
                        "customerId" to custRef.id,
                        "type" to entryType,
                        "amount" to safeAmount,
                        "itemsDescription" to cleanNote,
                        "timestamp" to now,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            return custRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, custRef.path, auth)
            throw e
        }
    }

    suspend fun updateCustomerDetails(
        customerId: String,
        name: String,
        phone: String,
        address: String,
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanName = name.trim().take(MAX_NAME_LENGTH)
        val cleanPhone = phone.trim().take(MAX_PHONE_LENGTH)
        val cleanAddress = address.trim().take(MAX_ADDRESS_LENGTH)
        require(cleanName.isNotEmpty()) { "Customer name is required." }

        val custRef = customersCol(uid).document(customerId)
        try {
            custRef.update(
                mapOf(
                    "name" to cleanName,
                    "phone" to cleanPhone,
                    "address" to cleanAddress,
                    "lastUpdated" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, custRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCustomer(customerId: String, ledgerOwnerId: String? = null) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val custRef = customersCol(uid).document(customerId)
        try {
            val entriesSnapshot = dueEntriesCol(uid, customerId).get().await()
            val entryDocs = entriesSnapshot.documents
            for (chunk in entryDocs.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            custRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, custRef.path, auth)
            throw e
        }
    }

    suspend fun addCustomerCredit(
        customerId: String,
        amount: Double,
        itemsDescription: String,
        timestamp: Long = System.currentTimeMillis(),
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanAmount = CurrencyUtils.roundCurrency(amount)
        validateAmount(cleanAmount)
        val cleanDesc = itemsDescription.trim().take(MAX_NOTE_LENGTH).ifBlank { "Goods taken on credit" }
        val custRef = customersCol(uid).document(customerId)
        val entryRef = dueEntriesCol(uid, customerId).document()
        val now = System.currentTimeMillis()
        val safeTimestamp = maxOf(0L, timestamp)
        try {
            val existingEntries = dueEntriesCol(uid, customerId).get().await()
                .toObjects(DueEntryRecord::class.java)
            val newEntry = DueEntryRecord(
                id = entryRef.id,
                userId = uid,
                customerId = customerId,
                type = "GOODS_TAKEN",
                amount = cleanAmount,
                itemsDescription = cleanDesc,
                timestamp = safeTimestamp
            )
            val totals = recalculateCustomerTotals(existingEntries + newEntry)
            validateNonNegativeAmount(totals.totalCredit)

            val batch = db.batch()
            batch.set(
                entryRef,
                mapOf(
                    "userId" to uid,
                    "customerId" to customerId,
                    "type" to "GOODS_TAKEN",
                    "amount" to cleanAmount,
                    "itemsDescription" to cleanDesc,
                    "timestamp" to safeTimestamp,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                custRef,
                mapOf(
                    "totalCredit" to totals.totalCredit,
                    "totalPaid" to totals.totalPaid,
                    "currentDue" to totals.currentDue,
                    "lastUpdated" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, entryRef.path, auth)
            throw e
        }
    }

    /**
     * Records a customer due payment or advance deposit strictly inside the Due Ledger.
     * Per user requirement, due/advance payments NEVER create an Income transaction.
     */
    suspend fun recordCustomerPayment(
        customerId: String,
        amount: Double,
        paymentNote: String,
        isAdvance: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanAmount = CurrencyUtils.roundCurrency(amount)
        validateAmount(cleanAmount)
        val custRef = customersCol(uid).document(customerId)
        val entryRef = dueEntriesCol(uid, customerId).document()
        val now = System.currentTimeMillis()
        val safeTimestamp = maxOf(0L, timestamp)
        val entryType = if (isAdvance) "ADVANCE_PAYMENT" else "PAYMENT_RECEIVED"
        val cleanNote = paymentNote.trim().take(MAX_NOTE_LENGTH).ifBlank {
            if (isAdvance) "Advance Payment" else "Cash Payment"
        }
        try {
            val existingEntries = dueEntriesCol(uid, customerId).get().await()
                .toObjects(DueEntryRecord::class.java)
            val newEntry = DueEntryRecord(
                id = entryRef.id,
                userId = uid,
                customerId = customerId,
                type = entryType,
                amount = cleanAmount,
                itemsDescription = cleanNote,
                timestamp = safeTimestamp
            )
            val totals = recalculateCustomerTotals(existingEntries + newEntry)
            validateNonNegativeAmount(totals.totalPaid)

            val batch = db.batch()
            batch.set(
                entryRef,
                mapOf(
                    "userId" to uid,
                    "customerId" to customerId,
                    "type" to entryType,
                    "amount" to cleanAmount,
                    "itemsDescription" to cleanNote,
                    "timestamp" to safeTimestamp,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                custRef,
                mapOf(
                    "totalCredit" to totals.totalCredit,
                    "totalPaid" to totals.totalPaid,
                    "currentDue" to totals.currentDue,
                    "lastUpdated" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, entryRef.path, auth)
            throw e
        }
    }

    /**
     * Edits any due entry (GOODS_TAKEN, PAYMENT_RECEIVED, or ADVANCE_PAYMENT) and automatically
     * recalculates totalCredit, totalPaid, and currentDue on the parent customer.
     */
    suspend fun updateDueEntry(
        customerId: String,
        entryId: String,
        type: String,
        amount: Double,
        itemsDescription: String,
        timestamp: Long,
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val cleanAmount = CurrencyUtils.roundCurrency(amount)
        validateAmount(cleanAmount)
        val cleanDesc = itemsDescription.trim().take(MAX_NOTE_LENGTH)
        val safeTimestamp = maxOf(0L, timestamp)
        val custRef = customersCol(uid).document(customerId)
        val entryRef = dueEntriesCol(uid, customerId).document(entryId)
        val now = System.currentTimeMillis()
        try {
            val existingEntries = dueEntriesCol(uid, customerId).get().await()
                .toObjects(DueEntryRecord::class.java)
            val updatedList = existingEntries.map { entry ->
                if (entry.id == entryId) {
                    entry.copy(
                        type = type,
                        amount = cleanAmount,
                        itemsDescription = cleanDesc,
                        timestamp = safeTimestamp
                    )
                } else {
                    entry
                }
            }
            val totals = recalculateCustomerTotals(updatedList)
            validateNonNegativeAmount(totals.totalCredit)
            validateNonNegativeAmount(totals.totalPaid)

            val batch = db.batch()
            batch.update(
                entryRef,
                mapOf(
                    "type" to type,
                    "amount" to cleanAmount,
                    "itemsDescription" to cleanDesc,
                    "timestamp" to safeTimestamp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                custRef,
                mapOf(
                    "totalCredit" to totals.totalCredit,
                    "totalPaid" to totals.totalPaid,
                    "currentDue" to totals.currentDue,
                    "lastUpdated" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, entryRef.path, auth)
            throw e
        }
    }

    /**
     * Deletes a due entry and recalculates the parent customer's credit/paid/due totals.
     */
    suspend fun deleteDueEntry(
        customerId: String,
        entryId: String,
        ledgerOwnerId: String? = null
    ) {
        val uid = resolveTargetOwnerId(ledgerOwnerId)
        val custRef = customersCol(uid).document(customerId)
        val entryRef = dueEntriesCol(uid, customerId).document(entryId)
        val now = System.currentTimeMillis()
        try {
            val existingEntries = dueEntriesCol(uid, customerId).get().await()
                .toObjects(DueEntryRecord::class.java)
            val remainingList = existingEntries.filterNot { it.id == entryId }
            val totals = recalculateCustomerTotals(remainingList)

            val batch = db.batch()
            batch.delete(entryRef)
            batch.update(
                custRef,
                mapOf(
                    "totalCredit" to totals.totalCredit,
                    "totalPaid" to totals.totalPaid,
                    "currentDue" to totals.currentDue,
                    "lastUpdated" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, entryRef.path, auth)
            throw e
        }
    }

    companion object {
        const val MAX_AMOUNT = 1_000_000_000.0
        const val MAX_NAME_LENGTH = 80
        const val MAX_PHONE_LENGTH = 30
        const val MAX_EMAIL_LENGTH = 120
        const val MAX_ADDRESS_LENGTH = 150
        const val MAX_CATEGORY_LENGTH = 60
        const val MAX_NOTE_LENGTH = 300

        data class CustomerTotals(
            val totalCredit: Double,
            val totalPaid: Double,
            val currentDue: Double
        )

        fun recalculateCustomerTotals(entries: List<DueEntryRecord>): CustomerTotals {
            val credit = CurrencyUtils.roundCurrency(
                entries.filter { it.type == "GOODS_TAKEN" }.sumOf { it.amount }
            )
            val paid = CurrencyUtils.roundCurrency(
                entries.filter {
                    it.type == "PAYMENT_RECEIVED" || it.type == "ADVANCE_PAYMENT"
                }.sumOf { it.amount }
            )
            val due = CurrencyUtils.roundCurrency(maxOf(0.0, credit - paid))
            return CustomerTotals(totalCredit = credit, totalPaid = paid, currentDue = due)
        }

        fun filterTransactionsForMonth(
            transactions: List<TransactionRecord>,
            year: Int,
            monthZeroIndexed: Int
        ): List<TransactionRecord> {
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthZeroIndexed)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endTime = calendar.timeInMillis

            return transactions.filter { it.timestamp in startTime..endTime }
        }
    }
}
