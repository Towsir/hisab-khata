package com.example

import com.example.auth.formatBangladeshiPhoneDisplay
import com.example.auth.normalizeBangladeshiPhone
import com.example.data.FirestoreBusinessRepository
import com.example.data.model.CustomerRecord
import com.example.data.model.DueEntryRecord
import com.example.data.model.TransactionRecord
import com.example.data.util.ShopLinkCrypto
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.CustomerMessageFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
    @Test
    fun testBangladeshiPhoneNormalization() {
        assertEquals("+8801712345678", normalizeBangladeshiPhone("01712345678"))
        assertEquals("+8801712345678", normalizeBangladeshiPhone("1712345678"))
        assertEquals("+8801812345678", normalizeBangladeshiPhone("01812-345 678"))
        assertEquals("+8801912345678", normalizeBangladeshiPhone("+8801912345678"))
        assertEquals("+8801712345678", normalizeBangladeshiPhone("+88001712345678"))
        assertEquals("+8801612345678", normalizeBangladeshiPhone("8801612345678"))
        assertEquals("+880 1712-345678", formatBangladeshiPhoneDisplay("01712345678"))
        assertNull(normalizeBangladeshiPhone("01112345678")) // Invalid operator prefix
        assertNull(normalizeBangladeshiPhone("0171234"))     // Too short
        assertNull(normalizeBangladeshiPhone(""))
    }

    @Test
    fun testDatePickerUtcAndLocalMergePreservesDateAndTime() {
        val refCal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 20, 14, 35, 0)
        }
        val pickedUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0)
        }.timeInMillis

        val merged = CurrencyUtils.mergeUtcDateWithLocalTime(pickedUtc, refCal.timeInMillis)
        val mergedCal = Calendar.getInstance().apply { timeInMillis = merged }
        assertEquals(2026, mergedCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, mergedCal.get(Calendar.MONTH))
        assertEquals(5, mergedCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, mergedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, mergedCal.get(Calendar.MINUTE))

        val backToUtc = CurrencyUtils.localMillisToUtcDateMillis(merged)
        assertEquals(pickedUtc, backToUtc)
    }

    @Test
    fun testRelativeDateFormatting() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 20, 15, 0, 0)
        }.timeInMillis
        val yesterday = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 19, 9, 0, 0)
        }.timeInMillis
        assertTrue(CurrencyUtils.formatRelativeDate(now, now).startsWith("Today · "))
        assertTrue(CurrencyUtils.formatRelativeDate(yesterday, now).startsWith("Yesterday · "))
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyUtils.formatBdt(1500.0)
        assertTrue(formatted.contains("1,500") || formatted.contains("1500"))
        assertTrue(formatted.startsWith("৳"))
    }

    @Test
    fun testMonthFormatting() {
        val monthStr = CurrencyUtils.formatMonthYear(2026, 10)
        assertEquals("November 2026", monthStr)
    }

    @Test
    fun testRecalculateCustomerTotalsAfterEditsAndDeletes() {
        val entries = listOf(
            DueEntryRecord(id = "e1", type = "GOODS_TAKEN", amount = 5000.0),
            DueEntryRecord(id = "e2", type = "GOODS_TAKEN", amount = 1500.0),
            DueEntryRecord(id = "e3", type = "PAYMENT_RECEIVED", amount = 2000.0),
            DueEntryRecord(id = "e4", type = "PAYMENT_RECEIVED", amount = 500.0)
        )
        val totals = FirestoreBusinessRepository.recalculateCustomerTotals(entries)
        assertEquals(6500.0, totals.totalCredit, 0.001)
        assertEquals(2500.0, totals.totalPaid, 0.001)
        assertEquals(4000.0, totals.currentDue, 0.001)
    }

    @Test
    fun testRecalculateCustomerTotalsNeverNegativeDue() {
        val entries = listOf(
            DueEntryRecord(id = "e1", type = "GOODS_TAKEN", amount = 1000.0),
            DueEntryRecord(id = "e2", type = "PAYMENT_RECEIVED", amount = 1500.0)
        )
        val totals = FirestoreBusinessRepository.recalculateCustomerTotals(entries)
        assertEquals(1000.0, totals.totalCredit, 0.001)
        assertEquals(1500.0, totals.totalPaid, 0.001)
        assertEquals(0.0, totals.currentDue, 0.001)
    }

    @Test
    fun testRecalculateCustomerTotalsWithAdvancePaymentAndAdvanceBalance() {
        val entries = listOf(
            DueEntryRecord(id = "e1", type = "ADVANCE_PAYMENT", amount = 3000.0),
            DueEntryRecord(id = "e2", type = "GOODS_TAKEN", amount = 1200.0),
            DueEntryRecord(id = "e3", type = "PAYMENT_RECEIVED", amount = 200.0)
        )
        val totals = FirestoreBusinessRepository.recalculateCustomerTotals(entries)
        assertEquals(1200.0, totals.totalCredit, 0.001)
        assertEquals(3200.0, totals.totalPaid, 0.001)
        assertEquals(0.0, totals.currentDue, 0.001)

        val customer = CustomerRecord(
            id = "c1",
            totalCredit = totals.totalCredit,
            totalPaid = totals.totalPaid,
            currentDue = totals.currentDue
        )
        assertEquals(2000.0, customer.advanceBalance, 0.001)
    }

    @Test
    fun testFilterTransactionsForMonth() {
        val novCal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 14, 10, 0)
        }
        val decCal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.DECEMBER, 1, 9, 0)
        }
        val list = listOf(
            TransactionRecord(id = "t1", type = "INCOME", amount = 1200.0, timestamp = novCal.timeInMillis),
            TransactionRecord(id = "t2", type = "EXPENSE", amount = 400.0, timestamp = decCal.timeInMillis)
        )
        val novOnly = FirestoreBusinessRepository.filterTransactionsForMonth(list, 2026, Calendar.NOVEMBER)
        assertEquals(1, novOnly.size)
        assertEquals("t1", novOnly.first().id)
    }

    @Test
    fun testNegativeAndEdgeCurrencyFormatting() {
        val negative = CurrencyUtils.formatBdt(-500.0)
        assertEquals("−৳500", negative)

        val negativeDecimal = CurrencyUtils.formatBdt(-1250.5)
        assertTrue(negativeDecimal.startsWith("−৳"))
        assertTrue(negativeDecimal.contains("1,250.5") || negativeDecimal.contains("1250.5"))

        assertEquals("৳0", CurrencyUtils.formatBdt(-0.0))
        assertEquals("৳0", CurrencyUtils.formatBdt(Double.NaN))
        assertEquals("৳0", CurrencyUtils.formatBdt(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testParseAmountInputAndEditableFormatting() {
        assertEquals(1500.0, CurrencyUtils.parseAmountInput("1,500")!!, 0.001)
        assertEquals(2450.5, CurrencyUtils.parseAmountInput("৳ 2,450.50")!!, 0.001)
        assertEquals(99.25, CurrencyUtils.parseAmountInput("  99.25 ")!!, 0.001)
        assertNull(CurrencyUtils.parseAmountInput(""))
        assertNull(CurrencyUtils.parseAmountInput("abc"))
        assertNull(CurrencyUtils.parseAmountInput("৳"))

        assertEquals("500", CurrencyUtils.formatEditableAmount(500.0))
        assertEquals("125.75", CurrencyUtils.formatEditableAmount(125.75))
        assertEquals("", CurrencyUtils.formatEditableAmount(0.0))
    }

    @Test
    fun testFloatingPointDriftRoundingInDueLedger() {
        val entries = listOf(
            DueEntryRecord(id = "e1", type = "GOODS_TAKEN", amount = 100.10),
            DueEntryRecord(id = "e2", type = "GOODS_TAKEN", amount = 100.20),
            DueEntryRecord(id = "e3", type = "PAYMENT_RECEIVED", amount = 200.30)
        )
        val totals = FirestoreBusinessRepository.recalculateCustomerTotals(entries)
        assertEquals(200.30, totals.totalCredit, 0.0)
        assertEquals(200.30, totals.totalPaid, 0.0)
        assertEquals(0.0, totals.currentDue, 0.0)

        val customer = CustomerRecord(
            id = "c_drift",
            totalCredit = totals.totalCredit,
            totalPaid = totals.totalPaid,
            currentDue = totals.currentDue
        )
        assertEquals(0.0, customer.advanceBalance, 0.0)
    }

    @Test
    fun testShopLinkTokenHashNormalizationAndDeterminism() {
        val hash1 = ShopLinkCrypto.computeTokenHash("  Owner@Example.COM ", "482 915")
        val hash2 = ShopLinkCrypto.computeTokenHash("owner@example.com", "482915")
        val hashOtherCode = ShopLinkCrypto.computeTokenHash("owner@example.com", "482916")

        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[0-9a-f]{64}$")))
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashOtherCode)
    }

    @Test
    fun testShopLinkCodeGenerationFormattingAndValidation() {
        val generated = ShopLinkCrypto.generateSixDigitCode()
        assertEquals(6, generated.length)
        assertTrue(ShopLinkCrypto.isValidSixDigitCode(generated))
        assertEquals("123 456", ShopLinkCrypto.formatSixDigitCode("123456"))
        assertEquals("14m 05s", ShopLinkCrypto.formatRemainingTime((14 * 60 + 5) * 1000L))
        assertEquals("0m 00s", ShopLinkCrypto.formatRemainingTime(-500L))

        assertTrue(ShopLinkCrypto.isValidEmail("shop.owner+bd@gmail.com"))
        assertFalse(ShopLinkCrypto.isValidEmail("invalid-email"))
        assertFalse(ShopLinkCrypto.isValidSixDigitCode("12345"))
    }

    @Test
    fun testCustomerMiniStatementIncludesShopNameLastCreditAndLastPaid() {
        val cal1 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 10, 10, 0, 0) }
        val cal2 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 15, 30, 0) }
        val cal3 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 20, 11, 0, 0) }

        val customer = CustomerRecord(
            id = "c1",
            name = "Karim Store",
            phone = "01711111111",
            totalCredit = 2000.0,
            totalPaid = 500.0,
            currentDue = 1500.0
        )
        val entries = listOf(
            DueEntryRecord(id = "e1", type = "GOODS_TAKEN", amount = 500.0, itemsDescription = "Sugar", timestamp = cal1.timeInMillis),
            DueEntryRecord(id = "e2", type = "GOODS_TAKEN", amount = 1500.0, itemsDescription = "Rice 25kg", timestamp = cal2.timeInMillis),
            DueEntryRecord(id = "e3", type = "PAYMENT_RECEIVED", amount = 500.0, itemsDescription = "Cash", timestamp = cal3.timeInMillis)
        )

        val msg = CustomerMessageFormatter.buildCustomerMiniStatement(
            shopName = "Bismillah Enterprise",
            customer = customer,
            entries = entries
        )

        assertTrue(msg.startsWith("Bismillah Enterprise · Account Summary"))
        assertTrue(msg.contains("Customer: Karim Store"))
        assertFalse(msg.contains("────────"))
        assertTrue(msg.contains("Last Credit: ৳1,500"))
        assertTrue(msg.contains("  ${CurrencyUtils.formatDate(cal2.timeInMillis)} · Rice 25kg"))
        assertTrue(msg.contains("Last Paid: ৳500"))
        assertTrue(msg.contains("  ${CurrencyUtils.formatDate(cal3.timeInMillis)} · Cash"))
        assertTrue(msg.contains("Total Credit: ৳2,000"))
        assertTrue(msg.contains("Total Paid: ৳500"))
        assertTrue(msg.contains("Current Due: ৳1,500"))
        assertTrue(msg.endsWith("— Hishab Khata"))
    }

    @Test
    fun testCustomerMiniStatementHandlesAdvancePaymentAndEmptyFallbacks() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 21, 9, 0, 0) }
        val advanceCustomer = CustomerRecord(
            id = "c_adv",
            name = "Rahim Traders",
            totalCredit = 0.0,
            totalPaid = 1200.0,
            currentDue = 0.0
        )
        val advanceEntries = listOf(
            DueEntryRecord(id = "e_adv", type = "ADVANCE_PAYMENT", amount = 1200.0, itemsDescription = "bKash", timestamp = cal.timeInMillis)
        )

        val advMsg = CustomerMessageFormatter.buildCustomerMiniStatement(
            shopName = "",
            customer = advanceCustomer,
            entries = advanceEntries
        )

        assertTrue(advMsg.startsWith("Hishab Khata · Account Summary"))
        assertTrue(advMsg.contains("Last Credit: None"))
        assertTrue(advMsg.contains("Last Paid: ৳1,200"))
        assertTrue(advMsg.contains("  ${CurrencyUtils.formatDate(cal.timeInMillis)} · Advance · bKash"))
        assertTrue(advMsg.contains("Advance Balance: ৳1,200"))
        assertFalse(advMsg.contains("— Hishab Khata"))

        val settledCustomer = CustomerRecord(
            id = "c_settled",
            name = "Noor Mart",
            totalCredit = 500.0,
            totalPaid = 500.0,
            currentDue = 0.0
        )
        val settledMsg = CustomerMessageFormatter.buildCustomerMiniStatement(
            shopName = "Hishab Khata",
            customer = settledCustomer,
            entries = emptyList()
        )
        assertTrue(settledMsg.contains("Last Credit: None"))
        assertTrue(settledMsg.contains("Last Paid: None"))
        assertTrue(settledMsg.contains("Status: Settled (৳0)"))
        assertFalse(settledMsg.contains("— Hishab Khata"))
    }
}

