package com.example.ui.components

import com.example.data.model.CustomerRecord
import com.example.data.model.DueEntryRecord

/**
 * Pure, unit-testable formatter for clean, short, scannable customer Due/Advance receipts
 * shared via SMS or WhatsApp. Derives the most recent credit ("Last Credit") and payment
 * ("Last Paid") directly from the customer's DueEntryRecord history.
 */
object CustomerMessageFormatter {

    fun findLastCreditEntry(entries: List<DueEntryRecord>): DueEntryRecord? {
        return entries
            .filter { it.type == "GOODS_TAKEN" }
            .maxByOrNull { it.timestamp }
    }

    fun findLastPaidEntry(entries: List<DueEntryRecord>): DueEntryRecord? {
        return entries
            .filter { it.type == "PAYMENT_RECEIVED" || it.type == "ADVANCE_PAYMENT" }
            .maxByOrNull { it.timestamp }
    }

    fun formatEntryAmount(entry: DueEntryRecord?): String {
        return if (entry == null) "None" else CurrencyUtils.formatBdt(entry.amount)
    }

    fun formatEntryMeta(entry: DueEntryRecord?): String {
        if (entry == null) return ""
        val parts = buildList {
            if (entry.timestamp > 0L) add(CurrencyUtils.formatDate(entry.timestamp))
            if (entry.type == "ADVANCE_PAYMENT") add("Advance")
            val trimmedNote = entry.itemsDescription.trim()
            if (trimmedNote.isNotBlank()) add(trimmedNote)
        }
        return parts.joinToString(" · ")
    }

    fun formatEntrySummary(entry: DueEntryRecord?): String {
        if (entry == null) return "None"
        val amountText = formatEntryAmount(entry)
        val meta = formatEntryMeta(entry)
        return if (meta.isNotBlank()) "$amountText · $meta" else amountText
    }

    fun buildCustomerMiniStatement(
        shopName: String,
        customer: CustomerRecord,
        entries: List<DueEntryRecord>
    ): String {
        val trimmedShop = shopName.trim()
        val hasCustomShop = trimmedShop.isNotBlank() && !trimmedShop.equals("Hishab Khata", ignoreCase = true)
        val titleShop = if (hasCustomShop) trimmedShop else "Hishab Khata"

        val lastCredit = findLastCreditEntry(entries)
        val lastPaid = findLastPaidEntry(entries)

        val balanceLine = when {
            customer.advanceBalance > 0.0 ->
                "Advance Balance: ${CurrencyUtils.formatBdt(customer.advanceBalance)}"
            customer.currentDue > 0.0 ->
                "Current Due: ${CurrencyUtils.formatBdt(customer.currentDue)}"
            else ->
                "Status: Settled (${CurrencyUtils.formatBdt(0.0)})"
        }

        return buildString {
            appendLine("$titleShop · Account Summary")
            appendLine("Customer: ${customer.name}")
            appendLine()
            appendLine("Last Credit: ${formatEntryAmount(lastCredit)}")
            val creditMeta = formatEntryMeta(lastCredit)
            if (creditMeta.isNotBlank()) {
                appendLine("  $creditMeta")
            }
            appendLine("Last Paid: ${formatEntryAmount(lastPaid)}")
            val paidMeta = formatEntryMeta(lastPaid)
            if (paidMeta.isNotBlank()) {
                appendLine("  $paidMeta")
            }
            appendLine()
            appendLine("Total Credit: ${CurrencyUtils.formatBdt(customer.totalCredit)}")
            appendLine("Total Paid: ${CurrencyUtils.formatBdt(customer.totalPaid)}")
            append(balanceLine)
            if (hasCustomShop) {
                appendLine()
                appendLine()
                append("— Hishab Khata")
            }
        }
    }
}
