package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer

private val INCOME_CATEGORIES = listOf(
    "Daily Sales", "Wholesale", "Services", "Due Collection", "Other Income"
)

private val EXPENSE_CATEGORIES = listOf(
    "Inventory & Stock", "Shop Rent", "Electricity & Bills", "Staff Salary",
    "Transport & Delivery", "Shop Maintenance", "Personal/Drawings", "Other Expense"
)

private val QUICK_AMOUNTS = listOf(100, 500, 1000, 2000, 5000)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialType: String = "INCOME",
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, note: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(if (initialType == "INCOME") INCOME_CATEGORIES.first() else EXPENSE_CATEGORIES.first())
    }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isIncome = type == "INCOME"

    val headerColor by animateColorAsState(
        targetValue = if (isIncome) IncomeGreen else ExpenseRed,
        animationSpec = spring(),
        label = "headerColor"
    )

    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(22.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isIncome) "Record Income (+ আয়)" else "Record Expense (− খরচ)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerColor
                        )
                        Text(
                            text = "Add entry to business ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("close_transaction_dialog_btn")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Income / Expense Animated Segmented Bar
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
                        targetValue = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                category = INCOME_CATEGORIES.first()
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
                        targetValue = if (!isIncome) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                category = EXPENSE_CATEGORIES.first()
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

                // Quick Amount Addition Chips for Effortless Fast Entry
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_AMOUNTS.forEach { quickVal ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (current + quickVal).toInt().toString()
                                    errorMessage = null
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$quickVal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
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

                // Category Selection
                Text(
                    text = "Category (ক্যাটাগরি)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val categories = if (isIncome) INCOME_CATEGORIES else EXPENSE_CATEGORIES
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        val catBg by animateColorAsState(
                            targetValue = if (isSelected) (if (isIncome) IncomeGreenContainer else ExpenseRedContainer) else MaterialTheme.colorScheme.surfaceVariant,
                            label = "catBg"
                        )
                        val catBorder = if (isSelected) (if (isIncome) IncomeGreen else ExpenseRed) else Color.Transparent

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = catBg,
                            border = if (isSelected) BorderStroke(1.dp, catBorder) else null,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) (if (isIncome) IncomeGreen else ExpenseRed) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
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

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Please enter an amount greater than 0"
                        } else {
                            onSave(type, category, amount, note.trim())
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) IncomeGreen else ExpenseRed
                    )
                ) {
                    Text(
                        text = if (isIncome) "Confirm Income (+ ৳${amountText.ifBlank { "0" }})" else "Confirm Expense (− ৳${amountText.ifBlank { "0" }})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
