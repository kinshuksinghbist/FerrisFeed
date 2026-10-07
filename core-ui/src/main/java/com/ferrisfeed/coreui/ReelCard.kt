package com.ferrisfeed.coreui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Unified reel info card (Spec v2).
 *
 * Layout, top to bottom, nothing else: centered [DifficultyLabel] -> hook
 * (headline) -> body -> takeaway as a plain closing line -> code well
 * contained in the same card. The container
 * carries a low-alpha wash of the track color (orange Rust, sky-blue
 * System Design) instead of any pill or badge.
 *
 * Deliberately action-free: like/save live on the feed's right rail, quiz
 * grading is the SRS signal, code has its own card. No inner scroll — the
 * pager owns all vertical motion.
 *
 * Contrast (TODO 23a): the container comes from [trackWash] (14% blend, so
 * body text sits on a near-surface color) and the takeaway closing line
 * uses [trackTextColor] (per-theme ink holding >= 4.5:1), never the raw
 * track hue. Shaped by `accessibility-audit` + `critique-color`.
 *
 * Speaker flow (TODO 24b): [animateDifficulty] gates the animated cue. The
 * caller keeps it false until speech recognition completes, so the motion
 * rewards the capture instead of competing with it; text is always composed
 * immediately (capture never blocks readiness, `doherty-threshold`).
 *
 * Code (user review 2026-10-07): the snippet is contained in this card via
 * [CodeBlock] — never a separate card — with copy + flip-to-output.
 */
@Composable
fun ReelCard(
    track: String,
    level: Int,
    hook: String,
    body: String,
    takeaway: String,
    code: String? = null,
    language: String = "rust",
    output: String? = null,
    modifier: Modifier = Modifier,
    animateDifficulty: Boolean = true,
) {
    val tinted = trackWash(track)
    val takeawayColor = trackTextColor(track)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tinted),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            DifficultyLabel(level = level, animated = animateDifficulty)

            Spacer(Modifier.height(10.dp))

            Text(
                text = hook,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (takeaway.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "\u2192 $takeaway",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = takeawayColor,
                )
            }

            if (!code.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                CodeBlock(
                    code = code,
                    language = language,
                    output = output,
                )
            }
        }
    }
}

@Preview(name = "ReelCard rust", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ReelCardRustPreview() {
    FerrisFeedTheme(darkTheme = true) {
        ReelCard(
            track = Tracks.RUST,
            level = 1,
            hook = "Why does this simple function not compile?",
            body = "Ownership moves values by default. When you pass a String to a function, the caller loses it. Borrow with & to keep using it afterwards.",
            takeaway = "Move by default; borrow with & to keep ownership.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "ReelCard sysdesign hard", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun ReelCardSysDesignPreview() {
    FerrisFeedTheme(darkTheme = false) {
        ReelCard(
            track = Tracks.SYSTEM_DESIGN,
            level = 3,
            hook = "Cache-aside: why do we check Redis before Postgres?",
            body = "Reads hit the fast cache first. On miss, load from Postgres and refill the cache with a TTL. Writes delete the cache key to avoid stale reads.",
            takeaway = "Read from cache, fall back to DB, invalidate on write.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
