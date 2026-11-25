package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionRecord
import com.example.ui.components.CategoryShare
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.MonthlyCashFlowSummaryCard
import java.util.Calendar

@Composable
fun ReportsScreen(
    selectedCalendar: Calendar,
    transactions: List<TransactionRecord>,
    categoryShares: List<CategoryShare>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    shopName: String = "Hishab Khata"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val year = selectedCalendar.get(Calendar.YEAR)
    val month = selectedCalendar.get(Calendar.MONTH)
    val monthTitle = CurrencyUtils.formatMonthYear(year, month)

    val incomeList = transactions.filter { it.type == "INCOME" }
    val expenseList = transactions.filter { it.type == "EXPENSE" }

    val totalIncome = incomeList.sumOf { it.amount }
    val totalExpense = expenseList.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense
    val marginPercentage = if (totalIncome > 0) (netProfit / totalIncome) * 100 else 0.0
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 168.dp + navBottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Frosted Glass Month Selector
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_selector_card"),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPreviousMonth()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("prev_month_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNextMonth()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("next_month_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Cash Flow Summary Card (strictly Income & Expense, never mixing Due Ledger)
        item {
            MonthlyCashFlowSummaryCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netProfit = netProfit,
                marginPercentage = marginPercentage
            )
        }

        // Expense Donut Chart
        item {
            ExpenseDonutChart(
                categoryShares = categoryShares,
                totalExpense = totalExpense
            )
        }

        // Frosted Glass Category Ranking
        if (categoryShares.isNotEmpty()) {
            item {
                Text(
                    text = "By Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(categoryShares, key = { it.category }) { share ->
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    cornerRadius = 18.dp,
                    accentTint = share.color
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(share.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = share.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${CurrencyUtils.formatBdt(share.amount)} · ${String.format("%.0f", share.percentage)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (share.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = share.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Clean Export Button
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = {
                    val trimmedShop = shopName.trim()
                    val hasCustomShop = trimmedShop.isNotBlank() && !trimmedShop.equals("Hishab Khata", ignoreCase = true)
                    val titleShop = if (hasCustomShop) trimmedShop else "Hishab Khata"

                    val reportText = buildString {
                        appendLine("$titleShop · Monthly Summary")
                        appendLine("Month: $monthTitle")
                        appendLine()
                        appendLine("Income: ${CurrencyUtils.formatBdt(totalIncome)}")
                        appendLine("Expense: ${CurrencyUtils.formatBdt(totalExpense)}")
                        append("Net Cash: ${CurrencyUtils.formatBdt(netProfit)}")
                        if (categoryShares.isNotEmpty()) {
                            appendLine()
                            appendLine()
                            appendLine("Expenses by Category:")
                            categoryShares.forEachIndexed { idx, share ->
                                val line = "  · ${share.category}: ${CurrencyUtils.formatBdt(share.amount)} (${String.format("%.0f", share.percentage)}%)"
                                if (idx < categoryShares.lastIndex) appendLine(line) else append(line)
                            }
                        }
                        if (hasCustomShop) {
                            appendLine()
                            appendLine()
                            append("— Hishab Khata")
                        }
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Statement"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_report_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Statement", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
