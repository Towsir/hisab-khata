package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AuroraMood {
    NEUTRAL,
    INCOME,
    EXPENSE,
    DUE,
    REPORTS
}

private data class AuroraPalette(
    val primaryOrb: Color,
    val secondaryOrb: Color,
    val tertiaryOrb: Color,
    val ribbonAccent: Color,
    val particleColor: Color,
    val touchGlow: Color
)

private data class GlowParticle(
    val baseX: Float,
    val baseY: Float,
    val radiusFraction: Float,
    val speedMultiplier: Float,
    val swayAmplitude: Float,
    val phaseOffset: Float,
    val isSecondaryTint: Boolean
)

private val AMBIENT_PARTICLES = listOf(
    GlowParticle(0.10f, 0.14f, 0.011f, 1.0f, 0.032f, 0.2f, false),
    GlowParticle(0.24f, 0.68f, 0.016f, 0.8f, 0.042f, 1.4f, true),
    GlowParticle(0.36f, 0.34f, 0.009f, 1.3f, 0.025f, 2.5f, false),
    GlowParticle(0.49f, 0.82f, 0.014f, 0.9f, 0.038f, 3.1f, true),
    GlowParticle(0.62f, 0.22f, 0.012f, 1.1f, 0.030f, 4.0f, false),
    GlowParticle(0.75f, 0.56f, 0.018f, 0.7f, 0.044f, 5.2f, true),
    GlowParticle(0.88f, 0.18f, 0.010f, 1.2f, 0.028f, 0.9f, false),
    GlowParticle(0.16f, 0.44f, 0.008f, 1.4f, 0.022f, 2.1f, true),
    GlowParticle(0.82f, 0.88f, 0.013f, 0.85f, 0.035f, 3.8f, false),
    GlowParticle(0.42f, 0.12f, 0.015f, 0.75f, 0.036f, 4.7f, true),
    GlowParticle(0.68f, 0.74f, 0.009f, 1.25f, 0.026f, 1.7f, false),
    GlowParticle(0.29f, 0.92f, 0.012f, 0.95f, 0.033f, 5.6f, false),
    GlowParticle(0.55f, 0.48f, 0.008f, 1.35f, 0.024f, 0.5f, true),
    GlowParticle(0.92f, 0.62f, 0.011f, 1.05f, 0.029f, 2.9f, false),
    GlowParticle(0.06f, 0.78f, 0.014f, 0.82f, 0.031f, 4.3f, true),
    GlowParticle(0.51f, 0.28f, 0.010f, 1.15f, 0.027f, 5.9f, false)
)

