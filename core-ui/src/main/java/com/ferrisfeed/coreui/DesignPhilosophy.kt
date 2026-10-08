package com.ferrisfeed.coreui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Radical design philosophy switcher (P7 Item 43):
 * - [StudioGlass]: Ethereal floating glassmorphism, OLED dark, soft radial glow, 3D specular rim.
 * - [NeoBrutalism]: Cyberpunk arcade HUD, hard 4dp offset 3D shadow, bold 2.5dp borders, mechanical press.
 */
enum class DesignPhilosophy(val displayName: String, val tagline: String) {
    StudioGlass("Studio Glass", "Fluid OLED Glass & Atmospheric Glow"),
    NeoBrutalism("Cyber Brutalist HUD", "Bold High-Contrast Outlines & Hard 3D Shadows"),
}

/** Ambient CompositionLocal providing active design philosophy. */
val LocalDesignPhilosophy = staticCompositionLocalOf { DesignPhilosophy.StudioGlass }

/**
 * Applies tactile 3D physical depth according to the active [DesignPhilosophy].
 * - In StudioGlass: Layered elevation shadow, specular top-edge rim, and 3D glass border.
 * - In NeoBrutalism: Hard offset 3D block shadow (no blur), solid 2.5dp border, mechanical press.
 */
fun Modifier.tactileDepth(
    shape: Shape = RoundedCornerShape(24.dp),
    depth: Dp = 6.dp,
    cornerRadius: Dp = 24.dp,
): Modifier = composed {
    val philosophy = LocalDesignPhilosophy.current
    val isDark = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current

    when (philosophy) {
        DesignPhilosophy.StudioGlass -> {
            this
                .shadow(
                    elevation = depth * 1.5f,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.20f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.65f else 0.25f),
                )
                .background(glassColor(), shape)
                .border(
                    width = 0.75.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.40f),
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.80f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.18f),
                            )
                        },
                    ),
                    shape = shape,
                )
                .drawBehind {
                    // Subtle specular rim highlight at the top edge
                    val highlightHeight = 3.dp.toPx()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (isDark) 0.18f else 0.40f),
                            1f to Color.Transparent,
                            startY = 0f,
                            endY = highlightHeight,
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    )
                }
        }
        DesignPhilosophy.NeoBrutalism -> {
            val shadowColor = if (isDark) Color(0xFF00E5FF) else Color(0xFF0F141C)
            val borderColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F141C)
            val containerColor = if (isDark) Color(0xFF131722) else Color(0xFFFFFDF8)
            val offsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }

            this
                .drawBehind {
                    // Hard offset 3D shadow (no blur)
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = size,
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    )
                }
                .background(containerColor, shape)
                .border(2.5.dp, borderColor, shape)
        }
    }
}

/**
 * Interactive click modifier that reacts according to active philosophy:
 * - StudioGlass: Fluid spring micro-scale (0.97f).
 * - NeoBrutalism: Mechanical translation (+2.5dp, +2.5dp) pressing down into the 3D shadow.
 */
fun Modifier.tactileClickable(
    interactionSource: InteractionSource,
    pressedOffset: Dp = 2.5.dp,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val philosophy = LocalDesignPhilosophy.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current

    if (reduceMotion) return@composed this

    when (philosophy) {
        DesignPhilosophy.StudioGlass -> {
            val scale by animateFloatAsState(
                targetValue = if (isPressed) pressedScale else 1f,
                animationSpec = FerrisMotion.Snappy,
                label = "glass-click-scale",
            )
            this.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        }
        DesignPhilosophy.NeoBrutalism -> {
            val offset by animateDpAsState(
                targetValue = if (isPressed) pressedOffset else 0.dp,
                animationSpec = FerrisMotion.SnappyDp,
                label = "brutalist-press-offset",
            )
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            this.graphicsLayer {
                translationX = offset.value * density
                translationY = offset.value * density
            }
        }
    }
}
