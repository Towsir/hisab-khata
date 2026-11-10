package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun formatBdt(amount: Double): String {
        return "৳" + numberFormat.format(amount)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = if (monthZeroIndexed in 0..11) months[monthZeroIndexed] else "Month"
        return "$monthName $year"
    }
}
