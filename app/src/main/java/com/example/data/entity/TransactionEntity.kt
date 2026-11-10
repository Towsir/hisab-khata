package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Sales", "Services", "Inventory", "Rent", "Utilities", "Salary", "Transport", "Misc"
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
