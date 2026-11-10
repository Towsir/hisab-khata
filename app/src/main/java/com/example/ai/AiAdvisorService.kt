package com.example.ai

import android.util.Log
import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAdvisorService"

sealed interface AiAdviceState {
    data object Idle : AiAdviceState
    data object Loading : AiAdviceState
    data class Success(val advice: String) : AiAdviceState
    data class Error(val message: String) : AiAdviceState
}

class AiAdvisorService {

    // Lazy initialization of gemini-4-argon with ThinkingLevel.HIGH
    private val advisorModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )
    }

    suspend fun generateBusinessAudit(
        transactions: List<TransactionEntity>,
        customers: List<CustomerEntity>,
        customQuestion: String? = null
    ): String = withContext(Dispatchers.IO) {
        val incomeList = transactions.filter { it.type == "INCOME" }
        val expenseList = transactions.filter { it.type == "EXPENSE" }

        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = expenseList.sumOf { it.amount }
        val netProfit = totalIncome - totalExpense
        val marginPercent = if (totalIncome > 0) (netProfit / totalIncome) * 100 else 0.0

        val expenseByCategory = expenseList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalCreditGiven = customers.sumOf { it.totalCredit }
        val totalDueOutstanding = customers.sumOf { it.currentDue }
        val totalPaidByCustomers = customers.sumOf { it.totalPaid }
        val customersWithDue = customers.filter { it.currentDue > 0 }
            .sortedByDescending { it.currentDue }

        val promptBuilder = StringBuilder()
        promptBuilder.appendLine("You are an expert Chief Financial Officer and business growth advisor for a Bangladeshi retail/wholesale store and small enterprise.")
        promptBuilder.appendLine("The currency is Bangladeshi Taka (BDT / ৳). Perform an in-depth, rigorous financial diagnostic analysis with deep reasoning.")
        promptBuilder.appendLine()
        promptBuilder.appendLine("=== BUSINESS FINANCIAL SUMMARY ===")
        promptBuilder.appendLine("- Total Income: ৳$totalIncome across ${incomeList.size} receipts")
        promptBuilder.appendLine("- Total Expenses: ৳$totalExpense across ${expenseList.size} vouchers")
        promptBuilder.appendLine("- Net Profit / Loss: ৳$netProfit (Net Margin: ${String.format("%.1f", marginPercent)}%)")
        promptBuilder.appendLine()
        promptBuilder.appendLine("=== EXPENSES BREAKDOWN BY CATEGORY ===")
        expenseByCategory.forEach { (cat, amt) ->
            val pct = if (totalExpense > 0) (amt / totalExpense) * 100 else 0.0
            promptBuilder.appendLine("- $cat: ৳$amt (${String.format("%.1f", pct)}%)")
        }
        promptBuilder.appendLine()
        promptBuilder.appendLine("=== CUSTOMER CREDIT & DUE LEDGER (BAKI) ===")
        promptBuilder.appendLine("- Total Goods Given on Credit: ৳$totalCreditGiven")
        promptBuilder.appendLine("- Total Amount Collected: ৳$totalPaidByCustomers")
        promptBuilder.appendLine("- Total Outstanding Due Remaining: ৳$totalDueOutstanding")
        promptBuilder.appendLine("- Number of Active Debtors: ${customersWithDue.size}")
        if (customersWithDue.isNotEmpty()) {
            promptBuilder.appendLine("Top Debtors:")
            customersWithDue.take(5).forEach { c ->
                promptBuilder.appendLine("  * ${c.name}: ৳${c.currentDue} due (Total Credit: ৳${c.totalCredit}, Paid: ৳${c.totalPaid})")
            }
        }
        promptBuilder.appendLine()

        if (!customQuestion.isNullOrBlank()) {
            promptBuilder.appendLine("=== MERCHANT SPECIFIC INQUIRY ===")
            promptBuilder.appendLine(customQuestion)
            promptBuilder.appendLine()
        }

        promptBuilder.appendLine("=== REQUIRED ANALYSIS OUTPUT STRUCTURE ===")
        promptBuilder.appendLine("Provide a clear, highly structured and professional analysis addressing:")
        promptBuilder.appendLine("1. 📊 Financial Health & Working Capital Audit (Evaluate revenue vs burn rate, operating margins)")
        promptBuilder.appendLine("2. ⚠️ Expense Leakage & Optimization Targets (Pinpoint largest costs and tangible reduction strategies)")
        promptBuilder.appendLine("3. 📒 Customer Due & Credit Risk Assessment (Evaluate bad debt exposure, debtor concentration risk, and polite recovery tactics for Bangladesh bazaar culture)")
        promptBuilder.appendLine("4. 🚀 3 Immediate High-Impact Action Items for the merchant to increase profit and cash on hand this month.")
        promptBuilder.appendLine("Keep tone professional, encouraging, practical, and formatted with clean bullet points and bold section headers.")

        try {
            // Realistic timeout for high reasoning thinking mode
            val response = withTimeout(90_000L) {
                advisorModel.generateContent(promptBuilder.toString())
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                text
            } else {
                "The financial advisor model completed analysis with an empty response. Please try again."
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Advisor reasoning timed out", e)
            throw Exception("Financial analysis timed out after 90 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Advisor generation failed", e)
            throw Exception(e.localizedMessage ?: "Failed to generate business financial audit.")
        }
    }
}
