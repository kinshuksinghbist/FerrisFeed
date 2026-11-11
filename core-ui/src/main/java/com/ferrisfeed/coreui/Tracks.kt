package com.ferrisfeed.coreui

/**
 * Canonical track identifiers used across feed, path, and search.
 * WASM content is dormant in the repo; do not re-add without a spec change.
 * Colours live in Theme.kt ([trackColor], [trackTextColor], [trackSoftColor]).
 */
object Tracks {
    const val RUST = "rust"
    const val SYSTEM_DESIGN = "system-design"

    fun label(track: String): String = when (track) {
        RUST -> "Rust"
        SYSTEM_DESIGN -> "System Design"
        else -> track
    }

    /** Difficulty word for a content level (schema 1..4; seeded content uses 1..3). */
    fun levelLabel(level: Int): String = when {
        level <= 1 -> "Beginner"
        level == 2 -> "Intermediate"
        else -> "Advanced"
    }
}
