package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.auth.formatBangladeshiPhoneDisplay
import com.example.auth.normalizeBangladeshiPhone
import com.example.ui.theme.ledgerColors

enum class PhoneAuthMode(
    val title: String,
    val subtitle: String,
    val primaryCta: String
) {
    LOGIN(
        title = "Log In with Phone",
        subtitle = "Enter your Bangladeshi mobile (+880), then verify with Google to access your cloud ledger.",
        primaryCta = "Continue"
    ),
    CREATE_ACCOUNT(
        title = "Create Account",
        subtitle = "Enter your Bangladeshi mobile (+880), verify with Google, then add your shop name.",
        primaryCta = "Continue"
    )
}

@Composable
fun PhoneAuthDialog(
    mode: PhoneAuthMode,
    isBusy: Boolean = false,
    onDismiss: () -> Unit,
    onPhoneConfirmed: (normalizedE164: String) -> Unit
) {
    var phoneInput by remember(mode) { mutableStateOf("") }
    var errorMessage by remember(mode) { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        PhoneAuthDialogContent(
            mode = mode,
            phoneInput = phoneInput,
            isBusy = isBusy,
            errorMessage = errorMessage,
            onPhoneInputChange = {
                if (it.length <= 20) {
                    phoneInput = it
                    errorMessage = null
                }
            },
            onContinueClicked = {
                val normalized = normalizeBangladeshiPhone(phoneInput)
                if (normalized == null) {
                    errorMessage = "Enter a valid Bangladeshi mobile number (e.g. 01712-345678)."
                } else {
                    errorMessage = null
                    onPhoneConfirmed(normalized)
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun PhoneAuthDialogContent(
    mode: PhoneAuthMode,
    phoneInput: String,
    isBusy: Boolean,
    errorMessage: String?,
    onPhoneInputChange: (String) -> Unit,
    onContinueClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    val normalizedPreview = remember(phoneInput) { normalizeBangladeshiPhone(phoneInput) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_auth_dialog")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("phone_auth_close_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneInputChange,
                label = { Text("Bangladeshi Mobile") },
                placeholder = { Text("01712-345678") },
                prefix = {
                    Text(
                        text = "+880 ",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_auth_number_input")
            )

            AnimatedVisibility(visible = normalizedPreview != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("phone_auth_formatted_preview")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ledgerColors.income,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatBangladeshiPhoneDisplay(normalizedPreview.orEmpty()),
                        style = MaterialTheme.typography.labelSmall,
                        color = ledgerColors.income,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ledgerColors.expenseContainer,
                    border = BorderStroke(1.dp, ledgerColors.expense.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("phone_auth_error_banner")
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ledgerColors.expense,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onContinueClicked()
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("phone_auth_continue_btn")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Continuing...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                } else {
                    Text(mode.primaryCta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
