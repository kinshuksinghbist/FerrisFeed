package com.ferrisfeed.coreui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ferris mascot brand mark drawn in Canvas (P6 39a).
 * Features an animated claw wiggle on idle that pauses when reduce-motion is enabled.
 */
@Composable
fun FerrisMark(
    size: Dp = 48.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val shouldAnimate = animated && !reduceMotion

    val clawAngle = if (shouldAnimate) {
        val transition = rememberInfiniteTransition(label = "ferris-claw")
        val angle by transition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "claw-wiggle",
        )
        angle
    } else {
        0f
    }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val orange = FerrisColors.FerrisOrange
            val eyeDark = Color(0xFF0B0E14)
            val white = Color.White

            // Left claw
            rotate(degrees = clawAngle, pivot = Offset(w * 0.22f, h * 0.38f)) {
                drawArc(
                    color = orange,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.06f, h * 0.16f),
                    size = Size(w * 0.28f, h * 0.28f),
                )
            }

            // Right claw
            rotate(degrees = -clawAngle, pivot = Offset(w * 0.78f, h * 0.38f)) {
                drawArc(
                    color = orange,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.66f, h * 0.16f),
                    size = Size(w * 0.28f, h * 0.28f),
                )
            }

            // Crab main body (ellipse)
            drawOval(
                color = orange,
                topLeft = Offset(w * 0.15f, h * 0.32f),
                size = Size(w * 0.70f, h * 0.52f),
            )

            // Small crab legs (subtle strokes/dots at base)
            drawCircle(orange, radius = w * 0.05f, center = Offset(w * 0.24f, h * 0.84f))
            drawCircle(orange, radius = w * 0.05f, center = Offset(w * 0.40f, h * 0.88f))
            drawCircle(orange, radius = w * 0.05f, center = Offset(w * 0.60f, h * 0.88f))
            drawCircle(orange, radius = w * 0.05f, center = Offset(w * 0.76f, h * 0.84f))

            // Eye stalks & eyes
            val leftEyeCenter = Offset(w * 0.38f, h * 0.42f)
            val rightEyeCenter = Offset(w * 0.62f, h * 0.42f)

            // Eye whites
            drawCircle(white, radius = w * 0.09f, center = leftEyeCenter)
            drawCircle(white, radius = w * 0.09f, center = rightEyeCenter)

            // Eye pupils
            drawCircle(eyeDark, radius = w * 0.055f, center = Offset(w * 0.39f, h * 0.42f))
            drawCircle(eyeDark, radius = w * 0.055f, center = Offset(w * 0.63f, h * 0.42f))

            // White eye glints
            drawCircle(white, radius = w * 0.02f, center = Offset(w * 0.38f, h * 0.39f))
            drawCircle(white, radius = w * 0.02f, center = Offset(w * 0.62f, h * 0.39f))
        }
    }
}

@Preview(name = "FerrisMark Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun FerrisMarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        FerrisMark(size = 64.dp)
    }
}
