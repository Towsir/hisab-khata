package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirestoreBusinessRepository
import com.example.data.model.CustomerRecord
import com.example.data.model.DueEntryRecord
import com.example.ui.theme.ledgerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryBottomSheet(
    customer: CustomerRecord,
    entries: List<DueEntryRecord>,
    shopName: String = "Hishab Khata",
    onDismiss: () -> Unit,
    onAddCreditClicked: () -> Unit,
    onCollectPaymentClicked: () -> Unit,
    onAdvancePaymentClicked: () -> Unit,
    onEditCustomer: (customerId: String, name: String, phone: String, address: String) -> Unit,
    onDeleteCustomer: (customerId: String) -> Unit,
    onEditDueEntry: (entryId: String, type: String, amount: Double, itemsDescription: String, timestamp: Long) -> Unit,
    onDeleteDueEntry: (entryId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ledgerColors = MaterialTheme.ledgerColors
    val hasDue = customer.currentDue > 0
    val hasAdvance = customer.advanceBalance > 0

    val lastCreditEntry = remember(entries) { CustomerMessageFormatter.findLastCreditEntry(entries) }
    val lastPaidEntry = remember(entries) { CustomerMessageFormatter.findLastPaidEntry(entries) }
    val miniStatement = remember(shopName, customer, entries) {
        CustomerMessageFormatter.buildCustomerMiniStatement(
            shopName = shopName,
            customer = customer,
            entries = entries
        )
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<DueEntryRecord?>(null) }
    var entryToDelete by remember { mutableStateOf<DueEntryRecord?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("customer_history_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Minimal Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_customer_btn")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Customer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    val subtitle = listOfNotNull(
                        customer.phone.takeIf { it.isNotBlank() },
                        customer.address.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("call_customer_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call Customer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${Uri.encode(customer.phone)}")
                                    putExtra("sms_body", miniStatement)
                                }
                                try {
                                    context.startActivity(smsIntent)
                                } catch (_: Exception) {
                                    val fallback = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, miniStatement)
                                    }
                                    context.startActivity(Intent.createChooser(fallback, "Send Customer Message"))
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sms_due_receipt_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ledgerColors.incomeContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Sms,
                                    contentDescription = "Send SMS Statement",
                                    tint = ledgerColors.income,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, miniStatement)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Customer Statement"))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_due_receipt_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Statement",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteConfirmDialog = true
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_customer_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ledgerColors.expenseContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Account",
                                tint = ledgerColors.expense,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_sheet_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean Balance Summary Card + Last Taken Credit & Last Paid Glance
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (hasAdvance) "Advance Balance (অগ্রিম)" else "Due Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (hasAdvance) CurrencyUtils.formatBdt(customer.advanceBalance)
                                else CurrencyUtils.formatBdt(customer.currentDue),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDue) ledgerColors.expense else ledgerColors.income
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Credit: ${CurrencyUtils.formatBdt(customer.totalCredit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Paid: ${CurrencyUtils.formatBdt(customer.totalPaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ledgerColors.income
                            )
                        }
                    }

                    if (lastCreditEntry != null || lastPaidEntry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("last_activity_glance")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Last Credit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CustomerMessageFormatter.formatEntryAmount(lastCreditEntry),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lastCreditEntry != null) ledgerColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val creditMeta = CustomerMessageFormatter.formatEntryMeta(lastCreditEntry)
                                    if (creditMeta.isNotBlank()) {
                                        Text(
                                            text = creditMeta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Last Paid",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CustomerMessageFormatter.formatEntryAmount(lastPaidEntry),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lastPaidEntry != null) ledgerColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val paidMeta = CustomerMessageFormatter.formatEntryMeta(lastPaidEntry)
                                    if (paidMeta.isNotBlank()) {
                                        Text(
                                            text = paidMeta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Crisp Action Pills: + Credit, Pay Due, Advance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddCreditClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_add_goods_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ledgerColors.expense,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Credit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }

                Button(
                    onClick = onCollectPaymentClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_collect_pay_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ledgerColors.income,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Due", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onAdvancePaymentClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_advance_pay_btn"),
                    border = BorderStroke(1.dp, ledgerColors.income.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ledgerColors.income),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Advance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History (${entries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap entry to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No entries recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val isGoods = entry.type == "GOODS_TAKEN"
                        val isAdvance = entry.type == "ADVANCE_PAYMENT"
                        val entryColor = if (isGoods) ledgerColors.expense else ledgerColors.income
                        val entryContainer = if (isGoods) ledgerColors.expenseContainer else ledgerColors.incomeContainer
                        val defaultTitle = when {
                            isGoods -> "Goods on credit"
                            isAdvance -> "Advance payment"
                            else -> "Due payment"
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { entryToEdit = entry }
                                .testTag("due_entry_card_${entry.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(color = entryContainer, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when {
                                                isGoods -> "+"
                                                isAdvance -> "★"
                                                else -> "−"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            color = entryColor,
                                            fontSize = if (isAdvance) 14.sp else 17.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.itemsDescription.ifBlank { defaultTitle },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isAdvance) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = ledgerColors.incomeContainer
                                                ) {
                                                    Text(
                                                        text = "Advance",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ledgerColors.income,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = CurrencyUtils.formatDateTime(entry.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = (if (isGoods) "+ " else "− ") + CurrencyUtils.formatBdt(entry.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = entryColor,
                                        maxLines = 1
                                    )

                                    IconButton(
                                        onClick = { entryToEdit = entry },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("edit_due_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Entry",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            entryToDelete = entry
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("delete_due_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Entry",
                                            tint = ledgerColors.expense.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Edit Due Entry Dialog (with Date Picker)
    entryToEdit?.let { entry ->
        EditDueEntryDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { newType, newAmount, newDesc, newTimestamp ->
                onEditDueEntry(entry.id, newType, newAmount, newDesc, newTimestamp)
                entryToEdit = null
            }
        )
    }

    // Delete Due Entry Confirmation Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Remove this ${CurrencyUtils.formatBdt(entry.amount)} entry? Balances will update automatically.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDueEntry(entry.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ledgerColors.expense,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Customer Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Customer?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently remove ${customer.name} and all their ledger entries.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteCustomer(customer.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ledgerColors.expense,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Customer Info Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(customer.name) }
        var editPhone by remember { mutableStateOf(customer.phone) }
        var editAddress by remember { mutableStateOf(customer.address) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { if (it.length <= FirestoreBusinessRepository.MAX_NAME_LENGTH) editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { if (it.length <= FirestoreBusinessRepository.MAX_PHONE_LENGTH) editPhone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { if (it.length <= FirestoreBusinessRepository.MAX_ADDRESS_LENGTH) editAddress = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onEditCustomer(customer.id, editName.trim(), editPhone.trim(), editAddress.trim())
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDueEntryDialog(
    entry: DueEntryRecord,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, description: String, timestamp: Long) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    var entryType by remember(entry.id) { mutableStateOf(entry.type) }
    var amountText by remember(entry.id) {
        mutableStateOf(CurrencyUtils.formatEditableAmount(entry.amount))
    }
    var description by remember(entry.id) { mutableStateOf(entry.itemsDescription) }
    var selectedTimestamp by remember(entry.id) { mutableLongStateOf(entry.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isGoods = entryType == "GOODS_TAKEN"
    val isAdvance = entryType == "ADVANCE_PAYMENT"
    val accentColor = if (isGoods) ledgerColors.expense else ledgerColors.income

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_due_entry_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean 3-option segmented selector: Credit / Payment / Advance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    val options = listOf(
                        Triple("GOODS_TAKEN", "Credit", ledgerColors.expense),
                        Triple("PAYMENT_RECEIVED", "Payment", ledgerColors.income),
                        Triple("ADVANCE_PAYMENT", "Advance", ledgerColors.income)
                    )
                    options.forEach { (typeKey, label, selectedColor) ->
                        val selected = entryType == typeKey
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "editEntryType_$typeKey"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable { entryType = typeKey }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_due_entry_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Date Selector Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .testTag("edit_due_entry_date_picker_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Entry Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyUtils.formatRelativeDate(selectedTimestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_NOTE_LENGTH) {
                            description = it
                        }
                    },
                    label = {
                        Text(
                            when {
                                isGoods -> "Items Note"
                                isAdvance -> "Advance Note"
                                else -> "Payment Note"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_due_entry_note_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val amt = CurrencyUtils.parseAmountInput(amountText)
                        when {
                            amt == null || !amt.isFinite() || amt <= 0.0 -> {
                                errorMessage = "Please enter an amount greater than 0"
                            }
                            amt > FirestoreBusinessRepository.MAX_AMOUNT -> {
                                errorMessage = "Amount cannot exceed ৳1,000,000,000"
                            }
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val finalDesc = description.trim().ifBlank {
                                    when {
                                        isGoods -> "Goods taken on credit"
                                        isAdvance -> "Advance Payment"
                                        else -> "Cash Payment"
                                    }
                                }
                                onSave(entryType, CurrencyUtils.roundCurrency(amt), finalDesc, selectedTimestamp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_due_entry_edit_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = CurrencyUtils.localMillisToUtcDateMillis(selectedTimestamp)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            selectedTimestamp = CurrencyUtils.mergeUtcDateWithLocalTime(
                                utcDateMillis = pickedUtc,
                                referenceLocalMillis = selectedTimestamp
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
