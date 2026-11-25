package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PhoneAuthDialog
import com.example.ui.components.PhoneAuthMode
import com.example.ui.components.PulsingEmblemHalo
import com.example.ui.theme.ledgerColors

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onPhoneNumberCaptured: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var activePhoneAuthMode by remember { mutableStateOf<PhoneAuthMode?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSession = false
            },
            scope = coroutineScope
        )
    }

    val startGoogleSignIn: () -> Unit = {
        errorMessage = null
        infoMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = coroutineScope,
            onAuthCancelled = {
                isLoading = false
                infoMessage = "Sign-in cancelled. Tap below to try again."
            }
        )
    }

    AuthScreenContent(
        isLoading = isLoading,
        isCheckingSession = isCheckingSession,
        infoMessage = infoMessage,
        errorMessage = errorMessage,
        onPhoneLoginClicked = {
            errorMessage = null
            infoMessage = null
            activePhoneAuthMode = PhoneAuthMode.LOGIN
        },
        onGoogleLoginClicked = {
            onPhoneNumberCaptured("")
            startGoogleSignIn()
        },
        onCreateAccountWithPhoneClicked = {
            errorMessage = null
            infoMessage = null
            activePhoneAuthMode = PhoneAuthMode.CREATE_ACCOUNT
        }
    )

    activePhoneAuthMode?.let { mode ->
        PhoneAuthDialog(
            mode = mode,
            isBusy = isLoading,
            onDismiss = { activePhoneAuthMode = null },
            onPhoneConfirmed = { normalizedE164 ->
                activePhoneAuthMode = null
                onPhoneNumberCaptured(normalizedE164)
                startGoogleSignIn()
            }
        )
    }
}

@Composable
fun AuthScreenContent(
    isLoading: Boolean,
    isCheckingSession: Boolean,
    infoMessage: String?,
    errorMessage: String?,
    onPhoneLoginClicked: () -> Unit,
    onGoogleLoginClicked: () -> Unit,
    onCreateAccountWithPhoneClicked: () -> Unit
) {
    val ledgerColors = MaterialTheme.ledgerColors
    val haptic = LocalHapticFeedback.current
    val isBusy = isLoading || isCheckingSession

    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        cardVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        ) {
            LiquidGlassCard(
                cornerRadius = 28.dp,
                accentTint = ledgerColors.income,
                shimmerEnabled = true,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .testTag("auth_glass_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Clean Brand Emblem + Wordmark Only
                    PulsingEmblemHalo(
                        accentColor = ledgerColors.income,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = "Hishab Khata",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Hishab Khata",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    // Primary Action: Log In with Phone Number
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPhoneLoginClicked()
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("phone_login_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCheckingSession) "Checking session..." else "Signing in...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Log In with Phone Number",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Action: Log In with Google
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGoogleLoginClicked()
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log In with Google",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtle Hairline Separator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tertiary Action: Create Account with Phone Number
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCreateAccountWithPhoneClicked()
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_account_phone_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Account with Phone Number",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // On-Demand Status & Error Feedback (hidden when idle)
                    AnimatedVisibility(visible = infoMessage != null && errorMessage == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("auth_info_banner")
                        ) {
                            Text(
                                text = infoMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ledgerColors.expenseContainer,
                            border = BorderStroke(1.dp, ledgerColors.expense.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = ledgerColors.expense,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
