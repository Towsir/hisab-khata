package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirestoreBusinessRepository
import com.example.data.model.CustomerRecord
import com.example.ui.theme.ledgerColors

enum class DueAdjustmentMode {
    ADD_ITEMS,       // Customer takes goods on credit
    COLLECT_PAYMENT, // Customer pays toward due (any surplus becomes advance)
    ADVANCE_PAYMENT  // Customer leaves advance payment deposit
}

private val QUICK_AMOUNTS = listOf(100, 500, 1000, 2000, 5000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustDueDialog(
    customer: CustomerRecord,
    mode: DueAdjustmentMode,
    onDismiss: () -> Unit,
    onAddCredit: (customerId: String, amount: Double, itemDescription: String, timestamp: Long) -> Unit,
    onRecordPayment: (customerId: String, amount: Double, note: String, isAdvance: Boolean, timestamp: Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AdjustDueDialogContent(
            customer = customer,
            mode = mode,
            onDismiss = onDismiss,
            onAddCredit = onAddCredit,
            onRecordPayment = onRecordPayment
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdjustDueDialogContent(
    customer: CustomerRecord,
    mode: DueAdjustmentMode,
    onDismiss: () -> Unit,
    onAddCredit: (customerId: String, amount: Double, itemDescription: String, timestamp: Long) -> Unit,
    onRecordPayment: (customerId: String, amount: Double, note: String, isAdvance: Boolean, timestamp: Long) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    var activeMode by remember(mode, customer.id) { mutableStateOf(mode) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTimestamp by remember(customer.id, mode) { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountDouble = CurrencyUtils.parseAmountInput(amountText)?.takeIf { it > 0.0 } ?: 0.0
    val isAdd = activeMode == DueAdjustmentMode.ADD_ITEMS
    val isAdvance = activeMode == DueAdjustmentMode.ADVANCE_PAYMENT

    // Live projected net due / advance preview (rounded cleanly to 2 decimals)
    val projectedNetDue = CurrencyUtils.roundCurrency(
        if (isAdd) {
            (customer.totalCredit + amountDouble) - customer.totalPaid
        } else {
            customer.totalCredit - (customer.totalPaid + amountDouble)
        }
    )

    val actionColor = if (isAdd) ledgerColors.expense else ledgerColors.income

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adjust_due_dialog")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (activeMode) {
                                DueAdjustmentMode.ADD_ITEMS -> "Add Credit (+ বাকি)"
                                DueAdjustmentMode.ADVANCE_PAYMENT -> "Advance Payment (অগ্রিম)"
                                DueAdjustmentMode.COLLECT_PAYMENT -> "Due Payment (জমা)"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_adjust_due_btn")
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Clean segmented toggle between Due Payment and Advance when paying
                if (!isAdd) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        val payBg by animateColorAsState(
                            targetValue = if (!isAdvance) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "payBg"
                        )
                        val advBg by animateColorAsState(
                            targetValue = if (isAdvance) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "advBg"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(payBg)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeMode = DueAdjustmentMode.COLLECT_PAYMENT
                                }
                                .padding(vertical = 10.dp)
                                .testTag("mode_pay_due_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pay Due",
                                fontWeight = if (!isAdvance) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isAdvance) ledgerColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(advBg)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeMode = DueAdjustmentMode.ADVANCE_PAYMENT
                                }
                                .padding(vertical = 10.dp)
                                .testTag("mode_advance_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Advance (অগ্রিম)",
                                fontWeight = if (isAdvance) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isAdvance) ledgerColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Current -> Updated Status Strip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (customer.advanceBalance > 0) "Current Advance" else "Current Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (customer.advanceBalance > 0) CurrencyUtils.formatBdt(customer.advanceBalance)
                                else CurrencyUtils.formatBdt(customer.currentDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.advanceBalance > 0) ledgerColors.income else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val newLabel = when {
                                projectedNetDue > 0 -> "New Due"
                                projectedNetDue < 0 -> "Advance Balance"
                                else -> "Settled"
                            }
                            val newValue = when {
                                projectedNetDue > 0 -> CurrencyUtils.formatBdt(projectedNetDue)
                                projectedNetDue < 0 -> CurrencyUtils.formatBdt(-projectedNetDue)
                                else -> "৳0"
                            }
                            Text(
                                text = newLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = newValue,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (projectedNetDue > 0) ledgerColors.expense else ledgerColors.income,
                                modifier = Modifier.testTag("projected_balance_value")
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
                    label = {
                        Text(
                            when (activeMode) {
                                DueAdjustmentMode.ADD_ITEMS -> "Credit Amount (৳)"
                                DueAdjustmentMode.ADVANCE_PAYMENT -> "Advance Amount (৳)"
                                DueAdjustmentMode.COLLECT_PAYMENT -> "Payment Amount (৳)"
                            }
                        )
                    },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = actionColor,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        if (amountText.isNotEmpty()) {
                            IconButton(onClick = { amountText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_due_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = actionColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage != null
                )

                // One-tap Full Due shortcut when collecting payment for a customer with an active due
                if (!isAdd && customer.currentDue > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ledgerColors.incomeContainer,
                        border = BorderStroke(1.dp, ledgerColors.income.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                amountText = CurrencyUtils.formatEditableAmount(customer.currentDue)
                                activeMode = DueAdjustmentMode.COLLECT_PAYMENT
                                errorMessage = null
                            }
                            .testTag("preset_full_due_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = ledgerColors.income,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Full Due (${CurrencyUtils.formatBdt(customer.currentDue)})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ledgerColors.income
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_AMOUNTS.forEach { quickVal ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val current = CurrencyUtils.parseAmountInput(amountText) ?: 0.0
                                    val nextVal = CurrencyUtils.roundCurrency(
                                        (current + quickVal).coerceAtMost(FirestoreBusinessRepository.MAX_AMOUNT)
                                    )
                                    amountText = CurrencyUtils.formatEditableAmount(nextVal)
                                    errorMessage = null
                                }
                                .testTag("adjust_quick_amount_$quickVal")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$quickVal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Entry Date Picker Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showDatePicker = true
                        }
                        .testTag("adjust_due_date_picker_btn")
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
                                tint = actionColor,
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
                        if (it.length <= FirestoreBusinessRepository.MAX_NOTE_LENGTH) description = it
                    },
                    label = { Text("Note (Optional)") },
                    placeholder = {
                        Text(
                            when (activeMode) {
                                DueAdjustmentMode.ADD_ITEMS -> "e.g. Rice 10kg"
                                DueAdjustmentMode.ADVANCE_PAYMENT -> "e.g. Advance booking"
                                DueAdjustmentMode.COLLECT_PAYMENT -> "e.g. Cash / bKash"
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_due_note_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = actionColor,
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
                                val roundedAmt = CurrencyUtils.roundCurrency(amt)
                                if (isAdd) {
                                    val itemDesc = description.trim().ifBlank { "Goods taken on credit" }
                                    onAddCredit(customer.id, roundedAmt, itemDesc, selectedTimestamp)
                                } else {
                                    val payDesc = description.trim().ifBlank {
                                        if (isAdvance) "Advance Payment" else "Cash Payment"
                                    }
                                    onRecordPayment(customer.id, roundedAmt, payDesc, isAdvance, selectedTimestamp)
                                }
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_adjust_due_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor,
                        contentColor = Color.White
                    )
                ) {
                    val btnLabel = when (activeMode) {
                        DueAdjustmentMode.ADD_ITEMS -> "Add Credit ${CurrencyUtils.formatBdt(amountDouble)}"
                        DueAdjustmentMode.ADVANCE_PAYMENT -> "Save Advance ${CurrencyUtils.formatBdt(amountDouble)}"
                        DueAdjustmentMode.COLLECT_PAYMENT -> "Save Payment ${CurrencyUtils.formatBdt(amountDouble)}"
                    }
                    Text(
                        text = btnLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
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
                    },
                    modifier = Modifier.testTag("confirm_adjust_due_date_btn")
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
