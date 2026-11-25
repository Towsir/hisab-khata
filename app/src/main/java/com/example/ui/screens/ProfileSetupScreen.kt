package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.auth.formatBangladeshiPhoneDisplay
import com.example.auth.normalizeBangladeshiPhone
import com.example.data.FirestoreBusinessRepository
import com.example.data.ThemeMode
import com.example.data.model.GeneratedLinkCode
import com.example.data.model.LinkedDeviceRecord
import com.example.data.model.UserProfileRecord
import com.example.ui.components.ConnectExistingShopDialog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.MultiDeviceAccessSection
import com.example.ui.theme.ledgerColors

@Composable
fun ProfileSetupScreen(
    initialName: String,
    initialPhone: String = "",
    initialEmail: String,
    isSaving: Boolean,
    errorMessage: String?,
    onSaveProfile: (displayName: String, phoneNumber: String, email: String) -> Unit,
    onSignOut: () -> Unit,
    isLinkingShop: Boolean = false,
    linkShopError: String? = null,
    onClearLinkShopError: () -> Unit = {},
    onLinkExistingShop: (email: String, code: String, onSuccess: () -> Unit) -> Unit = { _, _, _ -> }
) {
    val ledgerColors = MaterialTheme.ledgerColors
    var displayName by remember(initialName) { mutableStateOf(initialName) }
    var phoneNumber by remember(initialPhone) {
        mutableStateOf(
            if (initialPhone.isNotBlank()) formatBangladeshiPhoneDisplay(initialPhone) else ""
        )
    }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showConnectShopDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .testTag("profile_setup_screen"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassCard(
            cornerRadius = 26.dp,
            accentTint = ledgerColors.income,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shop Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("profile_setup_sign_out_btn")
                    ) {
                        Text("Switch Account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Link your shop name and Bangladeshi mobile (+880) to your cloud ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_NAME_LENGTH) {
                            displayName = it
                            validationError = null
                        }
                    },
                    label = { Text("Shop or Owner Name") },
                    placeholder = { Text("e.g. Bismillah Enterprise") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_PHONE_LENGTH) {
                            phoneNumber = it
                            validationError = null
                        }
                    },
                    label = { Text("Bangladeshi Mobile (+880)") },
                    placeholder = { Text("01712-345678") },
                    supportingText = { Text("Accepts 017..., 17..., or +88017...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        if (it.length <= FirestoreBusinessRepository.MAX_EMAIL_LENGTH) {
                            email = it
                            validationError = null
                        }
                    },
                    label = { Text("Email Address (optional)") },
                    placeholder = { Text("e.g. owner@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                val displayError = validationError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = ledgerColors.expense
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val trimmedName = displayName.trim()
                        val trimmedPhone = phoneNumber.trim()
                        val trimmedEmail = email.trim()
                        val normalizedPhone = normalizeBangladeshiPhone(trimmedPhone)
                        when {
                            trimmedName.isEmpty() -> validationError = "Please enter your shop or owner name"
                            trimmedPhone.isEmpty() -> validationError = "Please enter your Bangladeshi mobile number"
                            normalizedPhone == null -> validationError = "Enter a valid Bangladeshi mobile (e.g. 01712345678)"
                            trimmedEmail.isNotEmpty() && !trimmedEmail.contains("@") -> validationError = "Please enter a valid email address"
                            else -> onSaveProfile(trimmedName, normalizedPhone, trimmedEmail)
                        }
                    },
                    enabled = !isSaving && !isLinkingShop,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Saving...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    } else {
                        Text("Continue", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Already have a shop on another device?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect directly to your existing shop ledger with the owner's email and 6-digit link code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                onClearLinkShopError()
                                showConnectShopDialog = true
                            },
                            enabled = !isSaving && !isLinkingShop,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_setup_link_email_btn")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log In with Email & 6-Digit Code", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showConnectShopDialog) {
        ConnectExistingShopDialog(
            isLinking = isLinkingShop,
            errorMessage = linkShopError,
            onDismiss = {
                showConnectShopDialog = false
                onClearLinkShopError()
            },
            onSubmit = { shopEmail, code ->
                onLinkExistingShop(shopEmail, code) {
                    showConnectShopDialog = false
                }
            }
        )
    }
}

enum class AccountSettingsTab {
    SHOP_AND_THEME,
    DEVICES_AND_SYNC
}

