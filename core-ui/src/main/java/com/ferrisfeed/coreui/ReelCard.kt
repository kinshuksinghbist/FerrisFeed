package com.ferrisfeed.coreui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Main reel container. Layout contract (see docs/feed-ux.md):
 * - Header: [TrackPill] + [LevelBadge] + read-time label.
 * - Center: hook (headline), body (markdown-lite plain text), takeaway callout.
 * - Actions: Like / Save icon buttons, Deep Dive button, GotIt / Fuzzy SRS buttons.
 *
 * This card takes primitives so it can be used from feed, search, and path modules
 * without pulling a domain model into :core-ui.
 */
@Composable
fun ReelCard(
    track: String,
    level: Int,
    readTimeSec: Int,
    hook: String,
    body: String,
    takeaway: String,
    isLiked: Boolean,
    isSaved: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDeepDive: () -> Unit,
    onGotIt: () -> Unit,
    onFuzzy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrackPill(track = track)
                LevelBadge(level = level)
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatReadTime(readTimeSec),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Hook: curiosity gap, single line in data, allowed to wrap to 2 lines visually.
            Text(
                text = hook,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            // Body: 60-second explainer, plain text (markdown-lite rendered upstream).
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            // Takeaway callout
            TakeawayRow(takeaway = takeaway)

            Spacer(Modifier.height(16.dp))

            // Actions row 1: like / save / deep dive
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) FerrisColors.FerrisOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isSaved) "Unsave" else "Save",
                        tint = if (isSaved) FerrisColors.WasmBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDeepDive) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Deep Dive")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Actions row 2: SRS feedback — feeds the scheduler in rust-core / FeedViewModel.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onGotIt,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Got it ✓")
                }
                OutlinedButton(
                    onClick = onFuzzy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Still fuzzy")
                }
            }
        }
    }
}

@Composable
private fun TakeawayRow(takeaway: String) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "→ ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = takeaway,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

fun formatReadTime(seconds: Int): String {
    return if (seconds < 60) "${seconds}s read" else "${seconds / 60}m ${seconds % 60}s read"
}

@Preview(name = "ReelCard dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ReelCardDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        ReelCard(
            track = Tracks.RUST,
            level = 1,
            readTimeSec = 55,
            hook = "Why does this simple function not compile?",
            body = "Ownership moves values by default. When you pass a String to a function, the caller loses it. Borrow with & to keep using it afterwards.",
            takeaway = "Move by default; borrow with & to keep ownership.",
            isLiked = false,
            isSaved = true,
            onLike = {},
            onSave = {},
            onDeepDive = {},
            onGotIt = {},
            onFuzzy = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "ReelCard light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun ReelCardLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        ReelCard(
            track = Tracks.SYSTEM_DESIGN,
            level = 2,
            readTimeSec = 70,
            hook = "Cache-aside: why do we check Redis before Postgres?",
            body = "Reads hit the fast cache first. On miss, load from Postgres and refill the cache with a TTL. Writes delete the cache key to avoid stale reads.",
            takeaway = "Read from cache, fall back to DB, invalidate on write.",
            isLiked = true,
            isSaved = false,
            onLike = {},
            onSave = {},
            onDeepDive = {},
            onGotIt = {},
            onFuzzy = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
