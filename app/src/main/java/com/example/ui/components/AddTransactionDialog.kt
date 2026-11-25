package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
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
import com.example.data.model.TransactionRecord
import com.example.ui.theme.ledgerColors

val INCOME_CATEGORIES = listOf(
    "Daily Sales", "Wholesale", "Services", "Commission", "Other Income"
)

val EXPENSE_CATEGORIES = listOf(
    "Inventory & Stock", "Shop Rent", "Electricity & Bills", "Staff Salary",
    "Transport & Delivery", "Shop Maintenance", "Personal/Drawings", "Other Expense"
)

private val QUICK_AMOUNTS = listOf(100, 500, 1000, 2000, 5000)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    initialType: String = "INCOME",
    lockedType: String? = null,
    existingTransaction: TransactionRecord? = null,
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, note: String, timestamp: Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AddTransactionDialogContent(
            initialType = initialType,
            lockedType = lockedType,
            existingTransaction = existingTransaction,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AddTransactionDialogContent(
    initialType: String = "INCOME",
    lockedType: String? = null,
    existingTransaction: TransactionRecord? = null,
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, note: String, timestamp: Long) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    val isEditing = existingTransaction != null
    val effectiveInitialType = lockedType ?: existingTransaction?.type ?: initialType

    var type by remember(existingTransaction?.id, lockedType) { mutableStateOf(effectiveInitialType) }
    var amountText by remember(existingTransaction?.id) {
        mutableStateOf(existingTransaction?.let { CurrencyUtils.formatEditableAmount(it.amount) } ?: "")
    }
    var category by remember(existingTransaction?.id, effectiveInitialType) {
        val defaults = if (effectiveInitialType == "INCOME") INCOME_CATEGORIES else EXPENSE_CATEGORIES
        val existingCat = existingTransaction?.category
        mutableStateOf(
            if (!existingCat.isNullOrBlank()) existingCat else defaults.first()
        )
    }
    var note by remember(existingTransaction?.id) { mutableStateOf(existingTransaction?.note ?: "") }
    var selectedTimestamp by remember(existingTransaction?.id) {
        mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isIncome = type == "INCOME"
    val incomeColor = ledgerColors.income
    val incomeContainer = ledgerColors.incomeContainer
    val expenseColor = ledgerColors.expense
    val expenseContainer = ledgerColors.expenseContainer

    val headerColor by animateColorAsState(
        targetValue = if (isIncome) incomeColor else expenseColor,
        animationSpec = spring(),
        label = "headerColor"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_transaction_dialog")
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val title = when {
                            isEditing && isIncome -> "Edit Income Entry"
                            isEditing && !isIncome -> "Edit Expense Entry"
                            isIncome -> "Record Income (+ আয়)"
                            else -> "Record Expense (− খরচ)"
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerColor
                        )
                        Text(
                            text = if (isEditing) "Update amount, date, category or note"
                            else if (isIncome) "Add entry to Income ledger"
                            else "Add entry to Expense ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_transaction_dialog_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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

                // Type Switcher only shown when type is NOT locked
                if (lockedType == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        val incomeBgColor by animateColorAsState(
                            targetValue = if (isIncome) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "incomeBg"
                        )
                        val incomeTextColor by animateColorAsState(
                            targetValue = if (isIncome) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(),
                            label = "incomeText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(incomeBgColor)
                                .clickable {
                                    type = "INCOME"
                                    if (category !in INCOME_CATEGORIES) {
                                        category = INCOME_CATEGORIES.first()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = incomeTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Income (আয়)",
                                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                    color = incomeTextColor,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        val expenseBgColor by animateColorAsState(
                            targetValue = if (!isIncome) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "expenseBg"
                        )
                        val expenseTextColor by animateColorAsState(
                            targetValue = if (!isIncome) expenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(),
                            label = "expenseText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(expenseBgColor)
                                .clickable {
                                    type = "EXPENSE"
                                    if (category !in EXPENSE_CATEGORIES) {
                                        category = EXPENSE_CATEGORIES.first()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = expenseTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Expense (খরচ)",
                                    fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Medium,
                                    color = expenseTextColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount in BDT (৳)") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = headerColor,
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
                        .testTag("transaction_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = headerColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage != null
                )

                // Quick Amount Addition Chips (preserves decimals & handles pasted commas)
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
                                .testTag("quick_amount_$quickVal")
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

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Date Picker Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .testTag("transaction_date_picker_btn")
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
                                tint = headerColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Transaction Date",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selection
                Text(
                    text = "Category (ক্যাটাগরি)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val baseCategories = if (isIncome) INCOME_CATEGORIES else EXPENSE_CATEGORIES
                val categories = if (category !in baseCategories && category.isNotBlank()) {
                    baseCategories + category
                } else {
                    baseCategories
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        val catBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isIncome) incomeContainer else expenseContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            label = "catBg"
                        )
                        val catBorder = if (isSelected) (if (isIncome) incomeColor else expenseColor) else Color.Transparent

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = catBg,
                            border = if (isSelected) BorderStroke(1.dp, catBorder) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    category = cat
                                }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) (if (isIncome) incomeColor else expenseColor) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Field
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_NOTE_LENGTH) {
                            note = it
                        }
                    },
                    label = { Text("Note / Memo (বিবরণ - ঐচ্ছিক)") },
                    placeholder = { Text("e.g. Morning counter sales, Invoice #45") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = headerColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                val previewAmount = CurrencyUtils.parseAmountInput(amountText)?.takeIf { it > 0.0 } ?: 0.0
                Button(
                    onClick = {
                        val amount = CurrencyUtils.parseAmountInput(amountText)
                        when {
                            amount == null || !amount.isFinite() || amount <= 0.0 -> {
                                errorMessage = "Please enter an amount greater than 0"
                            }
                            amount > FirestoreBusinessRepository.MAX_AMOUNT -> {
                                errorMessage = "Amount cannot exceed ৳1,000,000,000"
                            }
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSave(
                                    type,
                                    category,
                                    CurrencyUtils.roundCurrency(amount),
                                    note.trim(),
                                    selectedTimestamp
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) incomeColor else expenseColor,
                        contentColor = Color.White
                    )
                ) {
                    val formattedBtnAmount = CurrencyUtils.formatBdt(previewAmount)
                    val buttonText = when {
                        isEditing -> "Save Changes ($formattedBtnAmount)"
                        isIncome -> "Confirm Income (+ $formattedBtnAmount)"
                        else -> "Confirm Expense (− $formattedBtnAmount)"
                    }
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
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
                    modifier = Modifier.testTag("confirm_date_picker_btn")
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
