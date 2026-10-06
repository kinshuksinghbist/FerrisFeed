package com.ferrisfeed.coreui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding

/**
 * Mastery ring. [progress] is 0f..1f. Color shifts from track color (low) to mint (mastered).
 * Animated so lighting up a roadmap node feels rewarding but stays within the 300ms spring budget.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 5.dp,
    label: String? = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = clamped, label = "progress")
    val color: Color = if (clamped >= 0.8f) FerrisColors.MintCorrect
    else MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = strokeWidth,
        )
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "ProgressRing", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ProgressRingPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Row(Modifier.padding(16.dp)) {
            ProgressRing(0.25f)
            Spacer(Modifier.width(12.dp))
            ProgressRing(0.62f)
            Spacer(Modifier.width(12.dp))
            ProgressRing(0.95f)
        }
    }
}
