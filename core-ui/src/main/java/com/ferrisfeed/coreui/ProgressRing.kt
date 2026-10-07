package com.ferrisfeed.coreui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mastery ring with round caps, gradient arc, and glow at >= 80% (P6 39c).
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
    val reduceMotion = LocalReduceMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reduceMotion) FerrisMotion.Quick else FerrisMotion.Smooth,
        label = "progress-ring",
    )

    val isMastered = clamped >= 0.8f
    val trackBase = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val activeColor = if (isMastered) FerrisColors.MintCorrect else MaterialTheme.colorScheme.primary
    val glowColor = if (isMastered) FerrisColors.MintCorrect.copy(alpha = 0.35f) else Color.Transparent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background track
            drawArc(
                color = trackBase,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Glow arc at >= 80%
            if (isMastered) {
                drawArc(
                    color = glowColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Foreground progress arc
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        if (label != null) {
            Text(
                text = label,
                style = NumberStyle.copy(fontSize = (size.value * 0.28f).coerceAtLeast(10f).dp.value.let { MaterialTheme.typography.labelSmall.fontSize }),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "ProgressRing Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
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
