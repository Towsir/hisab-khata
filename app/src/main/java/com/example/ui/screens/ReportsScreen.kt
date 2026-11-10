package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TransactionEntity
import com.example.ui.components.CategoryShare
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.MonthlyCashFlowSummaryCard
import java.util.Calendar

@Composable
fun ReportsScreen(
    selectedCalendar: Calendar,
    transactions: List<TransactionEntity>,
    categoryShares: List<CategoryShare>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val context = LocalContext.current
    val year = selectedCalendar.get(Calendar.YEAR)
    val month = selectedCalendar.get(Calendar.MONTH)
    val monthTitle = CurrencyUtils.formatMonthYear(year, month)

    val incomeList = transactions.filter { it.type == "INCOME" }
    val expenseList = transactions.filter { it.type == "EXPENSE" }

    val totalIncome = incomeList.sumOf { it.amount }
    val totalExpense = expenseList.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense
    val marginPercentage = if (totalIncome > 0) (netProfit / totalIncome) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_selector_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("prev_month_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Monthly Automatic Statement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("next_month_btn")
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

        // Cash Flow Summary Card
        item {
            MonthlyCashFlowSummaryCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netProfit = netProfit,
                marginPercentage = marginPercentage
            )
        }

        // Visual Donut Chart by Category
        item {
            ExpenseDonutChart(
                categoryShares = categoryShares,
                totalExpense = totalExpense
            )
        }

        // Category Detail Breakdown List
        if (categoryShares.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Ranking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categoryShares.size} Categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(categoryShares) { share ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(share.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = share.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = CurrencyUtils.formatBdt(share.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (share.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = share.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${String.format("%.1f", share.percentage)}% of total monthly expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Monthly share",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Export Report Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val reportText = buildString {
                        appendLine("📊 HISHAB KHATA - MONTHLY FINANCIAL STATEMENT")
                        appendLine("Period: $monthTitle")
                        appendLine("================================")
                        appendLine("Total Income: ${CurrencyUtils.formatBdt(totalIncome)}")
                        appendLine("Total Expenses: ${CurrencyUtils.formatBdt(totalExpense)}")
                        appendLine("Net Profit/Loss: ${CurrencyUtils.formatBdt(netProfit)} (Margin: ${String.format("%.1f", marginPercentage)}%)")
                        appendLine("================================")
                        appendLine("Expense Breakdown:")
                        categoryShares.forEach { share ->
                            appendLine("- ${share.category}: ${CurrencyUtils.formatBdt(share.amount)} (${String.format("%.1f", share.percentage)}%)")
                        }
                        appendLine("================================")
                        appendLine("Generated automatically by Hishab Khata.")
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Financial Statement"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_report_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export / Share Monthly Statement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
