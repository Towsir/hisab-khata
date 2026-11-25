package com.example.data.model

import androidx.annotation.Keep
import com.example.ui.components.CurrencyUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val linkedOwnerId: String = "",
    val activeLinkTokenHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isPlaceholderLinkedProfile: Boolean
        get() = displayName == PLACEHOLDER_LINKED_NAME && phoneNumber.isBlank()

    companion object {
        const val PLACEHOLDER_LINKED_NAME = "Linked Device"
    }
}

@Keep
@IgnoreExtraProperties
data class ShopLinkRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val shopName: String = "",
    val expiresAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LinkedDeviceRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val deviceUid: String = "",
    val deviceName: String = "",
    val deviceEmail: String = "",
    val tokenHash: String = "",
    val linkedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GeneratedLinkCode(
    val code: String,
    val email: String,
    val tokenHash: String,
    val expiresAt: Long
)

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "INCOME", // "INCOME" or "EXPENSE"
    val category: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val timestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val totalCredit: Double = 0.0,
    val totalPaid: Double = 0.0,
    val currentDue: Double = 0.0,
    val lastUpdated: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val advanceBalance: Double
        get() = CurrencyUtils.roundCurrency(maxOf(0.0, totalPaid - totalCredit))
}

@Keep
@IgnoreExtraProperties
data class DueEntryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val type: String = "GOODS_TAKEN", // "GOODS_TAKEN", "PAYMENT_RECEIVED", or "ADVANCE_PAYMENT"
    val amount: Double = 0.0,
    val itemsDescription: String = "",
    val timestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
