package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyUtils {
    private val numberFormatLocal = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat {
            return NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }
        }
    }

    /**
     * Rounds a monetary amount to exact 2-decimal precision and eliminates -0.0 / NaN drift.
     */
    fun roundCurrency(amount: Double): Double {
        if (!amount.isFinite()) return 0.0
        val rounded = (amount * 100.0).roundToLong() / 100.0
        return if (rounded == 0.0) 0.0 else rounded
    }

    /**
     * Safely parses user-typed or pasted currency strings (supports commas, spaces, and ৳ prefix).
     */
    fun parseAmountInput(raw: String): Double? {
        val cleaned = raw.trim()
            .replace("৳", "")
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (!parsed.isFinite()) return null
        return roundCurrency(parsed)
    }

    fun formatEditableAmount(amount: Double): String {
        val rounded = roundCurrency(amount)
        if (rounded <= 0.0) return ""
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    fun formatBdt(amount: Double): String {
        val safeAmount = roundCurrency(amount)
        val formatter = numberFormatLocal.get() ?: NumberFormat.getNumberInstance(Locale.US)
        val absFormatted = formatter.format(abs(safeAmount))
        return if (safeAmount < 0.0) "−৳$absFormatted" else "৳$absFormatted"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeDate(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val formattedDate = formatDate(timestamp)
        return when {
            isSameDay(target, today) -> "Today · $formattedDate"
            isSameDay(target, yesterday) -> "Yesterday · $formattedDate"
            else -> formattedDate
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Converts a local epoch timestamp to the UTC midnight millis expected by Material 3 DatePicker.
     */
    fun localMillisToUtcDateMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis
    }

    /**
     * Combines a UTC date selected from Material 3 DatePicker with the local time-of-day from [referenceLocalMillis]
     * so entries on the same date retain natural chronological ordering.
     */
    fun mergeUtcDateWithLocalTime(utcDateMillis: Long, referenceLocalMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDateMillis }
        val ref = Calendar.getInstance().apply { timeInMillis = referenceLocalMillis }
        val merged = Calendar.getInstance().apply {
            set(Calendar.YEAR, utc.get(Calendar.YEAR))
            set(Calendar.MONTH, utc.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, ref.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, ref.get(Calendar.MINUTE))
            set(Calendar.SECOND, ref.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, ref.get(Calendar.MILLISECOND))
        }
        return merged.timeInMillis
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
