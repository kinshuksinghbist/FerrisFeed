package com.ferrisfeed.coreui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared motion tokens (P6 31a).
 * Do not inline ad-hoc tween numbers in screens.
 */
object FerrisMotion {
    val Snappy = spring<Float>(dampingRatio = 0.8f, stiffness = 500f)
    val Bouncy = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)
    val Smooth = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
    val Quick = tween<Float>(durationMillis = 150, easing = LinearOutSlowInEasing)

    val QuickOffset = tween<IntOffset>(durationMillis = 150, easing = LinearOutSlowInEasing)
    val SmoothOffset = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
    val SnappyOffset = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 500f)
    val BouncyOffset = spring<IntOffset>(dampingRatio = 0.55f, stiffness = 380f)

    val SnappyDp = spring<Dp>(dampingRatio = 0.8f, stiffness = 500f)
    val BouncyDp = spring<Dp>(dampingRatio = 0.55f, stiffness = 380f)
    val QuickDp = tween<Dp>(durationMillis = 150, easing = LinearOutSlowInEasing)
    val SmoothDp = tween<Dp>(durationMillis = 350, easing = FastOutSlowInEasing)

    val QuickSize = tween<IntSize>(durationMillis = 150, easing = LinearOutSlowInEasing)
    val SmoothSize = tween<IntSize>(durationMillis = 350, easing = FastOutSlowInEasing)

    val QuickColor = tween<Color>(durationMillis = 150, easing = LinearOutSlowInEasing)
    val SmoothColor = tween<Color>(durationMillis = 350, easing = FastOutSlowInEasing)

    const val StaggerMs = 45
}

/** Flag to respect system reduce-motion settings (P6 31b). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Scales slightly on touch press with Snappy spring feedback (P6 31c). */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressed: Float = 0.94f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressed else 1f,
        animationSpec = FerrisMotion.Snappy,
        label = "press-scale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Translucent container with a 0.5dp hairline border (P6 31d). */
fun Modifier.glass(shape: Shape): Modifier = composed {
    this.background(glassColor(), shape)
        .border(0.5.dp, glassStroke(), shape)
}

/** Staggered fade and slide entrance for list items (P6 31e). */
fun Modifier.staggeredEntrance(index: Int): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) return@composed this

    var hasEntered by rememberSaveable { mutableStateOf(false) }
    val alpha = remember { Animatable(if (hasEntered) 1f else 0f) }
    val offsetY = remember { Animatable(if (hasEntered) 0f else 24f) }

    LaunchedEffect(Unit) {
        if (!hasEntered) {
            val delayMs = (index.coerceAtMost(8) * FerrisMotion.StaggerMs).toLong()
            delay(delayMs)
            launch { alpha.animateTo(1f, FerrisMotion.Smooth) }
            launch { offsetY.animateTo(0f, FerrisMotion.Smooth) }
            hasEntered = true
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = offsetY.value * density
    }
}

/** Animated number counter with per-digit slide (P6 31f). */
@Composable
fun AnimatedCounter(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val text = value.toString()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in text.indices) {
            val char = text[i]
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically(animationSpec = FerrisMotion.QuickOffset) { it } + fadeIn(animationSpec = FerrisMotion.Quick))
                            .togetherWith(slideOutVertically(animationSpec = FerrisMotion.QuickOffset) { -it } + fadeOut(animationSpec = FerrisMotion.Quick))
                    } else {
                        (slideInVertically(animationSpec = FerrisMotion.QuickOffset) { -it } + fadeIn(animationSpec = FerrisMotion.Quick))
                            .togetherWith(slideOutVertically(animationSpec = FerrisMotion.QuickOffset) { it } + fadeOut(animationSpec = FerrisMotion.Quick))
                    }
                },
                label = "digit-$i",
            ) { c ->
                Text(
                    text = c.toString(),
                    style = style,
                    color = color,
                    softWrap = false,
                )
            }
        }
    }
}

private data class Particle(
    val initialSpeed: Float,
    val angleRad: Float,
    val size: Float,
    val isCircle: Boolean,
    val color: Color,
    val rotationSpeed: Float,
)

private val ConfettiColors = listOf(
    Color(0xFFFF6B35), // Ferris orange
    Color(0xFF00D9A6), // Mint
    Color(0xFFFFC857), // Amber/gold
    Color(0xFFB8A6FF), // Lavender
)

/** Confetti burst on mastery or celebration (P6 31g). */
@Composable
fun ConfettiBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion || trigger <= 0) return

    val progress = remember(trigger) { Animatable(0f) }

    val particles = remember(trigger) {
        val rng = Random(trigger)
        List(60) {
            val angle = rng.nextFloat() * (2f * Math.PI.toFloat())
            val speed = rng.nextFloat() * 400f + 250f
            val size = rng.nextFloat() * 7f + 5f
            val isCircle = rng.nextBoolean()
            val color = ConfettiColors[rng.nextInt(ConfettiColors.size)]
            val rotationSpeed = (rng.nextFloat() - 0.5f) * 720f
            Particle(speed, angle, size, isCircle, color, rotationSpeed)
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics {}) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val t = progress.value
            val alpha = (1f - t).coerceIn(0f, 1f)
            val gravity = 320f * (t * t)

            for (p in particles) {
                val distance = p.initialSpeed * t
                val x = center.x + cos(p.angleRad) * distance
                val y = center.y + sin(p.angleRad) * distance + gravity
                val rotation = p.rotationSpeed * t

                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size / 2f,
                            center = Offset(x, y),
                        )
                    } else {
                        drawRoundRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(x - p.size / 2f, y - p.size / 3f),
                            size = Size(p.size, p.size * 0.7f),
                            cornerRadius = CornerRadius(2f, 2f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "AnimatedCounter Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun AnimatedCounterPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        AnimatedCounter(
            value = 42,
            style = NumberStyle,
            color = FerrisColors.FerrisOrange,
        )
    }
}

@Preview(name = "AnimatedCounter Preview Light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun AnimatedCounterPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        AnimatedCounter(
            value = 100,
            style = NumberStyle,
            color = FerrisColors.FerrisOrange,
        )
    }
}
