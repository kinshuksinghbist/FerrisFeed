package com.ferrisfeed.coreui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Canonical track identifiers used across feed, path, and search. */
object Tracks {
    const val RUST = "rust"
    const val WASM = "wasm"
    const val SYSTEM_DESIGN = "system-design"

    fun label(track: String): String = when (track) {
        RUST -> "Rust"
        WASM -> "WASM"
        SYSTEM_DESIGN -> "SysDesign"
        else -> track
    }
}

@Composable
fun trackColor(track: String): Color {
    val tracks = LocalTrackColors.current
    return when (track) {
        Tracks.RUST -> tracks.rust
        Tracks.WASM -> tracks.wasm
        Tracks.SYSTEM_DESIGN -> tracks.systemDesign
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Small pill showing the track with its signature color dot.
 * Minimum touch semantics provided via content description; visual size stays compact
 * because it sits inside the reel header alongside other controls.
 */
@Composable
fun TrackPill(
    track: String,
    modifier: Modifier = Modifier,
) {
    val dot = trackColor(track)
    Surface(
        modifier = modifier.semantics { contentDescription = "Track ${Tracks.label(track)}" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = Tracks.label(track),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Level badge: L1..L4 rendered as compact text chip. */
@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = "L$level",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview(name = "TrackPill dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun TrackPillPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Row(Modifier.padding(16.dp)) {
            TrackPill(Tracks.RUST)
            Spacer(Modifier.width(8.dp))
            TrackPill(Tracks.WASM)
            Spacer(Modifier.width(8.dp))
            TrackPill(Tracks.SYSTEM_DESIGN)
        }
    }
}

@Preview(name = "TrackPill light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun TrackPillLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        Row(Modifier.padding(16.dp)) {
            TrackPill(Tracks.RUST)
            Spacer(Modifier.width(8.dp))
            LevelBadge(2)
        }
    }
}
