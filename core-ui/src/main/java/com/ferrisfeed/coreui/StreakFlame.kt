package com.ferrisfeed.coreui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Streak flame: shows current day streak with lively flicker and tier coloring (P6 39b).
 * Respects reduce-motion by suppressing looping jitter.
 */
@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier,
    celebrate: Boolean = false,
) {
    val reduceMotion = LocalReduceMotion.current
    val shouldFlicker = streakDays > 0 && !reduceMotion

    val celebrateScale by animateFloatAsState(
        targetValue = if (celebrate && !reduceMotion) 1.28f else 1f,
        animationSpec = FerrisMotion.Bouncy,
        label = "flame-pop",
    )

    val (flickerScale, flickerAlpha) = if (shouldFlicker) {
        val transition = rememberInfiniteTransition(label = "flame-flicker")
        val scale by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "flame-scale",
        )
        val alpha by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "flame-alpha",
        )
        scale to alpha
    } else {
        1f to 1f
    }

    val flameColor = when {
        streakDays <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        streakDays < 7 -> FerrisColors.FerrisOrange
        streakDays < 30 -> FerrisColors.FerrisAmber
        else -> Color(0xFFFF3B30) // High streak red-orange
    }

    Box(
        modifier = modifier
            .glass(CircleShape)
            .clip(CircleShape)
            .semantics {
                contentDescription = "Streak: $streakDays days"
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = flameColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(celebrateScale * flickerScale)
                    .alpha(flickerAlpha),
            )
            Spacer(Modifier.width(4.dp))
            AnimatedCounter(
                value = streakDays,
                style = MaterialTheme.typography.titleMedium,
                color = if (streakDays > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Preview(name = "StreakFlame Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun StreakFlamePreview() {
    FerrisFeedTheme(darkTheme = true) {
        Row(Modifier.padding(16.dp)) {
            StreakFlame(0)
            Spacer(Modifier.width(8.dp))
            StreakFlame(7)
            Spacer(Modifier.width(8.dp))
            StreakFlame(35, celebrate = true)
        }
    }
}
