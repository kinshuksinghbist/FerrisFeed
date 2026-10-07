package com.ferrisfeed.coreui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Three vertical bars difficulty indicator (P6 34b).
 * Bars fill sequentially on first appearance, with post-speech reward animations.
 */
@Composable
fun DifficultyLabel(
    level: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val levelClamped = level.coerceIn(1, 3)
    val word = when (levelClamped) {
        1 -> "Easy"
        2 -> "Medium"
        else -> "Hard"
    }
    val activeColor = when (levelClamped) {
        1 -> FerrisColors.MintCorrect
        2 -> FerrisColors.FerrisAmber
        else -> FerrisColors.Error
    }

    val reduceMotion = LocalReduceMotion.current

    // Sequential fill on appearance (80ms apart, Snappy)
    val bar0 = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val bar1 = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val bar2 = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            bar0.animateTo(1f, FerrisMotion.Snappy)
            if (levelClamped >= 2) {
                delay(80)
                bar1.animateTo(1f, FerrisMotion.Snappy)
            }
            if (levelClamped >= 3) {
                delay(80)
                bar2.animateTo(1f, FerrisMotion.Snappy)
            }
        }
    }

    // Tier looping motions only when animated
    val shouldLoop = animated && !reduceMotion
    val loopScale = if (shouldLoop && levelClamped == 1) {
        val transition = rememberInfiniteTransition(label = "easy-pulse")
        transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse-scale",
        ).value
    } else 1f

    val loopAlpha = if (shouldLoop && levelClamped == 2) {
        val transition = rememberInfiniteTransition(label = "medium-shimmer")
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shimmer-alpha",
        ).value
    } else if (shouldLoop && levelClamped >= 3) {
        val transition = rememberInfiniteTransition(label = "hard-flicker")
        transition.animateFloat(
            initialValue = 0.60f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(450),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "flicker-alpha",
        ).value
    } else 1f

    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val barHeights = listOf(8.dp, 12.dp, 16.dp)
    val animatables = listOf(bar0, bar1, bar2)

    Row(
        modifier = modifier.semantics { contentDescription = "Difficulty: ${word.lowercase()}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .scale(loopScale)
                .alpha(loopAlpha),
        ) {
            for (i in 0..2) {
                val isFilled = i < levelClamped
                val fillProgress = animatables[i].value
                val color = if (isFilled && fillProgress > 0.05f) activeColor else inactiveColor
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeights[i])
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = word,
            style = MaterialTheme.typography.labelMedium,
            color = activeColor,
        )
    }
}

@Preview(name = "Difficulty labels", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun DifficultyLabelPreview() {
    FerrisFeedTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.background(androidx.compose.ui.graphics.Color(0xFF0B0E14)),
        ) {
            DifficultyLabel(level = 1)
            DifficultyLabel(level = 2)
            DifficultyLabel(level = 3)
        }
    }
}
