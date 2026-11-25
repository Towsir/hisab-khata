package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NavigationTab
import com.example.ui.screens.CashbookSegment
import com.example.ui.theme.ledgerColors

/**
 * Always-reachable, context-aware Quick Add pill FAB positioned above the floating glass dock.
 * Smoothly adapts its accent color, icon, and label per tab and active Cashbook segment:
 * - CASHBOOK (INCOME) -> "+ Income"
 * - CASHBOOK (EXPENSE) -> "+ Expense"
 * - DUE_LEDGER -> "+ Customer"
 * - REPORTS -> "+ Quick Add" (expands a tidy 3-action frosted menu)
 */
@Composable
fun QuickAddFab(
    selectedTab: NavigationTab,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddCustomer: () -> Unit,
    cashbookSegment: CashbookSegment = CashbookSegment.INCOME,
    modifier: Modifier = Modifier
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    var reportsMenuExpanded by remember { mutableStateOf(false) }

    // Automatically close reports quick-add menu when navigating away
    LaunchedEffect(selectedTab) {
        if (selectedTab != NavigationTab.REPORTS) {
            reportsMenuExpanded = false
        }
    }

    val targetContainerColor = when (selectedTab) {
        NavigationTab.CASHBOOK -> if (cashbookSegment == CashbookSegment.INCOME) {
            ledgerColors.income
        } else {
            ledgerColors.expense
        }
        NavigationTab.DUE_LEDGER -> MaterialTheme.colorScheme.primary
        NavigationTab.REPORTS -> MaterialTheme.colorScheme.primary
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "quickAddFabColor"
    )
    val contentColor = if (selectedTab == NavigationTab.CASHBOOK) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quickAddFabScale"
    )

    val fabShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 16.dp, bottom = 92.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            shape = fabShape,
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isDark) 0.24f else 0.42f)
            ),
            shadowElevation = if (isDark) 8.dp else 10.dp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .heightIn(min = 52.dp)
                .clip(fabShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (selectedTab) {
                            NavigationTab.CASHBOOK -> if (cashbookSegment == CashbookSegment.INCOME) {
                                onAddIncome()
                            } else {
                                onAddExpense()
                            }
                            NavigationTab.DUE_LEDGER -> onAddCustomer()
                            NavigationTab.REPORTS -> reportsMenuExpanded = !reportsMenuExpanded
                        }
                    }
                )
                .testTag("quick_add_fab")
        ) {
            AnimatedContent(
                targetState = Triple(selectedTab, cashbookSegment, reportsMenuExpanded),
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(180))) togetherWith
                        (fadeOut(tween(130)) + scaleOut(targetScale = 0.92f, animationSpec = tween(130)))
                },
                label = "quickAddFabLabel"
            ) { (tab, segment, menuOpen) ->
                val (icon, label) = when (tab) {
                    NavigationTab.CASHBOOK -> if (segment == CashbookSegment.INCOME) {
                        Icons.Default.Add to "Income"
                    } else {
                        Icons.Default.Add to "Expense"
                    }
                    NavigationTab.DUE_LEDGER -> Icons.Default.PersonAdd to "Customer"
                    NavigationTab.REPORTS -> if (menuOpen) {
                        Icons.Default.Close to "Close"
                    } else {
                        Icons.Default.Add to "Quick Add"
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }

        DropdownMenu(
            expanded = reportsMenuExpanded && selectedTab == NavigationTab.REPORTS,
            onDismissRequest = { reportsMenuExpanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.96f else 0.98f),
            modifier = Modifier.testTag("reports_quick_add_menu")
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Add Income",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(ledgerColors.incomeContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = ledgerColors.income,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    reportsMenuExpanded = false
                    onAddIncome()
                },
                modifier = Modifier.testTag("quick_add_income_item")
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Add Expense",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(ledgerColors.expenseContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = ledgerColors.expense,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    reportsMenuExpanded = false
                    onAddExpense()
                },
                modifier = Modifier.testTag("quick_add_expense_item")
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Add Customer",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    reportsMenuExpanded = false
                    onAddCustomer()
                },
                modifier = Modifier.testTag("quick_add_customer_item")
            )
        }
    }
}
