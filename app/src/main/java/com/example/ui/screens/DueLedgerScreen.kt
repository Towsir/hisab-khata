package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.DueAmber
import com.example.ui.theme.DueAmberContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer

@Composable
fun DueLedgerScreen(
    customers: List<CustomerEntity>,
    onCustomerClicked: (CustomerEntity) -> Unit,
    onAddItemsClicked: (CustomerEntity) -> Unit,
    onCollectPaymentClicked: (CustomerEntity) -> Unit,
    onAddNewCustomerClicked: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("HAS_DUE") } // "HAS_DUE", "ALL", "PAID"
    var sortBy by remember { mutableStateOf("DUE_DESC") } // "DUE_DESC", "RECENT", "NAME"

    val totalDueAll = customers.sumOf { it.currentDue }
    val totalCreditAll = customers.sumOf { it.totalCredit }
    val totalPaidAll = customers.sumOf { it.totalPaid }
    val countDebtors = customers.count { it.currentDue > 0 }

    val filteredCustomers = customers
        .filter { customer ->
            val matchesSearch = customer.name.contains(searchQuery, ignoreCase = true) ||
                    customer.phone.contains(searchQuery) ||
                    customer.address.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (statusFilter) {
                "HAS_DUE" -> customer.currentDue > 0
                "PAID" -> customer.currentDue <= 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
        .sortedWith { a, b ->
            when (sortBy) {
                "RECENT" -> b.lastUpdated.compareTo(a.lastUpdated)
                "NAME" -> a.name.compareTo(b.name, ignoreCase = true)
                else -> b.currentDue.compareTo(a.currentDue) // "DUE_DESC"
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewCustomerClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("due_ledger_screen"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Aggregate Due Banner (Refined Neutral Look)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_due_banner"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Outstanding Due (মোট বাকি)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = if (countDebtors > 0) DueAmberContainer else IncomeGreenContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$countDebtors Active Debtors",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (countDebtors > 0) DueAmber else IncomeGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = CurrencyUtils.formatBdt(totalDueAll),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalDueAll > 0) ExpenseRed else IncomeGreen,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Credit Given",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = CurrencyUtils.formatBdt(totalCreditAll),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Paid Back",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = CurrencyUtils.formatBdt(totalPaidAll),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search & Sort Bar
            item {
                Column(modifier = Modifier.animateContentSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input"),
                        placeholder = { Text("Search by name, phone or village...") },
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
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters and Sorting Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = statusFilter == "HAS_DUE",
                                onClick = { statusFilter = "HAS_DUE" },
                                label = { Text("Has Due (${customers.count { it.currentDue > 0 }})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ExpenseRedContainer,
                                    selectedLabelColor = ExpenseRed
                                ),
                                modifier = Modifier.testTag("filter_has_due_chip")
                            )

                            FilterChip(
                                selected = statusFilter == "ALL",
                                onClick = { statusFilter = "ALL" },
                                label = { Text("All (${customers.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("filter_all_cust_chip")
                            )

                            FilterChip(
                                selected = statusFilter == "PAID",
                                onClick = { statusFilter = "PAID" },
                                label = { Text("Paid (${customers.count { it.currentDue <= 0 }})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IncomeGreenContainer,
                                    selectedLabelColor = IncomeGreen
                                ),
                                modifier = Modifier.testTag("filter_paid_cust_chip")
                            )
                        }

                        // Cycle Sort
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sortBy = when (sortBy) {
                                        "DUE_DESC" -> "RECENT"
                                        "RECENT" -> "NAME"
                                        else -> "DUE_DESC"
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (sortBy) {
                                        "RECENT" -> "Recent"
                                        "NAME" -> "A-Z"
                                        else -> "Highest"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Customer Cards
            if (filteredCustomers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No customers found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ New Customer' to add an account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val isDue = customer.currentDue > 0
                    val repaymentRatio = if (customer.totalCredit > 0) {
                        (customer.totalPaid / customer.totalCredit).toFloat().coerceIn(0f, 1f)
                    } else 1f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onCustomerClicked(customer) }
                            .testTag("customer_card_${customer.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Customer Info + Calling Shortcut
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
                                            .size(42.dp)
                                            .background(
                                                color = if (isDue) ExpenseRedContainer else IncomeGreenContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isDue) ExpenseRed else IncomeGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (customer.phone.isNotBlank()) {
                                            Text(
                                                text = customer.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${customer.phone}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Call",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isDue) CurrencyUtils.formatBdt(customer.currentDue) else "Paid ✓",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDue) ExpenseRed else IncomeGreen
                                        )
                                        Text(
                                            text = if (isDue) "Remaining Due" else "Zero Balance",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Repayment Ratio Progress
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Credit: ${CurrencyUtils.formatBdt(customer.totalCredit)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Paid: ${CurrencyUtils.formatBdt(customer.totalPaid)} (${(repaymentRatio * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { repaymentRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = IncomeGreen,
                                    trackColor = ExpenseRed.copy(alpha = 0.2f),
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ExpenseRedContainer,
                                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAddItemsClicked(customer) }
                                        .testTag("add_credit_btn_${customer.id}")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Add Items (বাকি)",
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = IncomeGreenContainer,
                                    border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onCollectPaymentClicked(customer) }
                                        .testTag("collect_pay_btn_${customer.id}")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Collect Pay (জমা)",
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen,
                                            fontSize = 12.sp
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
}
