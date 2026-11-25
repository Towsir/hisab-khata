package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GeneratedLinkCode
import com.example.data.model.LinkedDeviceRecord
import com.example.data.util.ShopLinkCrypto
import com.example.ui.theme.ledgerColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EmailLoginMode(val label: String) {
    GOOGLE_EMAIL("Google Email"),
    EMAIL_AND_CODE("Email + Code")
}

/**
 * Login-screen modal allowing returning shopkeepers or secondary devices to log in to an
 * existing shop profile either via their existing Google email or via Shop Email + 6-Digit Code.
 */
@Composable
fun EmailLoginDialog(
    isLoading: Boolean,
    initialMode: EmailLoginMode = EmailLoginMode.EMAIL_AND_CODE,
    onDismiss: () -> Unit,
    onContinueWithGoogleEmail: () -> Unit,
    onContinueWithEmailAndCode: (email: String, code: String) -> Unit
) {
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        EmailLoginDialogContent(
            isLoading = isLoading,
            initialMode = initialMode,
            onDismiss = onDismiss,
            onContinueWithGoogleEmail = onContinueWithGoogleEmail,
            onContinueWithEmailAndCode = onContinueWithEmailAndCode
        )
    }
}

@Composable
fun EmailLoginDialogContent(
    isLoading: Boolean,
    initialMode: EmailLoginMode = EmailLoginMode.EMAIL_AND_CODE,
    onDismiss: () -> Unit,
    onContinueWithGoogleEmail: () -> Unit,
    onContinueWithEmailAndCode: (email: String, code: String) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptics = LocalHapticFeedback.current
    var mode by remember { mutableStateOf(initialMode) }
    var shopEmail by remember { mutableStateOf("") }
    var linkCode by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_login_dialog")
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log In with Existing Email",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Access your shop ledger on another device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_email_login_dialog_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Segmented Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EmailLoginMode.entries.forEach { option ->
                        val selected = mode == option
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            animationSpec = spring(),
                            label = "emailLoginTabBg_${option.name}"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(),
                            label = "emailLoginTabText_${option.name}"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable(enabled = !isLoading) {
                                    mode = option
                                    validationError = null
                                }
                                .padding(vertical = 12.dp)
                                .testTag("email_login_tab_${option.name.lowercase(Locale.ROOT)}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = mode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "emailLoginModeContent"
                ) { currentMode ->
                    when (currentMode) {
                        EmailLoginMode.GOOGLE_EMAIL -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Same Google Account",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Choose the Google email already linked to your Hishab Khata shop. Your transactions, customers, and due records will restore immediately.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onContinueWithGoogleEmail()
                                    },
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("email_login_google_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Google Email", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        EmailLoginMode.EMAIL_AND_CODE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Enter the existing shop owner's email and the 6-digit link code shown in Account & Settings on their primary device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = shopEmail,
                                    onValueChange = {
                                        if (it.length <= 120) {
                                            shopEmail = it
                                            validationError = null
                                        }
                                    },
                                    label = { Text("Existing Shop Email") },
                                    placeholder = { Text("owner@example.com") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_link_email_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                OutlinedTextField(
                                    value = linkCode,
                                    onValueChange = { raw ->
                                        val digits = ShopLinkCrypto.normalizeSixDigitCode(raw).take(6)
                                        linkCode = digits
                                        validationError = null
                                    },
                                    label = { Text("6-Digit Link Code") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_link_code_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                AnimatedVisibility(visible = validationError != null) {
                                    Text(
                                        text = validationError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ledgerColors.expense
                                    )
                                }

                                Button(
                                    onClick = {
                                        val normalizedEmail = ShopLinkCrypto.normalizeEmail(shopEmail)
                                        val cleanCode = ShopLinkCrypto.normalizeSixDigitCode(linkCode)
                                        when {
                                            !ShopLinkCrypto.isValidEmail(normalizedEmail) ->
                                                validationError = "Please enter a valid existing shop email address."
                                            cleanCode.length != 6 ->
                                                validationError = "Please enter the full 6-digit link code."
                                            else -> {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onContinueWithEmailAndCode(normalizedEmail, cleanCode)
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("email_link_submit_btn")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Continuing...", fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign In & Link Shop", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Reusable dialog for an already signed-in device (Profile Setup or Account Settings)
 * to connect directly to an existing shop profile using the owner's email + 6-digit code.
 */
@Composable
fun ConnectExistingShopDialog(
    isLinking: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (email: String, code: String) -> Unit
) {
    Dialog(onDismissRequest = { if (!isLinking) onDismiss() }) {
        ConnectExistingShopDialogContent(
            isLinking = isLinking,
            errorMessage = errorMessage,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

@Composable
fun ConnectExistingShopDialogContent(
    isLinking: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (email: String, code: String) -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptics = LocalHapticFeedback.current
    var shopEmail by remember { mutableStateOf("") }
    var linkCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connect_existing_shop_dialog")
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connect Existing Shop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Link this device with Email + 6-Digit Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLinking,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_connect_shop_dialog_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = shopEmail,
                    onValueChange = {
                        if (it.length <= 120) {
                            shopEmail = it
                            localError = null
                        }
                    },
                    label = { Text("Existing Shop Email") },
                    placeholder = { Text("owner@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connect_shop_email_input")
                )

                OutlinedTextField(
                    value = linkCode,
                    onValueChange = { raw ->
                        linkCode = ShopLinkCrypto.normalizeSixDigitCode(raw).take(6)
                        localError = null
                    },
                    label = { Text("6-Digit Link Code") },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connect_shop_code_input")
                )

                val shownError = localError ?: errorMessage
                AnimatedVisibility(visible = !shownError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ledgerColors.expenseContainer,
                        border = BorderStroke(1.dp, ledgerColors.expense.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shownError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = ledgerColors.expense,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val normalizedEmail = ShopLinkCrypto.normalizeEmail(shopEmail)
                        val cleanCode = ShopLinkCrypto.normalizeSixDigitCode(linkCode)
                        when {
                            !ShopLinkCrypto.isValidEmail(normalizedEmail) ->
                                localError = "Please enter a valid shop email address."
                            cleanCode.length != 6 ->
                                localError = "Please enter the full 6-digit link code."
                            else -> {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSubmit(normalizedEmail, cleanCode)
                            }
                        }
                    },
                    enabled = !isLinking,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("connect_shop_submit_btn")
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect to Shop", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
}

/**
 * Multi-Device Access & Linked Devices section rendered inside Account & Settings.
 */
@Composable
fun MultiDeviceAccessSection(
    shopName: String,
    ownerEmail: String,
    isLinkedToOtherShop: Boolean,
    activeLinkCode: GeneratedLinkCode?,
    isGeneratingCode: Boolean,
    linkedDevices: List<LinkedDeviceRecord>,
    onGenerateOrRotateCode: () -> Unit,
    onUnlinkDevice: (LinkedDeviceRecord) -> Unit,
    onOpenConnectShopDialog: () -> Unit,
    onDisconnectFromLinkedShop: () -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val clipboardManager = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    var copiedFeedback by remember { mutableStateOf(false) }
    var devicePendingUnlink by remember { mutableStateOf<LinkedDeviceRecord?>(null) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    var nowMillis by remember(activeLinkCode) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeLinkCode) {
        if (activeLinkCode != null) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                if (nowMillis >= activeLinkCode.expiresAt) break
                delay(1000L)
            }
        }
    }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(2000L)
            copiedFeedback = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_device_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Multi-Device Access",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isLinkedToOtherShop) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    ledgerColors.incomeContainer
                }
            ) {
                Text(
                    text = if (isLinkedToOtherShop) "Linked Device" else "Primary Owner",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLinkedToOtherShop) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        ledgerColors.income
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (isLinkedToOtherShop) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Synced with $shopName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (ownerEmail.isNotBlank()) {
                        Text(
                            text = "Shop Email: $ownerEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "All income, expense, and due changes stay synced live with this shop's primary ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenConnectShopDialog,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_switch_shop_btn")
                        ) {
                            Text("Switch Shop", fontSize = 13.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showDisconnectConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ledgerColors.expense.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ledgerColors.expense),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("disconnect_linked_shop_btn")
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        } else {
            // Primary Owner: Rotating 6-Digit Link Code Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Link Another Device with Email + Code",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (ownerEmail.isNotBlank()) {
                            "Shop Email: $ownerEmail"
                        } else {
                            "Add an email in the Shop & Theme tab to generate a 6-digit link code."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val remainingMillis = activeLinkCode?.let { it.expiresAt - nowMillis } ?: 0L
                    val isCodeActive = activeLinkCode != null && remainingMillis > 0L

                    AnimatedContent(
                        targetState = isCodeActive to activeLinkCode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "linkCodeDisplay"
                    ) { (active, codeData) ->
                        if (active && codeData != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
                                                text = ShopLinkCrypto.formatSixDigitCode(codeData.code),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.testTag("active_link_code_value")
                                            )
                                            Text(
                                                text = "Expires in ${ShopLinkCrypto.formatRemainingTime(remainingMillis)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ledgerColors.income
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                clipboardManager.setText(
                                                    AnnotatedString(
                                                        "Hishab Khata Shop Link\nEmail: ${codeData.email}\n6-Digit Code: ${codeData.code}"
                                                    )
                                                )
                                                copiedFeedback = true
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("copy_link_code_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy email and 6-digit code",
                                                tint = if (copiedFeedback) ledgerColors.income else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onGenerateOrRotateCode()
                                    },
                                    enabled = !isGeneratingCode,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("rotate_link_code_btn")
                                ) {
                                    if (isGeneratingCode) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Rotating...")
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Rotate 6-Digit Code", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onGenerateOrRotateCode()
                                },
                                enabled = !isGeneratingCode && ShopLinkCrypto.isValidEmail(ownerEmail),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_link_code_btn")
                            ) {
                                if (isGeneratingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating...", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate 6-Digit Code", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Linked Devices List with Unlink Control
            Text(
                text = "Linked Devices (${linkedDevices.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (linkedDevices.isEmpty()) {
                Text(
                    text = "No secondary devices connected yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    linkedDevices.forEach { device ->
                        LinkedDeviceRow(
                            device = device,
                            onUnlinkClicked = { devicePendingUnlink = device }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenConnectShopDialog,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_join_shop_btn")
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log In to Another Shop by Email", fontWeight = FontWeight.Medium)
            }
        }
    }

    devicePendingUnlink?.let { target ->
        AlertDialog(
            onDismissRequest = { devicePendingUnlink = null },
            title = { Text("Unlink ${target.deviceName}?") },
            text = {
                Text("This device (${target.deviceEmail.ifBlank { "linked account" }}) will immediately lose access to your shop ledger.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnlinkDevice(target)
                        devicePendingUnlink = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ledgerColors.expense),
                    modifier = Modifier.testTag("confirm_unlink_device_btn")
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { devicePendingUnlink = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect from $shopName?") },
            text = {
                Text("This device will stop syncing with $shopName and return to your own account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDisconnectConfirm = false
                        onDisconnectFromLinkedShop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ledgerColors.expense),
                    modifier = Modifier.testTag("confirm_disconnect_shop_btn")
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LinkedDeviceRow(
    device: LinkedDeviceRecord,
    onUnlinkClicked: () -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val dateLabel = remember(device.linkedAt) {
        if (device.linkedAt > 0L) {
            SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(device.linkedAt))
        } else {
            "Recently linked"
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("linked_device_item_${device.deviceUid}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName.ifBlank { "Linked Device" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        device.deviceEmail.takeIf { it.isNotBlank() },
                        dateLabel
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onUnlinkClicked,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("unlink_device_btn_${device.deviceUid}")
            ) {
                Text(
                    text = "Unlink",
                    color = ledgerColors.expense,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
