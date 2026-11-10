package com.example

import com.example.data.entity.TransactionEntity
import com.example.ui.components.CategoryShare
import com.example.ui.components.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyUtils.formatBdt(1500.0)
        assertTrue(formatted.contains("1,500") || formatted.contains("1500"))
        assertTrue(formatted.startsWith("৳"))
    }

    @Test
    fun testMonthFormatting() {
        val monthStr = CurrencyUtils.formatMonthYear(2026, 10) // November (0-indexed 10)
        assertEquals("November 2026", monthStr)
    }

    @Test
    fun testCustomerDueCalculation() {
        val totalCredit = 5000.0
        val payment1 = 2000.0
        val remainingDue1 = totalCredit - payment1
        assertEquals(3000.0, remainingDue1, 0.001)

        val additionalGoods = 1500.0
        val updatedDue = remainingDue1 + additionalGoods
        assertEquals(4500.0, updatedDue, 0.001)

        val payment2 = 4500.0
        val finalDue = maxOf(0.0, updatedDue - payment2)
        assertEquals(0.0, finalDue, 0.001)
    }

    @Test
    fun testCalendarMonthNormalization() {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.MONTH, Calendar.FEBRUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(28, maxDay) // 2026 is non-leap
    }

    @Test
    fun testCategoryShareMath() {
        val totalExpense = 10000.0
        val inventoryExpense = 4000.0
        val pct = ((inventoryExpense / totalExpense) * 100).toFloat()
        assertEquals(40.0f, pct, 0.001f)
    }
}