private fun paletteForMood(mood: AuroraMood, isDark: Boolean): AuroraPalette {
    return when (mood) {
        AuroraMood.INCOME -> if (isDark) {
            AuroraPalette(
                primaryOrb = Color(0xFF10B981),
                secondaryOrb = Color(0xFF06B6D4),
                tertiaryOrb = Color(0xFF34D399),
                ribbonAccent = Color(0xFF2DD4BF),
                particleColor = Color(0xFFA7F3D0),
                touchGlow = Color(0xFF6EE7B7)
            )
        } else {
            AuroraPalette(
                primaryOrb = Color(0xFF34D399),
                secondaryOrb = Color(0xFF38BDF8),
                tertiaryOrb = Color(0xFF6EE7B7),
                ribbonAccent = Color(0xFF14B8A6),
                particleColor = Color(0xFF059669),
                touchGlow = Color(0xFF10B981)
            )
        }

        AuroraMood.EXPENSE -> if (isDark) {
            AuroraPalette(
                primaryOrb = Color(0xFFF43F5E),
                secondaryOrb = Color(0xFFF59E0B),
                tertiaryOrb = Color(0xFFFB7185),
                ribbonAccent = Color(0xFFEC4899),
                particleColor = Color(0xFFFECDD3),
                touchGlow = Color(0xFFFDA4AF)
            )
        } else {
            AuroraPalette(
                primaryOrb = Color(0xFFFB7185),
                secondaryOrb = Color(0xFFFBBF24),
                tertiaryOrb = Color(0xFFFDA4AF),
                ribbonAccent = Color(0xFFF43F5E),
                particleColor = Color(0xFFE11D48),
                touchGlow = Color(0xFFF43F5E)
            )
        }

        AuroraMood.DUE -> if (isDark) {
            AuroraPalette(
                primaryOrb = Color(0xFFF59E0B),
                secondaryOrb = Color(0xFF6366F1),
                tertiaryOrb = Color(0xFF10B981),
                ribbonAccent = Color(0xFFFBBF24),
                particleColor = Color(0xFFFDE68A),
                touchGlow = Color(0xFFFCD34D)
            )
        } else {
            AuroraPalette(
                primaryOrb = Color(0xFFFBBF24),
                secondaryOrb = Color(0xFF818CF8),
                tertiaryOrb = Color(0xFF6EE7B7),
                ribbonAccent = Color(0xFFF59E0B),
                particleColor = Color(0xFFD97706),
                touchGlow = Color(0xFFF59E0B)
            )
        }

        AuroraMood.REPORTS -> if (isDark) {
            AuroraPalette(
                primaryOrb = Color(0xFF3B82F6),
                secondaryOrb = Color(0xFF06B6D4),
                tertiaryOrb = Color(0xFF8B5CF6),
                ribbonAccent = Color(0xFF60A5FA),
                particleColor = Color(0xFFBFDBFE),
                touchGlow = Color(0xFF60A5FA)
            )
        } else {
            AuroraPalette(
                primaryOrb = Color(0xFF60A5FA),
                secondaryOrb = Color(0xFF22D3EE),
                tertiaryOrb = Color(0xFFA78BFA),
                ribbonAccent = Color(0xFF3B82F6),
                particleColor = Color(0xFF2563EB),
                touchGlow = Color(0xFF3B82F6)
            )
        }

        AuroraMood.NEUTRAL -> if (isDark) {
            AuroraPalette(
                primaryOrb = Color(0xFF10B981),
                secondaryOrb = Color(0xFF3B82F6),
                tertiaryOrb = Color(0xFF06B6D4),
                ribbonAccent = Color(0xFF2DD4BF),
                particleColor = Color(0xFFA7F3D0),
                touchGlow = Color(0xFF34D399)
            )
        } else {
            AuroraPalette(
                primaryOrb = Color(0xFF34D399),
                secondaryOrb = Color(0xFF60A5FA),
                tertiaryOrb = Color(0xFF67E8F9),
                ribbonAccent = Color(0xFF0EA5E9),
                particleColor = Color(0xFF059669),
                touchGlow = Color(0xFF059669)
            )
        }
    }
}

/**
 * Full-bleed luxury living aurora canvas:
 * 1. Atmospheric gradient base for obsidian dark / pearl-ice light depth
 * 2. Deep flowing silk aurora ribbons drifting across the viewport
 * 3. Soft tab-reactive drifting radial mesh orbs
 * 4. 16 floating luminous bokeh glow particles with smooth parallax breathing
 * 5. Touch-reactive luminous halo + twin staggered water-glass tap ripples
 */
