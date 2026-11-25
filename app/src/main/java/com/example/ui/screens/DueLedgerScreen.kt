package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerRecord
import com.example.ui.components.AmbientWaveGraphic
import com.example.ui.components.CurrencyUtils
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.RollingBdtCounter
import com.example.ui.theme.ledgerColors

@Composable
fun DueLedgerScreen(
    customers: List<CustomerRecord>,
    onCustomerClicked: (CustomerRecord) -> Unit,
    onAddItemsClicked: (CustomerRecord) -> Unit,
    onCollectPaymentClicked: (CustomerRecord) -> Unit,
    onAddNewCustomerClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ledgerColors = MaterialTheme.ledgerColors
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "DUE", "ADVANCE", "ALL"
    var sortBy by remember { mutableStateOf("DUE_DESC") } // "DUE_DESC", "RECENT", "NAME"

    val hasActiveFilter = searchQuery.isNotBlank() || statusFilter != "ALL"

    val totalDueAll = customers.sumOf { it.currentDue }
    val totalAdvanceAll = customers.sumOf { it.advanceBalance }
    val dueCount = customers.count { it.currentDue > 0 }
    val advanceCount = customers.count { it.advanceBalance > 0 }
    val heroAccent = if (totalDueAll > 0) ledgerColors.expense else ledgerColors.income

    val filteredCustomers = customers
        .filter { customer ->
            val matchesSearch = customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.phone.contains(searchQuery) ||
                customer.address.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (statusFilter) {
                "DUE" -> customer.currentDue > 0
                "ADVANCE" -> customer.advanceBalance > 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
        .sortedWith { a, b ->
            when (sortBy) {
                "RECENT" -> b.lastUpdated.compareTo(a.lastUpdated)
                "NAME" -> a.name.compareTo(b.name, ignoreCase = true)
                else -> b.currentDue.compareTo(a.currentDue)
            }
        }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("due_ledger_screen"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 168.dp + navBottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sleek Liquid Glass Hero Banner
        item(key = "due_summary_card") {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_due_banner"),
                cornerRadius = 24.dp,
                accentTint = heroAccent,
                shimmerEnabled = true
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AmbientWaveGraphic(
                        accentColor = heroAccent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .align(Alignment.BottomCenter)
                    )

                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Due (মোট বাকি)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${customers.size} customers",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        RollingBdtCounter(
                            amount = totalDueAll,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = heroAccent,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            thickness = 0.8.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val dueActive = statusFilter == "DUE"
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (dueActive) {
                                    ledgerColors.expenseContainer.copy(alpha = 0.72f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (dueActive) ledgerColors.expense.copy(alpha = 0.38f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        statusFilter = if (statusFilter == "DUE") "ALL" else "DUE"
                                    }
                                    .testTag("due_summary_owing")
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
                                                .background(ledgerColors.expense, CircleShape)
                                        )
                                        Text(
                                            text = "With Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$dueCount accounts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dueCount > 0) ledgerColors.expense else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val advanceActive = statusFilter == "ADVANCE"
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (advanceActive) {
                                    ledgerColors.incomeContainer.copy(alpha = 0.72f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (advanceActive) ledgerColors.income.copy(alpha = 0.38f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        statusFilter = if (statusFilter == "ADVANCE") "ALL" else "ADVANCE"
                                    }
                                    .testTag("due_summary_advance")
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
                                                .background(ledgerColors.income, CircleShape)
                                        )
                                        Text(
                                            text = "Advance Held",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyUtils.formatBdt(totalAdvanceAll),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ledgerColors.income,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tidy Section Header + Collapsible Search & Status Filters
        item(key = "due_filter_section") {
            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = if (hasActiveFilter) {
                        "${filteredCustomers.size} of ${customers.size}"
                    } else {
                        "${customers.size}"
                    }
                    Text(
                        text = "Customers · $countLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val toggleBg by animateColorAsState(
                            targetValue = if (isSearchExpanded || hasActiveFilter) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                            },
                            animationSpec = tween(200),
                            label = "dueSearchToggleBg"
                        )
                        val toggleContent by animateColorAsState(
                            targetValue = if (isSearchExpanded || hasActiveFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(200),
                            label = "dueSearchToggleTint"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = toggleBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSearchExpanded || hasActiveFilter) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                                .testTag("toggle_due_search_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchExpanded) "Close customer search and filters" else "Search and filter customers",
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
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sortBy = when (sortBy) {
                                        "DUE_DESC" -> "RECENT"
                                        "RECENT" -> "NAME"
                                        else -> "DUE_DESC"
                                    }
                                }
                                .testTag("due_sort_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort customers",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (sortBy) {
                                        "RECENT" -> "Recent"
                                        "NAME" -> "A–Z"
                                        else -> "Highest"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Active Filter Pill (when collapsed while filters are active)
                AnimatedVisibility(
                    visible = !isSearchExpanded && hasActiveFilter,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
                ) {
                    val activeSummary = buildList {
                        when (statusFilter) {
                            "DUE" -> add("Has Due")
                            "ADVANCE" -> add("Advance")
                        }
                        if (searchQuery.isNotBlank()) add("\"${searchQuery.trim()}\"")
                    }.joinToString(" · ")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("active_due_filter_banner")
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
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    searchQuery = ""
                                    statusFilter = "ALL"
                                },
                                modifier = Modifier.testTag("clear_due_filter_pill_btn")
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Collapsible Search Input & Status Filter Chips
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(140))
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_search_input"),
                            placeholder = { Text("Search name or phone...") },
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
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = statusFilter == "ALL",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    statusFilter = "ALL"
                                },
                                label = { Text("All (${customers.size})", fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("filter_all_cust_chip")
                            )

                            FilterChip(
                                selected = statusFilter == "DUE",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    statusFilter = "DUE"
                                },
                                label = { Text("Due ($dueCount)", fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                    selectedContainerColor = ledgerColors.expenseContainer,
                                    selectedLabelColor = ledgerColors.expense
                                ),
                                modifier = Modifier.testTag("filter_has_due_chip")
                            )

                            FilterChip(
                                selected = statusFilter == "ADVANCE",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    statusFilter = "ADVANCE"
                                },
                                label = { Text("Advance ($advanceCount)", fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                    selectedContainerColor = ledgerColors.incomeContainer,
                                    selectedLabelColor = ledgerColors.income
                                ),
                                modifier = Modifier.testTag("filter_advance_cust_chip")
                            )
                        }
                    }
                }
            }
        }

        // Customer List
        if (filteredCustomers.isEmpty()) {
            val isFilteredOut = customers.isNotEmpty() && hasActiveFilter
            item(key = "due_empty_state") {
                LiquidGlassCard(
                    cornerRadius = 18.dp,
                    accentTint = ledgerColors.dueAmber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isFilteredOut) Icons.Default.Search else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFilteredOut) "No matching customers" else "No customers yet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFilteredOut) "Try a different name, phone number, or reset filters."
                            else "Tap + Customer below to add a customer or advance.",
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
                                    statusFilter = "ALL"
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_due_filters_btn")
                            ) {
                                Text("Reset Filters", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredCustomers, key = { it.id }) { customer ->
                val isDue = customer.currentDue > 0
                val hasAdvance = customer.advanceBalance > 0
                val statusColor = if (isDue) ledgerColors.expense else ledgerColors.income
                val statusContainer = if (isDue) ledgerColors.expenseContainer else ledgerColors.incomeContainer
                val initial = customer.name.trim().firstOrNull()?.uppercaseChar()?.toString()

                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .animateContentSize()
                        .testTag("customer_card_${customer.id}"),
                    cornerRadius = 18.dp,
                    accentTint = statusColor,
                    onClick = { onCustomerClicked(customer) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color = statusContainer, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!initial.isNullOrBlank()) {
                                        Text(
                                            text = initial,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (customer.phone.isNotBlank()) {
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (customer.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${customer.phone}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Call ${customer.name}",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = when {
                                            isDue -> CurrencyUtils.formatBdt(customer.currentDue)
                                            hasAdvance -> CurrencyUtils.formatBdt(customer.advanceBalance)
                                            else -> "Settled"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = when {
                                            isDue -> "Due"
                                            hasAdvance -> "Advance"
                                            else -> "৳0 balance"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ledgerColors.expenseContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAddItemsClicked(customer)
                                    }
                                    .testTag("add_credit_btn_${customer.id}")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = ledgerColors.expense, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Credit",
                                        fontWeight = FontWeight.SemiBold,
                                        color = ledgerColors.expense,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ledgerColors.incomeContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCollectPaymentClicked(customer)
                                    }
                                    .testTag("collect_pay_btn_${customer.id}")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ledgerColors.income, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pay / Advance",
                                        fontWeight = FontWeight.SemiBold,
                                        color = ledgerColors.income,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
