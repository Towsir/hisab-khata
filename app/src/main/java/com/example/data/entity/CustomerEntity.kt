package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalCredit: Double = 0.0, // Total value of goods given on credit
    val totalPaid: Double = 0.0,   // Total amount received from customer
    val currentDue: Double = 0.0,  // Outstanding balance (totalCredit - totalPaid)
    val lastUpdated: Long = System.currentTimeMillis()
)