@Composable
fun InteractiveAuroraBackground(
    mood: AuroraMood,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val targetPalette = remember(mood, isDarkTheme) { paletteForMood(mood, isDarkTheme) }

    val orb1Color by animateColorAsState(
        targetValue = targetPalette.primaryOrb,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "auroraOrb1"
    )
    val orb2Color by animateColorAsState(
        targetValue = targetPalette.secondaryOrb,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "auroraOrb2"
    )
    val orb3Color by animateColorAsState(
        targetValue = targetPalette.tertiaryOrb,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "auroraOrb3"
    )
    val ribbonColor by animateColorAsState(
        targetValue = targetPalette.ribbonAccent,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "auroraRibbon"
    )
    val particleColor by animateColorAsState(
        targetValue = targetPalette.particleColor,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "auroraParticles"
    )
    val touchGlowColor by animateColorAsState(
        targetValue = targetPalette.touchGlow,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "auroraTouchGlow"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "auroraFlow")
    val phaseA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhaseA"
    )
    val phaseB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhaseB"
    )
    val phaseC by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhaseC"
    )
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraParticleDrift"
    )

    val scope = rememberCoroutineScope()
    var hasTouch by remember { mutableStateOf(false) }
    val smoothTouchOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var rippleCenter by remember { mutableStateOf<Offset?>(null) }
    val rippleProgress = remember { Animatable(1f) }

    val baseBackground = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeChange = event.changes.firstOrNull()
                        if (activeChange != null && activeChange.pressed) {
                            val pos = activeChange.position
                            val wasPressed = activeChange.previousPressed
                            val isFirstTouch = !hasTouch
                            hasTouch = true
                            scope.launch {
                                if (isFirstTouch) {
                                    smoothTouchOffset.snapTo(pos)
                                } else {
                                    smoothTouchOffset.animateTo(
                                        targetValue = pos,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = 110f
                                        )
                                    )
                                }
                            }
                            if (!wasPressed) {
                                rippleCenter = pos
                                scope.launch {
                                    rippleProgress.snapTo(0f)
                                    rippleProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 1050,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // Layer 1: Subtle Atmospheric Vertical Depth Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            orb1Color.copy(alpha = 0.08f),
                            Color.Transparent,
                            orb2Color.copy(alpha = 0.07f)
                        )
                    } else {
                        listOf(
                            orb1Color.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.35f),
                            orb2Color.copy(alpha = 0.05f)
                        )
                    }
                )
            )

            // Layer 2: Soft Drifting Radial Mesh Orbs
            val primaryAlpha = if (isDarkTheme) 0.27f else 0.23f
            val secondaryAlpha = if (isDarkTheme) 0.22f else 0.19f
            val tertiaryAlpha = if (isDarkTheme) 0.20f else 0.20f

            val c1 = Offset(
                x = w * (0.26f + 0.26f * cos(phaseA)),
                y = h * (0.16f + 0.11f * sin(phaseA * 1.2f))
            )
            val r1 = w * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orb1Color.copy(alpha = primaryAlpha),
                        orb1Color.copy(alpha = primaryAlpha * 0.36f),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1
            )

            val c2 = Offset(
                x = w * (0.78f + 0.22f * sin(phaseB)),
                y = h * (0.46f + 0.15f * cos(phaseB * 0.9f))
            )
            val r2 = w * 0.92f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orb2Color.copy(alpha = secondaryAlpha),
                        orb2Color.copy(alpha = secondaryAlpha * 0.32f),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2
            )

            val c3 = Offset(
                x = w * (0.46f + 0.28f * cos(phaseC + 1.1f)),
                y = h * (0.85f + 0.09f * sin(phaseC))
            )
            val r3 = w * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orb3Color.copy(alpha = tertiaryAlpha),
                        orb3Color.copy(alpha = tertiaryAlpha * 0.34f),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = r3
                ),
                radius = r3,
                center = c3
            )

            // Layer 3: Deep Flowing Translucent Silk Aurora Ribbons (Upper + Lower)
            fun drawSilkRibbon(
                baseYRatio: Float,
                bandHeightRatio: Float,
                phase: Float,
                freq: Float,
                tintA: Color,
                tintB: Color,
                fillAlpha: Float,
                crestAlpha: Float
            ) {
                val steps = 28
                val stepX = w / steps
                val centerY = h * baseYRatio
                val bandH = h * bandHeightRatio
                val amp = h * 0.045f

                val ribbonPath = Path()
                val crestPath = Path()

                for (i in 0..steps) {
                    val x = i * stepX
                    val t = i.toFloat() / steps
                    val waveTop = sin(t * (2.0 * PI).toFloat() * freq + phase) * amp +
                        cos(t * (2.0 * PI).toFloat() * (freq * 0.6f) - phase * 0.7f) * (amp * 0.45f)
                    val topY = centerY - bandH * 0.5f + waveTop
                    if (i == 0) {
                        ribbonPath.moveTo(x, topY)
                        crestPath.moveTo(x, topY)
                    } else {
                        ribbonPath.lineTo(x, topY)
                        crestPath.lineTo(x, topY)
                    }
                }
                for (i in steps downTo 0) {
                    val x = i * stepX
                    val t = i.toFloat() / steps
                    val waveBottom = sin(t * (2.0 * PI).toFloat() * freq + phase + 0.9f) * (amp * 1.15f)
                    val bottomY = centerY + bandH * 0.5f + waveBottom
                    ribbonPath.lineTo(x, bottomY)
                }
                ribbonPath.close()

                drawPath(
                    path = ribbonPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            tintA.copy(alpha = fillAlpha),
                            tintB.copy(alpha = fillAlpha * 0.85f),
                            Color.Transparent
                        ),
                        start = Offset(0f, centerY - bandH),
                        end = Offset(w, centerY + bandH)
                    )
                )

                // Soft luminous silk edge hairline
                drawPath(
                    path = crestPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            tintA.copy(alpha = crestAlpha),
                            tintB.copy(alpha = crestAlpha * 0.8f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 1.6f, cap = StrokeCap.Round)
                )
            }

            drawSilkRibbon(
                baseYRatio = 0.27f,
                bandHeightRatio = 0.16f,
                phase = phaseA,
                freq = 1.15f,
                tintA = orb1Color,
                tintB = ribbonColor,
                fillAlpha = if (isDarkTheme) 0.14f else 0.11f,
                crestAlpha = if (isDarkTheme) 0.34f else 0.24f
            )

            drawSilkRibbon(
                baseYRatio = 0.68f,
                bandHeightRatio = 0.18f,
                phase = -phaseB + 1.2f,
                freq = 1.35f,
                tintA = orb2Color,
                tintB = orb3Color,
                fillAlpha = if (isDarkTheme) 0.12f else 0.10f,
                crestAlpha = if (isDarkTheme) 0.28f else 0.20f
            )

            // Layer 4: Floating Luminous Bokeh Glow Particles
            val baseParticleAlpha = if (isDarkTheme) 0.42f else 0.26f
            AMBIENT_PARTICLES.forEach { p ->
                val rawY = (p.baseY - particleDrift * p.speedMultiplier) % 1f
                val wrappedY = if (rawY < 0f) rawY + 1f else rawY
                val swayX = p.baseX + sin(phaseC * p.speedMultiplier + p.phaseOffset) * p.swayAmplitude
                val px = (swayX.coerceIn(0.02f, 0.98f)) * w
                val py = wrappedY * h

                // Fade gently near top & bottom screen edges
                val edgeFade = (sin(wrappedY * PI).toFloat()).coerceIn(0f, 1f)
                val breath = 0.55f + 0.45f * sin(phaseA * 1.6f + p.phaseOffset)
                val alpha = baseParticleAlpha * edgeFade * breath
                if (alpha > 0.01f) {
                    val pColor = if (p.isSecondaryTint) ribbonColor else particleColor
                    val glowRadius = w * p.radiusFraction * 3.1f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pColor.copy(alpha = alpha),
                                pColor.copy(alpha = alpha * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(px, py),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(px, py)
                    )
                    // Crisp luminous inner starlight core
                    drawCircle(
                        color = if (isDarkTheme) Color.White.copy(alpha = alpha * 0.85f) else pColor.copy(alpha = alpha * 0.75f),
                        radius = (w * p.radiusFraction * 0.36f).coerceAtLeast(1.4f),
                        center = Offset(px, py)
                    )
                }
            }

            // Layer 5: Interactive Touch Glow Halo + Twin Staggered Water-Glass Ripples
            if (hasTouch) {
                val touchPos = smoothTouchOffset.value
                val touchRadius = w * 0.58f
                val touchAlpha = if (isDarkTheme) 0.22f else 0.16f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            touchGlowColor.copy(alpha = touchAlpha),
                            touchGlowColor.copy(alpha = touchAlpha * 0.26f),
                            Color.Transparent
                        ),
                        center = touchPos,
                        radius = touchRadius
                    ),
                    radius = touchRadius,
                    center = touchPos
                )
            }

            val rCenter = rippleCenter
            val progress = rippleProgress.value
            if (rCenter != null && progress < 1f) {
                val maxRingRadius = w * 0.44f
                val fade1 = (1f - progress) * (1f - progress)
                val ringAlpha1 = (if (isDarkTheme) 0.34f else 0.24f) * fade1
                drawCircle(
                    color = touchGlowColor.copy(alpha = ringAlpha1),
                    radius = 22f + maxRingRadius * progress,
                    center = rCenter,
                    style = Stroke(width = 3.2f * (1f - progress * 0.55f))
                )

                // Secondary delayed ripple ring for fluid luxury feel
                val delayed = ((progress - 0.16f) / 0.84f).coerceIn(0f, 1f)
                if (delayed > 0f && delayed < 1f) {
                    val fade2 = (1f - delayed) * (1f - delayed)
                    val ringAlpha2 = (if (isDarkTheme) 0.22f else 0.15f) * fade2
                    drawCircle(
                        color = ribbonColor.copy(alpha = ringAlpha2),
                        radius = 18f + (maxRingRadius * 0.78f) * delayed,
                        center = rCenter,
                        style = Stroke(width = 2.0f * (1f - delayed * 0.5f))
                    )
                }
            }
        }

        content()
    }
}
