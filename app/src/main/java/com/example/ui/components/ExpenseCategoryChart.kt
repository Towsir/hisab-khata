package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ledgerColors

data class CategoryShare(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseDonutChart(
    categoryShares: List<CategoryShare>,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<CategoryShare?>(null) }

    LaunchedEffect(categoryShares) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_donut_chart_card"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expense Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any category to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedCategory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = null }
                    ) {
                        Text(
                            text = "Reset view",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (categoryShares.isEmpty() || totalExpense <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses logged for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive Donut Chart Container
                Box(
                    modifier = Modifier.size(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val strokeWidthNormal = 28.dp.toPx()
                        val strokeWidthHighlight = 34.dp.toPx()

                        val arcSize = Size(size.width - strokeWidthHighlight, size.height - strokeWidthHighlight)
                        val topLeft = Offset(strokeWidthHighlight / 2, strokeWidthHighlight / 2)

                        var currentStartAngle = -90f
                        categoryShares.forEach { share ->
                            val sweepAngle = (share.percentage / 100f) * 360f * progress.value
                            val isSelected = selectedCategory?.category == share.category
                            val activeWidth = if (isSelected) strokeWidthHighlight else strokeWidthNormal

                            if (sweepAngle > 0f) {
                                drawArc(
                                    color = if (selectedCategory == null || isSelected) share.color else share.color.copy(alpha = 0.3f),
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = activeWidth, cap = StrokeCap.Round)
                                )
                                currentStartAngle += sweepAngle
                            }
                        }
                    }

                    // Center Animated Information Callout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedCategory,
                            label = "centerCallout"
                        ) { cat ->
                            if (cat != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cat.color,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = CurrencyUtils.formatBdt(cat.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format("%.1f", cat.percentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Total Spent",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyUtils.formatBdt(totalExpense),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${categoryShares.size} Categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Interactive Category Pill Legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryShares.forEach { share ->
                        val isSelected = selectedCategory?.category == share.category
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) share.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, share.color) else BorderStroke(1.dp, Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = if (isSelected) null else share
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(share.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = share.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${String.format("%.1f", share.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCashFlowSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netProfit: Double,
    marginPercentage: Double,
    modifier: Modifier = Modifier
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val totalFlow = (totalIncome + totalExpense).toFloat()
    val incomeRatioTarget = if (totalFlow > 0f) (totalIncome.toFloat() / totalFlow) else 0.5f
    val animatedRatio by animateFloatAsState(
        targetValue = incomeRatioTarget,
        animationSpec = spring(),
        label = "flowRatio"
    )

    val netAccent = if (netProfit >= 0) ledgerColors.income else ledgerColors.expense

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_cashflow_summary_card"),
        cornerRadius = 24.dp,
        accentTint = netAccent,
        shimmerEnabled = true
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AmbientWaveGraphic(
                accentColor = netAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .align(Alignment.BottomCenter)
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Flow Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (netProfit >= 0) ledgerColors.incomeContainer else ledgerColors.expenseContainer
                    ) {
                        Text(
                            text = if (netProfit >= 0) "Profitable (+${String.format("%.1f", marginPercentage)}%)" else "Operating Loss",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = netAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Inflow (আয়)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RollingBdtCounter(
                            amount = totalIncome,
                            prefix = "+ ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ledgerColors.income
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Total Outflow (খরচ)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RollingBdtCounter(
                            amount = totalExpense,
                            prefix = "− ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ledgerColors.expense
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dual Progress Comparison Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ledgerColors.expense.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedRatio.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ledgerColors.income)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Net Profit Frosted Sub-Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (netProfit >= 0) "Net Business Profit (নিট লাভ)" else "Net Operating Loss (নিট ক্ষতি)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Revenue Margin: ${String.format("%.1f", marginPercentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RollingBdtCounter(
                            amount = netProfit,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = netAccent
                        )
                    }
                }
            }
        }
    }
}
