package com.ferrisfeed.coreui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for skeleton loading states (P6 39d).
 * Automatically disables animation under reduce-motion and falls back to a subtle static opacity.
 */
fun Modifier.shimmer(): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    if (reduceMotion) {
        return@composed this.background(base)
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )

    this.background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(translate - 350f, 0f),
            end = Offset(translate, 120f),
        ),
    )
}

@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    height: Int = 16,
    widthFraction: Float = 1f,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .clip(RoundedCornerShape(8.dp))
            .shimmer(),
    )
}

/** Full reel skeleton matching new [ReelCard] proportions (P6 39d). */
@Composable
fun ReelSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 24.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmer(),
                )
            }

            Spacer(Modifier.height(4.dp))

            // 3-line hook
            ShimmerBar(height = 26, widthFraction = 0.95f)
            ShimmerBar(height = 26, widthFraction = 0.75f)

            Spacer(Modifier.height(6.dp))

            // 3-line body
            ShimmerBar(height = 16, widthFraction = 1f)
            ShimmerBar(height = 16, widthFraction = 0.9f)
            ShimmerBar(height = 16, widthFraction = 0.65f)

            Spacer(Modifier.height(8.dp))

            // Code panel block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmer(),
            )
        }
    }
}

/** Path node skeleton row for Path screen loading (P6 39d). */
@Composable
fun PathNodeSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ring circle skeleton
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .shimmer(),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerBar(height = 18, widthFraction = 0.7f)
            ShimmerBar(height = 14, widthFraction = 0.45f)
        }
    }
}

@Preview(name = "Shimmer dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ShimmerPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReelSkeleton()
            PathNodeSkeleton()
        }
    }
}
