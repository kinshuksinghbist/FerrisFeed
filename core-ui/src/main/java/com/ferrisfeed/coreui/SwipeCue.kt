package com.ferrisfeed.coreui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Bouncing swipe cue (P6 33e + 38e).
 * Displays a 3-chevron stack bouncing 10dp up (900ms infinite loop: 450ms up + 450ms down)
 * with descriptive micro-copy in [labelMedium].
 * Respects [LocalReduceMotion].
 */
@Composable
fun SwipeCue(
    text: String = "Swipe for next",
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = LocalReduceMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "SwipeBounce")
    val bounceY by if (reduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "BounceY",
        )
    }

    Column(
        modifier = modifier.graphicsLayer { translationY = bounceY * density },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 20.dp).clearAndSetSemantics {}) {
            val strokeWidth = 2.dp.toPx()
            val w = size.width
            val chevronHeight = 5.dp.toPx()
            // 3 chevrons stacked
            for (i in 0..2) {
                val yTop = i * 4.5.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, yTop + chevronHeight)
                    lineTo(w / 2f, yTop)
                    lineTo(w, yTop + chevronHeight)
                }
                val alpha = (1f - i * 0.25f).coerceIn(0.3f, 1f)
                drawPath(
                    path = path,
                    color = color.copy(alpha = alpha),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DisplayFont,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
    }
}

@Preview(name = "SwipeCue dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SwipeCueDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        SwipeCue()
    }
}

@Preview(name = "SwipeCue light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun SwipeCueLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        SwipeCue()
    }
}
