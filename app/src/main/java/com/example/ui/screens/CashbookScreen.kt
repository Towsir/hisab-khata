package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionRecord
import com.example.ui.components.AmbientWaveGraphic
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.EXPENSE_CATEGORIES
import com.example.ui.components.INCOME_CATEGORIES
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.RollingBdtCounter
import com.example.ui.theme.ledgerColors
import java.util.Calendar
import kotlin.math.roundToInt

enum class CashbookSegment(
    val typeKey: String,
    val label: String,
    val bnLabel: String
) {
    INCOME("INCOME", "Income", "আয়"),
    EXPENSE("EXPENSE", "Expense", "খরচ")
}

@Composable
fun CashbookScreen(
    selectedSegment: CashbookSegment,
    onSegmentSelected: (CashbookSegment) -> Unit,
    incomeTransactions: List<TransactionRecord>,
    expenseTransactions: List<TransactionRecord>,
    onEditTransaction: (TransactionRecord) -> Unit,
    onDeleteTransaction: (TransactionRecord) -> Unit,
    onDeleteAllForSegment: (CashbookSegment) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    val isIncome = selectedSegment == CashbookSegment.INCOME
    val activeTransactions = if (isIncome) incomeTransactions else expenseTransactions
    val accentColor = if (isIncome) ledgerColors.income else ledgerColors.expense
    val containerColor = if (isIncome) ledgerColors.incomeContainer else ledgerColors.expenseContainer
    val categories = if (isIncome) INCOME_CATEGORIES else EXPENSE_CATEGORIES

    // Totals computed strictly from each isolated list
    val totalIncome = remember(incomeTransactions) { incomeTransactions.sumOf { it.amount } }
    val totalExpense = remember(expenseTransactions) { expenseTransactions.sumOf { it.amount } }
    val netCash = totalIncome - totalExpense

    val thisMonthNet = remember(incomeTransactions, expenseTransactions) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        fun List<TransactionRecord>.sumThisMonth(): Double = filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month
        }.sumOf { it.amount }
        incomeTransactions.sumThisMonth() - expenseTransactions.sumThisMonth()
    }

    // Filter state scoped strictly per CashbookSegment so Income & Expense never cross-filter
    var isSearchExpanded by remember(selectedSegment) { mutableStateOf(false) }
    var selectedCategory by remember(selectedSegment) { mutableStateOf("ALL") }
    var searchQuery by remember(selectedSegment) { mutableStateOf("") }
    var showOverflowMenu by remember(selectedSegment) { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionRecord?>(null) }

    val hasActiveFilter = searchQuery.isNotBlank() || selectedCategory != "ALL"

    val filteredTransactions = remember(activeTransactions, selectedCategory, searchQuery) {
        activeTransactions.filter { tx ->
            val matchesCategory = selectedCategory == "ALL" || tx.category == selectedCategory
            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true) ||
                    tx.amount.toString().contains(searchQuery)
            }
            matchesCategory && matchesSearch
        }
    }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cashbook_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 168.dp + navBottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Single-Glance Cashbook Hero Summary Card (Net Cash + Separated Income & Expense)
        item(key = "cashbook_hero") {
            val netColor = if (netCash >= 0.0) ledgerColors.income else ledgerColors.expense
            val monthNetColor = if (thisMonthNet >= 0.0) ledgerColors.income else ledgerColors.expense
            val monthNetContainer = if (thisMonthNet >= 0.0) ledgerColors.incomeContainer else ledgerColors.expenseContainer

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashbook_summary_card"),
                cornerRadius = 24.dp,
                accentTint = accentColor,
                shimmerEnabled = true
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AmbientWaveGraphic(
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .align(Alignment.BottomCenter)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Cash Balance",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = monthNetContainer.copy(alpha = if (isDark) 0.72f else 0.90f),
                                border = BorderStroke(1.dp, monthNetColor.copy(alpha = 0.24f))
                            ) {
                                val monthFormatted = if (thisMonthNet > 0.0) {
                                    "+${CurrencyUtils.formatBdt(thisMonthNet)}"
                                } else {
                                    CurrencyUtils.formatBdt(thisMonthNet)
                                }
                                Text(
                                    text = "This mo: $monthFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = monthNetColor,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        RollingBdtCounter(
                            amount = netCash,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.35f else 0.55f),
                            thickness = 0.8.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Two Strictly Separated Quick-Glance Columns (Tap to Jump Directly)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CashbookSummaryMiniTile(
                                title = "Income",
                                amountText = "+ ${CurrencyUtils.formatBdt(totalIncome)}",
                                countText = "${incomeTransactions.size} entries",
                                accentColor = ledgerColors.income,
                                containerColor = ledgerColors.incomeContainer,
                                isSelected = isIncome,
                                isDark = isDark,
                                onClick = {
                                    if (!isIncome) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSegmentSelected(CashbookSegment.INCOME)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cashbook_summary_income")
                            )

                            CashbookSummaryMiniTile(
                                title = "Expense",
                                amountText = "− ${CurrencyUtils.formatBdt(totalExpense)}",
                                countText = "${expenseTransactions.size} entries",
                                accentColor = ledgerColors.expense,
                                containerColor = ledgerColors.expenseContainer,
                                isSelected = !isIncome,
                                isDark = isDark,
                                onClick = {
                                    if (isIncome) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSegmentSelected(CashbookSegment.EXPENSE)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cashbook_summary_expense")
                            )
                        }
                    }
                }
            }
        }

        // 2. Frosted Segmented Switcher (Income | Expense — Strictly Separate Books)
        item(key = "cashbook_switcher") {
            CashbookSegmentSwitcher(
                selectedSegment = selectedSegment,
                incomeCount = incomeTransactions.size,
                expenseCount = expenseTransactions.size,
                onSegmentSelected = onSegmentSelected
            )
        }

        // 3. Active Ledger Section Header + Collapsible Search & Category Filter Tray
        item(key = "cashbook_filter_section") {
            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = if (hasActiveFilter) {
                        "${filteredTransactions.size} of ${activeTransactions.size}"
                    } else {
                        "${activeTransactions.size}"
                    }
                    Text(
                        text = if (isIncome) "Income · $countLabel" else "Expenses · $countLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val toggleBg by animateColorAsState(
                            targetValue = if (isSearchExpanded || hasActiveFilter) {
                                containerColor
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                            },
                            animationSpec = tween(200),
                            label = "cashbookSearchToggleBg"
                        )
                        val toggleContent by animateColorAsState(
                            targetValue = if (isSearchExpanded || hasActiveFilter) {
                                accentColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(200),
                            label = "cashbookSearchToggleTint"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = toggleBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSearchExpanded || hasActiveFilter) {
                                    accentColor.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                }
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isSearchExpanded = !isSearchExpanded
                                }
                                .testTag("toggle_tx_search_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchExpanded) "Close search and filters" else "Search and filter entries",
                                    tint = toggleContent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSearchExpanded) "Done" else "Filter",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = toggleContent
                                )
                                if (hasActiveFilter && !isSearchExpanded) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(accentColor, CircleShape)
                                    )
                                }
                            }
                        }

                        if (activeTransactions.isNotEmpty()) {
                            Box {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showOverflowMenu = true
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("tx_more_menu_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More ledger options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isIncome) "Clear all income" else "Clear all expenses",
                                                color = ledgerColors.expense,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = ledgerColors.expense,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDeleteAllConfirm = true
                                        },
                                        modifier = Modifier.testTag(if (isIncome) "delete_all_income_btn" else "delete_all_expense_btn")
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Filter Pill Banner (shown when collapsed while filters are active)
                AnimatedVisibility(
                    visible = !isSearchExpanded && hasActiveFilter,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
                ) {
                    val activeSummary = buildList {
                        if (selectedCategory != "ALL") add(selectedCategory)
                        if (searchQuery.isNotBlank()) add("\"${searchQuery.trim()}\"")
                    }.joinToString(" · ")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("active_tx_filter_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing: $activeSummary",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    searchQuery = ""
                                    selectedCategory = "ALL"
                                },
                                modifier = Modifier.testTag("clear_tx_filter_pill_btn")
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }

                // Collapsible Search Input & Category Chips
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(if (isIncome) "Search income..." else "Search expenses...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(if (isIncome) "income_search_input" else "expense_search_input")
                        )

                        val visibleCategories = categories.filter { cat ->
                            activeTransactions.any { it.category == cat } || selectedCategory == cat
                        }
                        if (visibleCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == "ALL",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = "ALL"
                                    },
                                    label = { Text("All (${activeTransactions.size})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                        selectedContainerColor = containerColor,
                                        selectedLabelColor = accentColor
                                    )
                                )
                                visibleCategories.forEach { cat ->
                                    val count = activeTransactions.count { it.category == cat }
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategory = cat
                                        },
                                        label = { Text("$cat ($count)", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                            selectedContainerColor = containerColor,
                                            selectedLabelColor = accentColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Isolated Active Ledger Rows (or Empty / Zero-Match State)
        if (filteredTransactions.isEmpty()) {
            val isFilteredOut = activeTransactions.isNotEmpty() && hasActiveFilter
            item(key = "cashbook_empty_${selectedSegment.name}") {
                LiquidGlassCard(
                    cornerRadius = 18.dp,
                    accentTint = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isFilteredOut) Icons.Default.Search else Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when {
                                isFilteredOut -> "No matching entries"
                                isIncome -> "No income recorded yet"
                                else -> "No expenses recorded yet"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isFilteredOut -> "Try a different search term or clear your active filters."
                                isIncome -> "Tap + Income below to record your first sale."
                                else -> "Tap + Expense below to record a shop expense."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (isFilteredOut) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    searchQuery = ""
                                    selectedCategory = "ALL"
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_tx_filters_btn")
                            ) {
                                Text("Reset Filters", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { "${selectedSegment.name}_${it.id}" }) { tx ->
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .animateContentSize()
                        .testTag("transaction_card_${tx.id}"),
                    cornerRadius = 18.dp,
                    accentTint = accentColor,
                    onClick = { onEditTransaction(tx) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(color = containerColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = tx.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val metaLine = buildString {
                                    if (tx.note.isNotBlank()) {
                                        append(tx.note)
                                        append(" · ")
                                    }
                                    append(CurrencyUtils.formatDateTime(tx.timestamp))
                                }
                                Text(
                                    text = metaLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (if (isIncome) "+ " else "− ") + CurrencyUtils.formatBdt(tx.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1
                            )

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    transactionToDelete = tx
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_tx_${tx.id}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Transaction",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Single Transaction Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = if (isIncome) "Delete Income Entry?" else "Delete Expense Entry?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Remove ${tx.category} (${CurrencyUtils.formatBdt(tx.amount)}) from ${CurrencyUtils.formatDate(tx.timestamp)}? You can still undo right after."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val target = tx
                        transactionToDelete = null
                        onDeleteTransaction(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ledgerColors.expense,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_tx_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { transactionToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_tx_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog (strictly scoped to the active segment only)
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = {
                Text(
                    text = if (isIncome) "Clear All Income?" else "Clear All Expenses?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isIncome) "This permanently removes all Income entries. Expenses and Due Ledger remain safe."
                    else "This permanently removes all Expense entries. Income and Due Ledger remain safe."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirm = false
                        onDeleteAllForSegment(selectedSegment)
                    },
                    modifier = Modifier.testTag("confirm_delete_all_tx_btn")
                ) {
                    Text("Clear All", color = ledgerColors.expense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_all_tx_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CashbookSummaryMiniTile(
    title: String,
    amountText: String,
    countText: String,
    accentColor: Color,
    containerColor: Color,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            containerColor.copy(alpha = if (isDark) 0.55f else 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.40f else 0.55f)
        },
        animationSpec = tween(220),
        label = "miniTileBg_$title"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = if (isDark) 0.45f else 0.35f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        animationSpec = tween(220),
        label = "miniTileBorder_$title"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(accentColor, CircleShape)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CashbookSegmentSwitcher(
    selectedSegment: CashbookSegment,
    incomeCount: Int,
    expenseCount: Int,
    onSegmentSelected: (CashbookSegment) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val selectedIndex = if (selectedSegment == CashbookSegment.INCOME) 0f else 1f
    val pillProgress by animateFloatAsState(
        targetValue = selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 380f
        ),
        label = "cashbookSegmentPill"
    )
    val activeAccent by animateColorAsState(
        targetValue = if (selectedSegment == CashbookSegment.INCOME) ledgerColors.income else ledgerColors.expense,
        animationSpec = tween(240),
        label = "cashbookSegmentAccent"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.76f else 0.84f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("cashbook_segment_switcher")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val slotWidthPx = constraints.maxWidth / 2f
            val slotWidthDp = with(density) { slotWidthPx.toDp() }
            val offsetXPx = (pillProgress * slotWidthPx).roundToInt()

            // Sliding Active Frosted Pill
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = offsetXPx, y = 0) }
                    .width(slotWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    activeAccent.copy(alpha = 0.30f),
                                    activeAccent.copy(alpha = 0.16f)
                                )
                            } else {
                                listOf(
                                    activeAccent.copy(alpha = 0.18f),
                                    activeAccent.copy(alpha = 0.10f)
                                )
                            }
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val segments = listOf(
                    Triple(CashbookSegment.INCOME, incomeCount, "cashbook_tab_income"),
                    Triple(CashbookSegment.EXPENSE, expenseCount, "cashbook_tab_expense")
                )
                segments.forEach { (segment, count, tag) ->
                    val isSelected = selectedSegment == segment
                    val segColor = if (segment == CashbookSegment.INCOME) ledgerColors.income else ledgerColors.expense
                    val contentTint by animateColorAsState(
                        targetValue = if (isSelected) segColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "segmentTint_${segment.name}"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSegmentSelected(segment)
                                    }
                                }
                            )
                            .testTag(tag),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (segment == CashbookSegment.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = contentTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${segment.label} (${segment.bnLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "· $count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = contentTint.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
