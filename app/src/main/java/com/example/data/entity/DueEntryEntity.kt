package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "due_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class DueEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val type: String, // "GOODS_TAKEN" or "PAYMENT_RECEIVED"
    val amount: Double,
    val itemsDescription: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