@Composable
fun AccountProfileDialog(
    profile: UserProfileRecord?,
    fallbackEmail: String,
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    onSaveProfile: (displayName: String, phoneNumber: String, email: String) -> Unit,
    onSignOut: () -> Unit,
    isLinkedToOtherShop: Boolean = false,
    activeLinkCode: GeneratedLinkCode? = null,
    isGeneratingLinkCode: Boolean = false,
    isLinkingShop: Boolean = false,
    linkShopError: String? = null,
    linkedDevices: List<LinkedDeviceRecord> = emptyList(),
    onGenerateOrRotateLinkCode: (shopName: String, ownerEmail: String) -> Unit = { _, _ -> },
    onLinkExistingShop: (email: String, code: String, onSuccess: () -> Unit) -> Unit = { _, _, _ -> },
    onClearLinkShopError: () -> Unit = {},
    onUnlinkDevice: (LinkedDeviceRecord) -> Unit = {},
    onDisconnectFromLinkedShop: () -> Unit = {}
) {
    var showConnectShopDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        AccountProfileDialogContent(
            profile = profile,
            fallbackEmail = fallbackEmail,
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = onThemeModeSelected,
            onDismiss = onDismiss,
            onSaveProfile = onSaveProfile,
            onSignOut = onSignOut,
            isLinkedToOtherShop = isLinkedToOtherShop,
            activeLinkCode = activeLinkCode,
            isGeneratingLinkCode = isGeneratingLinkCode,
            linkedDevices = linkedDevices,
            onGenerateOrRotateLinkCode = onGenerateOrRotateLinkCode,
            onOpenConnectShopDialog = {
                onClearLinkShopError()
                showConnectShopDialog = true
            },
            onUnlinkDevice = onUnlinkDevice,
            onDisconnectFromLinkedShop = onDisconnectFromLinkedShop
        )
    }

    if (showConnectShopDialog) {
        ConnectExistingShopDialog(
            isLinking = isLinkingShop,
            errorMessage = linkShopError,
            onDismiss = {
                showConnectShopDialog = false
                onClearLinkShopError()
            },
            onSubmit = { shopEmail, code ->
                onLinkExistingShop(shopEmail, code) {
                    showConnectShopDialog = false
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun AccountProfileDialogContent(
    profile: UserProfileRecord?,
    fallbackEmail: String,
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    onSaveProfile: (displayName: String, phoneNumber: String, email: String) -> Unit,
    onSignOut: () -> Unit,
    isLinkedToOtherShop: Boolean = false,
    activeLinkCode: GeneratedLinkCode? = null,
    isGeneratingLinkCode: Boolean = false,
    linkedDevices: List<LinkedDeviceRecord> = emptyList(),
    onGenerateOrRotateLinkCode: (shopName: String, ownerEmail: String) -> Unit = { _, _ -> },
    onOpenConnectShopDialog: () -> Unit = {},
    onUnlinkDevice: (LinkedDeviceRecord) -> Unit = {},
    onDisconnectFromLinkedShop: () -> Unit = {},
    initialTab: AccountSettingsTab = AccountSettingsTab.SHOP_AND_THEME
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var displayName by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
    var phoneNumber by remember(profile) {
        val raw = profile?.phoneNumber.orEmpty()
        mutableStateOf(if (raw.isNotBlank()) formatBangladeshiPhoneDisplay(raw) else "")
    }
    var email by remember(profile) { mutableStateOf(profile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_profile_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Account & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLinkedToOtherShop) "Linked shop sync active" else "Cloud sync active",
                        style = MaterialTheme.typography.bodySmall,
                        color = ledgerColors.income,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_account_dialog_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean Two-Tab Switcher (Shop & Theme | Devices & Sync)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple(AccountSettingsTab.SHOP_AND_THEME, "Shop & Theme", Icons.Outlined.Storefront),
                    Triple(AccountSettingsTab.DEVICES_AND_SYNC, "Devices & Sync", Icons.Default.Devices)
                )
                tabs.forEach { (tab, label, icon) ->
                    val isSelected = selectedTab == tab
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        animationSpec = spring(),
                        label = "settingsTabBg_${tab.name}"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(),
                        label = "settingsTabContent_${tab.name}"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = tab
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag(
                                if (tab == AccountSettingsTab.SHOP_AND_THEME) "settings_tab_shop" else "settings_tab_devices"
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "accountSettingsTabContent"
            ) { tab ->
                when (tab) {
                    AccountSettingsTab.SHOP_AND_THEME -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Appearance / Dark Mode Toggle
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modes = listOf(
                                    Triple(ThemeMode.SYSTEM, Icons.Default.BrightnessAuto, "theme_mode_system"),
                                    Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "theme_mode_light"),
                                    Triple(ThemeMode.DARK, Icons.Default.DarkMode, "theme_mode_dark")
                                )
                                modes.forEach { (mode, icon, tag) ->
                                    val selected = currentThemeMode == mode
                                    val bg by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        animationSpec = spring(),
                                        label = "themeModeBg_${mode.name}"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        animationSpec = spring(),
                                        label = "themeModeContent_${mode.name}"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bg)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onThemeModeSelected(mode)
                                            }
                                            .padding(vertical = 10.dp)
                                            .testTag(tag),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = mode.label,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = mode.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = contentColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isLinkedToOtherShop) "Connected Shop Profile (Read-Only)" else "Shop Profile",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = {
                                    if (it.length <= FirestoreBusinessRepository.MAX_NAME_LENGTH) {
                                        displayName = it
                                        validationError = null
                                    }
                                },
                                enabled = !isLinkedToOtherShop,
                                label = { Text("Shop / Owner Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_name_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    if (it.length <= FirestoreBusinessRepository.MAX_PHONE_LENGTH) {
                                        phoneNumber = it
                                        validationError = null
                                    }
                                },
                                enabled = !isLinkedToOtherShop,
                                label = { Text("Bangladeshi Mobile (+880)") },
                                placeholder = { Text("01712-345678") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_phone_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    if (it.length <= FirestoreBusinessRepository.MAX_EMAIL_LENGTH) {
                                        email = it
                                        validationError = null
                                    }
                                },
                                enabled = !isLinkedToOtherShop,
                                label = { Text("Email Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_email_input")
                            )

                            if (validationError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ledgerColors.expense
                                )
                            }

                            if (!isLinkedToOtherShop) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val trimmedName = displayName.trim()
                                        val trimmedPhone = phoneNumber.trim()
                                        val trimmedEmail = email.trim()
                                        val normalizedPhone = if (trimmedPhone.isEmpty()) "" else normalizeBangladeshiPhone(trimmedPhone)
                                        when {
                                            trimmedName.isEmpty() -> validationError = "Shop name cannot be empty"
                                            trimmedPhone.isNotEmpty() && normalizedPhone == null ->
                                                validationError = "Enter a valid Bangladeshi mobile (e.g. 01712345678)"
                                            trimmedEmail.isNotEmpty() && !trimmedEmail.contains("@") ->
                                                validationError = "Please enter a valid email address"
                                            else -> {
                                                onSaveProfile(trimmedName, normalizedPhone.orEmpty(), trimmedEmail)
                                                onDismiss()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_account_changes_button")
                                ) {
                                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSignOut()
                                },
                                border = BorderStroke(1.dp, ledgerColors.expense.copy(alpha = 0.35f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ledgerColors.expenseContainer.copy(alpha = 0.55f),
                                    contentColor = ledgerColors.expense
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    AccountSettingsTab.DEVICES_AND_SYNC -> {
                        MultiDeviceAccessSection(
                            shopName = displayName.ifBlank { profile?.displayName.orEmpty().ifBlank { "Hishab Khata" } },
                            ownerEmail = email.ifBlank { fallbackEmail },
                            isLinkedToOtherShop = isLinkedToOtherShop,
                            activeLinkCode = activeLinkCode,
                            isGeneratingCode = isGeneratingLinkCode,
                            linkedDevices = linkedDevices,
                            onGenerateOrRotateCode = {
                                onGenerateOrRotateLinkCode(
                                    displayName.trim().ifBlank { profile?.displayName.orEmpty().ifBlank { "Hishab Khata" } },
                                    email.trim().ifBlank { fallbackEmail }
                                )
                            },
                            onUnlinkDevice = onUnlinkDevice,
                            onOpenConnectShopDialog = onOpenConnectShopDialog,
                            onDisconnectFromLinkedShop = {
                                onDisconnectFromLinkedShop()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}
