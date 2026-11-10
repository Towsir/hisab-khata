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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CustomerEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

enum class DueAdjustmentMode {
    ADD_ITEMS,      // Customer takes more goods on credit
    COLLECT_PAYMENT // Customer pays off part or all of their balance
}

private val QUICK_AMOUNTS = listOf(100, 500, 1000, 2000, 5000)

@Composable
fun AdjustDueDialog(
    customer: CustomerEntity,
    mode: DueAdjustmentMode,
    onDismiss: () -> Unit,
    onAddCredit: (customerId: Long, amount: Double, itemDescription: String) -> Unit,
    onRecordPayment: (customerId: Long, amount: Double, note: String, addToIncome: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var addToIncome by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
    val isAdd = mode == DueAdjustmentMode.ADD_ITEMS

    val calculatedNewDue = if (isAdd) {
        customer.currentDue + amountDouble
    } else {
        maxOf(0.0, customer.currentDue - amountDouble)
    }

    val actionColor = if (isAdd) ExpenseRed else IncomeGreen

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adjust_due_dialog")
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
                            text = if (isAdd) "Add Goods (+ বাকি)" else "Collect Payment (− জমা)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("close_adjust_due_btn")
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

                // Interactive Balance Transformation Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Due (বর্তমান বাকি)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatBdt(customer.currentDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (isAdd) "+" else "−",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = actionColor
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Updated Due (নতুন বাকি)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatBdt(calculatedNewDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (calculatedNewDue > 0) ExpenseRed else IncomeGreen
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
                    label = { Text(if (isAdd) "Amount for items taken (৳) *" else "Amount paid by customer (৳) *") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
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

                // Quick Amount Addition Chips
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

                // Item description / Payment notes
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isAdd) "Products taken (e.g. Rice 10kg, Dal 2kg)" else "Payment note (e.g. Cash / bKash)") },
                    placeholder = { Text(if (isAdd) "Item names or invoice notes" else "Cash received") },
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

                if (!isAdd) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { addToIncome = !addToIncome }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = addToIncome,
                                onCheckedChange = { addToIncome = it },
                                colors = CheckboxDefaults.colors(checkedColor = IncomeGreen),
                                modifier = Modifier.testTag("add_to_income_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add to Today's Cash Flow Income (+ আয়)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            errorMessage = "Please enter an amount greater than 0"
                        } else {
                            if (isAdd) {
                                val itemDesc = if (description.isNotBlank()) description.trim() else "Goods taken on credit"
                                onAddCredit(customer.id, amt, itemDesc)
                            } else {
                                val payDesc = if (description.isNotBlank()) description.trim() else "Cash Payment"
                                onRecordPayment(customer.id, amt, payDesc, addToIncome)
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_adjust_due_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor)
                ) {
                    Text(
                        text = if (isAdd) "Confirm +${CurrencyUtils.formatBdt(amountDouble)}" else "Confirm −${CurrencyUtils.formatBdt(amountDouble)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
