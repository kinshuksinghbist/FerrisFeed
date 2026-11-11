package com.ferrisfeed.path

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisChip
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisSurface
import com.ferrisfeed.coreui.FerrisTheme
import com.ferrisfeed.coreui.LevelMeter
import com.ferrisfeed.coreui.TrackTag
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.data.Curriculum

/**
 * Title-cases each word of a stored topic string. Kept for legacy labels and
 * tests; screens prefer [Curriculum.titleOf], which knows the real names.
 */
internal fun String.displayTopic(): String = split(" ", "_", "-")
    .filter { it.isNotBlank() }
    .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/** Searchable reel summary (projection of the full reel; Room FTS returns these). */
data class SearchResult(
    val id: String,
    val track: String,
    val level: Int,
    /** Exact stored topic key — the same string the curriculum and topic feed use. */
    val topic: String,
    val hook: String,
    val takeaway: String,
    val hasCode: Boolean,
    val hasQuiz: Boolean,
    val snippet: String = "",
)

/**
 * Browse filters. null = no constraint; any set value narrows. [topic] is an
 * exact match on the stored topic key.
 */
data class SearchFilters(
    val track: String? = null,
    val level: Int? = null,
    val hasCode: Boolean? = null,
    val hasQuiz: Boolean? = null,
    val topic: String? = null,
) {
    fun isScoped(): Boolean =
        track != null || level != null || hasCode != null || hasQuiz != null || topic != null

    fun cleared(): SearchFilters = SearchFilters()
}

/** Search field with an auto-focused caret and a clear/close button. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val palette = FerrisTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, palette.hairline, shape)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search lessons, e.g. borrow, Raft",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        FerrisIconButton(
            icon = Icons.Filled.Close,
            contentDescription = if (query.isEmpty()) "Close search" else "Clear search",
            onClick = { if (query.isEmpty()) onClose() else onQueryChange("") },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Track and level chips under the search field. */
@Composable
fun SearchFilterRow(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Tracks.RUST, Tracks.SYSTEM_DESIGN).forEach { track ->
            FerrisChip(
                label = Tracks.label(track),
                selected = filters.track == track,
                leadingDot = trackColor(track),
                onClick = {
                    onFiltersChange(filters.copy(track = if (filters.track == track) null else track, topic = null))
                },
            )
        }
        (1..3).forEach { level ->
            FerrisChip(
                label = Tracks.levelLabel(level),
                selected = filters.level == level,
                onClick = { onFiltersChange(filters.copy(level = if (filters.level == level) null else level)) },
            )
        }
    }
}

/** One search hit: meta line, hook, takeaway. */
@Composable
fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FerrisSurface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackTag(result.track, Curriculum.titleOf(result.topic), Modifier.weight(1f))
                LevelMeter(result.level, showLabel = false)
            }
            Text(
                text = result.hook,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.takeaway.isNotBlank()) {
                Text(
                    text = result.takeaway,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "Search row", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun SearchRowPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SearchField(query = "borrow", onQueryChange = {}, onClose = {})
            SearchFilterRow(filters = SearchFilters(track = Tracks.RUST), onFiltersChange = {})
            SearchResultRow(
                result = SearchResult(
                    id = "1", track = Tracks.RUST, level = 2, topic = "borrowing",
                    hook = "Only one writer allowed — Rust enforces it at compile time.",
                    takeaway = "Mutable borrows are exclusive by design.",
                    hasCode = true, hasQuiz = true,
                ),
                onClick = {},
            )
        }
    }
}
