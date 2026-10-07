package com.ferrisfeed.coreui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Small centered difficulty label, the only header chrome on a reel card
 * (Spec v2: replaces TrackPill + LevelBadge + read-time).
 *
 * Maps content `level`: 1 -> Easy, 2 -> Medium, 3+ -> Hard. Each tier has a
 * distinct looping motion so difficulty reads at a glance, even peripherally:
 * Easy breathes (slow scale pulse), Medium shimmers (gradient sweep),
 * Hard flickers like an ember (fast small alpha jitter).
 *
 * TODO 24b: [animated] gates the motion. The speaker-opening flow keeps it
 * false until recognition completes (`loading-states`: the cue is the
 * reward, not the wait); text composes immediately either way so capture
 * never blocks readiness (`doherty-threshold`).
 */
@Composable
fun DifficultyLabel(
    level: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val (text, color) = when {
        level <= 1 -> "easy" to FerrisColors.MintCorrect
        level == 2 -> "medium" to FerrisColors.FerrisAmber
        else -> "hard" to FerrisColors.Error
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Difficulty: $text" },
        contentAlignment = Alignment.Center,
    ) {
        if (!animated) {
            LabelText(text = text, color = color)
        } else when {
            level <= 1 -> BreathingLabel(text = text, color = color)
            level == 2 -> ShimmerLabel(text = text, color = color)
            else -> FlickerLabel(text = text, color = color)
        }
    }
}

@Composable
private fun LabelText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

/** Easy: slow scale pulse, ~2s cycle. */
@Composable
private fun BreathingLabel(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "easy-breath")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    LabelText(text = text, color = color, modifier = Modifier.scale(scale))
}

/** Medium: horizontal shimmer sweep across the text, ~1.6s cycle. */
@Composable
private fun ShimmerLabel(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "medium-shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = 0.45f),
            color,
            color.copy(alpha = 0.45f),
        ),
        start = Offset(x = offset * 200f - 200f, y = 0f),
        end = Offset(x = offset * 200f, y = 0f),
    )
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            brush = brush,
        ),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

/** Hard: ember flicker — fast small alpha jitter, ~0.9s cycle. */
@Composable
private fun FlickerLabel(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "hard-flicker")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flicker",
    )
    LabelText(text = text, color = color, modifier = Modifier.alpha(alpha))
}

@Preview(name = "Difficulty labels", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun DifficultyLabelPreview() {
    FerrisFeedTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column {
            DifficultyLabel(level = 1)
            DifficultyLabel(level = 2)
            DifficultyLabel(level = 3)
        }
    }
}
