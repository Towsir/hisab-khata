package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class LedgerColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val dueAmber: Color,
    val dueAmberContainer: Color
)

private val LightLedgerColors = LedgerColors(
    income = IncomeGreen,
    incomeContainer = IncomeGreenContainer,
    expense = ExpenseRed,
    expenseContainer = ExpenseRedContainer,
    dueAmber = DueAmber,
    dueAmberContainer = DueAmberContainer
)

private val DarkLedgerColors = LedgerColors(
    income = IncomeGreenDark,
    incomeContainer = IncomeGreenContainerDark,
    expense = ExpenseRedDark,
    expenseContainer = ExpenseRedContainerDark,
    dueAmber = DueAmberDark,
    dueAmberContainer = DueAmberContainerDark
)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

val MaterialTheme.ledgerColors: LedgerColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLedgerColors.current

private val DarkColorScheme = darkColorScheme(
    primary = SlatePrimaryDark,
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF8FAFC),
    secondary = SlateSecondaryDark,
    onSecondary = Color(0xFF090D16),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    error = ExpenseRedDark,
    background = NeutralBackgroundDark,
    surface = NeutralSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    outline = NeutralBorderDark,
    outlineVariant = Color(0xFF1E293B),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = SlatePrimary,
    onPrimary = Color.White,
    primaryContainer = NeutralSurfaceVariant,
    onPrimaryContainer = SlatePrimary,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8FAFC),
    onSecondaryContainer = SlateSecondary,
    error = ExpenseRed,
    background = NeutralBackground,
    surface = NeutralSurface,
    surfaceVariant = NeutralSurfaceVariant,
    outline = NeutralBorder,
    outlineVariant = NeutralBorderSubtle,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional neutral styling
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val ledgerColors = if (darkTheme) DarkLedgerColors else LightLedgerColors

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
