package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Reusable Frosted Liquid Glass card surface.
 * Uses translucent tinted fills, directional specular rim highlights, a crisp top-edge sheen,
 * and an optional slow diagonal light sweep (shimmerEnabled) for primary hero cards.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accentTint: Color? = null,
    shimmerEnabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(cornerRadius)

    val baseFill = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.83f else 0.90f)
    val topSheen = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.66f)
    val bottomTint = accentTint?.copy(alpha = if (isDark) 0.10f else 0.05f) ?: Color.Transparent

    val shimmerProgress = if (shimmerEnabled) {
        val transition = rememberInfiniteTransition(label = "glassHeroSheen")
        transition.animateFloat(
            initialValue = -0.6f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glassSheenX"
        ).value
    } else {
        -1f
    }

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.34f),
                accentTint?.copy(alpha = 0.30f) ?: Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.98f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.52f),
                accentTint?.copy(alpha = 0.28f) ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
            )
        }
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.984f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassCardPressScale"
    )

    val cardModifier = if (onClick != null) {
        modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
    } else {
        modifier.clip(shape)
    }

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = baseFill,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 2.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.drawBehind {
                // Vertical frosted glass depth wash
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topSheen,
                            Color.Transparent,
                            bottomTint
                        )
                    )
                )

                // Crisp top-edge specular glass glint
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark) 0.36f else 0.90f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.12f, 1.dp.toPx()),
                    end = Offset(size.width * 0.88f, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )

                // Subtle slow diagonal light reflection on hero cards
                if (shimmerEnabled && shimmerProgress in -0.5f..1.5f) {
                    val centerX = size.width * shimmerProgress
                    val band = size.width * 0.34f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDark) 0.075f else 0.22f),
                                Color.Transparent
                            ),
                            start = Offset(centerX - band, 0f),
                            end = Offset(centerX + band, size.height)
                        )
                    )
                }
            }
        ) {
            content()
        }
    }
}

/**
 * Animated Rolling BDT Balance Counter.
 * Slides changing characters vertically (upward when value increases, downward when decreasing)
 * like a refined fintech balance ticker.
 */
@Composable
fun RollingBdtCounter(
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "",
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    var previousAmount by remember { mutableDoubleStateOf(amount) }
    val isIncreasing = amount >= previousAmount
    SideEffect {
        previousAmount = amount
    }

    val formatted = remember(amount, prefix) { prefix + CurrencyUtils.formatBdt(amount) }

    Row(
        modifier = modifier.clearAndSetSemantics {
            text = AnnotatedString(formatted)
            contentDescription = formatted
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        formatted.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    val direction = if (isIncreasing) 1 else -1
                    (slideInVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullHeight -> direction * fullHeight }
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            targetOffsetY = { fullHeight -> -direction * fullHeight }
                        ) + fadeOut(animationSpec = tween(180)))
                },
                label = "rollingDigit_$index"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    fontWeight = fontWeight,
                    color = color,
                    letterSpacing = letterSpacing,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Dual-layer smooth flowing sine wave motion graphic for hero cards.
 */
@Composable
fun AmbientWaveGraphic(
    accentColor: Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "ambientWave")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        fun buildWavePath(phase: Float, amplitude: Float, frequency: Float, baselineRatio: Float): Path {
            val path = Path()
            val baseline = h * baselineRatio
            val steps = 36
            val stepWidth = w / steps
            path.moveTo(0f, h)
            path.lineTo(0f, baseline + sin(phase) * amplitude)
            for (i in 1..steps) {
                val x = i * stepWidth
                val angle = (i.toFloat() / steps) * (2.0 * PI).toFloat() * frequency + phase
                val y = baseline + sin(angle) * amplitude
                path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.close()
            return path
        }

        val backWave = buildWavePath(
            phase = wavePhase2,
            amplitude = h * 0.22f,
            frequency = 1.4f,
            baselineRatio = 0.45f
        )
        val frontWave = buildWavePath(
            phase = wavePhase1,
            amplitude = h * 0.18f,
            frequency = 1.9f,
            baselineRatio = 0.58f
        )

        val alpha1 = if (isDark) 0.18f else 0.13f
        val alpha2 = if (isDark) 0.24f else 0.17f

        drawPath(
            path = backWave,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = alpha1), Color.Transparent)
            )
        )
        drawPath(
            path = frontWave,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = alpha2), Color.Transparent)
            )
        )

        // Crisp luminous wave crest hairline
        val crestPath = Path()
        val steps = 36
        val stepWidth = w / steps
        val baseline = h * 0.58f
        val amplitude = h * 0.18f
        crestPath.moveTo(0f, baseline + sin(wavePhase1) * amplitude)
        for (i in 1..steps) {
            val x = i * stepWidth
            val angle = (i.toFloat() / steps) * (2.0 * PI).toFloat() * 1.9f + wavePhase1
            crestPath.lineTo(x, baseline + sin(angle) * amplitude)
        }
        drawPath(
            path = crestPath,
            color = accentColor.copy(alpha = if (isDark) 0.36f else 0.24f),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

/**
 * Subtle breathing concentric rings for the welcome brand medallion.
 */
@Composable
fun PulsingEmblemHalo(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emblemHalo")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emblemPulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.34f
            val maxExtra = size.minDimension * 0.16f

            val r1 = baseRadius + maxExtra * pulse
            val a1 = (1f - pulse) * 0.35f
            drawCircle(
                color = accentColor.copy(alpha = a1),
                radius = r1,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            val pulse2 = (pulse + 0.5f) % 1f
            val r2 = baseRadius + maxExtra * pulse2
            val a2 = (1f - pulse2) * 0.25f
            drawCircle(
                color = accentColor.copy(alpha = a2),
                radius = r2,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        content()
    }
}
