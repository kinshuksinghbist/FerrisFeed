package com.ferrisfeed.coreui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Canonical track identifiers used across feed, path, and search.
 * WASM content is dormant in repo; do not re-add without a spec change.
 *
 * The `TrackPill` / `LevelBadge` composables that used to live here were
 * deleted with the reel-header overhaul (Spec v2): difficulty is now the
 * centered [DifficultyLabel] and track identity is the card wash itself.
 */
object Tracks {
    const val RUST = "rust"
    const val SYSTEM_DESIGN = "system-design"

    fun label(track: String): String = when (track) {
        RUST -> "Rust"
        SYSTEM_DESIGN -> "SysDesign"
        else -> track
    }
}

@Composable
fun trackColor(track: String): Color {
    val tracks = LocalTrackColors.current
    return when (track) {
        Tracks.RUST -> tracks.rust
        Tracks.SYSTEM_DESIGN -> tracks.systemDesign
        else -> MaterialTheme.colorScheme.primary
    }
}
