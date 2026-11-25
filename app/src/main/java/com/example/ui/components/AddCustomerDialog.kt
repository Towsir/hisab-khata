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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.ledgerColors

private val CUSTOMER_QUICK_AMOUNTS = listOf(100, 500, 1000, 2000, 5000)

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, initialAmount: Double, initialNote: String, isInitialAdvance: Boolean) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialAmountText by remember { mutableStateOf("") }
    var initialNote by remember { mutableStateOf("") }
    var isInitialAdvance by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_customer_dialog")
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
                        text = "New Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_customer_dialog_btn")
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_NAME_LENGTH) {
                            name = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Name *") },
                    placeholder = { Text("e.g. Md. Tariqul Islam") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_PHONE_LENGTH) phone = it
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 017xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_ADDRESS_LENGTH) address = it
                    },
                    label = { Text("Address (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Opening Balance Type: Due vs Advance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    val dueBg by animateColorAsState(
                        targetValue = if (!isInitialAdvance) MaterialTheme.colorScheme.surface else Color.Transparent,
                        animationSpec = spring(),
                        label = "dueBg"
                    )
                    val advBg by animateColorAsState(
                        targetValue = if (isInitialAdvance) MaterialTheme.colorScheme.surface else Color.Transparent,
                        animationSpec = spring(),
                        label = "advBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(dueBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isInitialAdvance = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Opening Due",
                            fontWeight = if (!isInitialAdvance) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!isInitialAdvance) ledgerColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(advBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isInitialAdvance = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Advance (অগ্রিম)",
                            fontWeight = if (isInitialAdvance) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isInitialAdvance) ledgerColors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = initialAmountText,
                    onValueChange = {
                        initialAmountText = it
                        errorMessage = null
                    },
                    label = {
                        Text(if (isInitialAdvance) "Advance Amount (৳ Optional)" else "Starting Due (৳ Optional)")
                    },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_initial_due_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isInitialAdvance) ledgerColors.income else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CUSTOMER_QUICK_AMOUNTS.forEach { quickVal ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val current = CurrencyUtils.parseAmountInput(initialAmountText) ?: 0.0
                                    val nextVal = CurrencyUtils.roundCurrency(
                                        (current + quickVal).coerceAtMost(FirestoreBusinessRepository.MAX_AMOUNT)
                                    )
                                    initialAmountText = CurrencyUtils.formatEditableAmount(nextVal)
                                    errorMessage = null
                                }
                                .testTag("customer_quick_amount_$quickVal")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
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

                if ((CurrencyUtils.parseAmountInput(initialAmountText) ?: 0.0) > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = initialNote,
                        onValueChange = {
                            if (it.length <= FirestoreBusinessRepository.MAX_NOTE_LENGTH) initialNote = it
                        },
                        label = { Text("Note (Optional)") },
                        placeholder = {
                            Text(if (isInitialAdvance) "e.g. Booking deposit" else "e.g. 2 bags rice")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_initial_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val trimmed = initialAmountText.trim()
                        val parsedAmount = if (trimmed.isEmpty()) 0.0 else CurrencyUtils.parseAmountInput(trimmed)
                        when {
                            name.trim().isBlank() -> errorMessage = "Please enter customer name"
                            parsedAmount == null || !parsedAmount.isFinite() || parsedAmount < 0.0 ->
                                errorMessage = "Please enter a valid starting amount"
                            parsedAmount > FirestoreBusinessRepository.MAX_AMOUNT ->
                                errorMessage = "Amount cannot exceed ৳1,000,000,000"
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSave(
                                    name.trim(),
                                    phone.trim(),
                                    address.trim(),
                                    CurrencyUtils.roundCurrency(parsedAmount),
                                    initialNote.trim(),
                                    isInitialAdvance
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_customer_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Customer", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
