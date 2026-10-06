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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shimmer skeleton shown while the pager prefetches the next 5 reels.
 * Uses an infinite gradient sweep; keep usage brief (prefetch path only) to save battery.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(translate - 300f, 0f),
            end = Offset(translate, 100f),
        ),
    )
}

@Composable
private fun ShimmerBar(
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

/** Full reel skeleton matching [ReelCard] proportions to avoid layout shift. */
@Composable
fun ReelSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBar(widthFraction = 0.5f, height = 22)
            ShimmerBar(height = 28)
            ShimmerBar(widthFraction = 0.85f)
            ShimmerBar(widthFraction = 0.9f)
            ShimmerBar(widthFraction = 0.7f)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer(),
            )
            ShimmerBar(widthFraction = 0.6f, height = 20)
        }
    }
}

@Preview(name = "Shimmer dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ShimmerPreview() {
    FerrisFeedTheme(darkTheme = true) {
        ReelSkeleton(modifier = Modifier.padding(16.dp))
    }
}

// Unused import guard for Color (kept for future tint customization).
@Suppress("unused")
private val ShimmerTintFallback = Color.Transparent
