package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NavigationTab
import com.example.ui.screens.CashbookSegment
import com.example.ui.theme.ledgerColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class DockTabSpec(
    val tab: NavigationTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
    val testTag: String
)

/**
 * Floating Liquid Glass Bottom Navigation Dock with a spring-driven morphing liquid pill indicator.
 * Respects WindowInsets.navigationBars so gesture pills and 3-button nav bars never overlap.
 */
@Composable
fun FloatingLiquidGlassDock(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    cashbookSegment: CashbookSegment = CashbookSegment.INCOME,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current

    val tabs = remember {
        listOf(
            DockTabSpec(
                tab = NavigationTab.CASHBOOK,
                selectedIcon = Icons.Filled.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                contentDescription = "Cashbook",
                testTag = "nav_cashbook"
            ),
            DockTabSpec(
                tab = NavigationTab.DUE_LEDGER,
                selectedIcon = Icons.Filled.Book,
                unselectedIcon = Icons.Outlined.Book,
                contentDescription = "Due Ledger",
                testTag = "nav_due_ledger"
            ),
            DockTabSpec(
                tab = NavigationTab.REPORTS,
                selectedIcon = Icons.Filled.Assessment,
                unselectedIcon = Icons.Outlined.Assessment,
                contentDescription = "Reports",
                testTag = "nav_reports"
            )
        )
    }

    val activeAccentTarget = when (selectedTab) {
        NavigationTab.CASHBOOK -> if (cashbookSegment == CashbookSegment.INCOME) {
            ledgerColors.income
        } else {
            ledgerColors.expense
        }
        NavigationTab.DUE_LEDGER -> ledgerColors.dueAmber
        NavigationTab.REPORTS -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    }
    val activeAccent by animateColorAsState(
        targetValue = activeAccentTarget,
        animationSpec = tween(durationMillis = 360),
        label = "dockAccent"
    )

    // Dual-spring morphing pill physics: leading edge moves quicker than trailing edge,
    // stretching the glass droplet organically across the dock during tab transitions!
    val targetIndex = selectedTab.index.toFloat()
    val leadProgress by animateFloatAsState(
        targetValue = targetIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 460f
        ),
        label = "liquidPillLead"
    )
    val trailProgress by animateFloatAsState(
        targetValue = targetIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 230f
        ),
        label = "liquidPillTrail"
    )

    val dockShape = RoundedCornerShape(32.dp)
    val dockFill = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.84f else 0.88f)
    val dockBorder = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.28f),
                activeAccent.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.10f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.96f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                activeAccent.copy(alpha = 0.28f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = dockShape,
            color = dockFill,
            border = BorderStroke(1.2.dp, dockBorder),
            shadowElevation = if (isDark) 10.dp else 14.dp,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(68.dp)
                .testTag("app_bottom_nav_bar")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Top liquid glass sheen highlight inside the floating capsule
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.55f),
                                    Color.Transparent,
                                    activeAccent.copy(alpha = if (isDark) 0.08f else 0.04f)
                                )
                            )
                        )
                    }
                    .padding(6.dp)
            ) {
                val slotWidthPx = constraints.maxWidth.toFloat() / tabs.size
                val pillLeftIndex = min(leadProgress, trailProgress)
                val pillRightIndex = max(leadProgress, trailProgress) + 1f
                val pillLeftPx = pillLeftIndex * slotWidthPx
                val pillWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                    ((pillRightIndex - pillLeftIndex) * slotWidthPx).toDp()
                }

                // Morphing Liquid Glass Pill Indicator
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = pillLeftPx.roundToInt(), y = 0) }
                        .width(pillWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        activeAccent.copy(alpha = 0.28f),
                                        activeAccent.copy(alpha = 0.14f)
                                    )
                                } else {
                                    listOf(
                                        activeAccent.copy(alpha = 0.18f),
                                        activeAccent.copy(alpha = 0.09f)
                                    )
                                }
                            )
                        )
                        .drawBehind {
                            // Subtle specular rim on the liquid pill itself
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.30f else 0.75f),
                                        activeAccent.copy(alpha = 0.22f)
                                    )
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                )

                // Interactive Tab Buttons
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { spec ->
                        val isSelected = selectedTab == spec.tab
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockIconScale_${spec.tab.name}"
                        )
                        val itemTint by animateColorAsState(
                            targetValue = if (isSelected) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 220),
                            label = "dockItemTint_${spec.tab.name}"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!isSelected) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onTabSelected(spec.tab)
                                        }
                                    }
                                )
                                .testTag(spec.testTag),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) spec.selectedIcon else spec.unselectedIcon,
                                contentDescription = spec.contentDescription,
                                tint = itemTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spec.tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = itemTint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
