package com.ferrisfeed.path

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor

/** Searchable reel summary (projection of the full reel; Room FTS returns these). */
data class SearchResult(
    val id: String,
    val track: String,
    val level: Int,
    val hook: String,
    val takeaway: String,
    val hasCode: Boolean,
    val hasQuiz: Boolean,
    val snippet: String = "",
)

data class SearchFilters(
    val track: String? = null, // null = all
    val level: Int? = null,
    val hasCode: Boolean? = null, // null = either
    val hasQuiz: Boolean? = null,
)

/** Pure filter used by Room FTS path and previews; keeps UI logic unit-testable. */
fun filterResults(
    all: List<SearchResult>,
    query: String,
    filters: SearchFilters,
): List<SearchResult> {
    val q = query.trim().lowercase()
    return all.filter { r ->
        (filters.track == null || r.track == filters.track) &&
            (filters.level == null || r.level == filters.level) &&
            (filters.hasCode == null || r.hasCode == filters.hasCode) &&
            (filters.hasQuiz == null || r.hasQuiz == filters.hasQuiz) &&
            (q.isBlank() || r.hook.lowercase().contains(q) ||
                r.takeaway.lowercase().contains(q) || r.snippet.lowercase().contains(q) ||
                r.id.lowercase().contains(q))
    }
}

/**
 * Search field + filter chips (Spec v2, S7).
 *
 * This used to be a full-screen tab; it is now a section at the top of the
 * Path screen, so it deliberately renders no list of its own (a nested lazy
 * list inside the Path list would fight the parent scroller). PathScreen
 * renders [SearchResultRow] items lazily below this section.
 *
 * Query + filters are hoisted into [PathViewModel] so the text field and the
 * result set can never disagree.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchSection(
    query: String,
    filters: SearchFilters,
    onQueryChanged: (String, SearchFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun emit(
        track: String? = filters.track,
        level: Int? = filters.level,
        hasCode: Boolean? = filters.hasCode,
        hasQuiz: Boolean? = filters.hasQuiz,
    ) = onQueryChanged(query, SearchFilters(track, level, hasCode, hasQuiz))

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChanged(it, filters) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Search reels, traps, quizzes…") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Track filter
            FilterChip(
                selected = filters.track == null,
                onClick = { emit(track = null) },
                label = { Text("All tracks") },
            )
            listOf(Tracks.RUST, Tracks.SYSTEM_DESIGN).forEach { t ->
                FilterChip(
                    selected = filters.track == t,
                    onClick = { emit(track = if (filters.track == t) null else t) },
                    label = { Text(Tracks.label(t)) },
                )
            }
            // Level filter
            (1..4).forEach { lv ->
                FilterChip(
                    selected = filters.level == lv,
                    onClick = { emit(level = if (filters.level == lv) null else lv) },
                    label = { Text("L$lv") },
                )
            }
            FilterChip(
                selected = filters.hasCode == true,
                onClick = { emit(hasCode = if (filters.hasCode == true) null else true) },
                label = { Text("Has code") },
            )
            FilterChip(
                selected = filters.hasQuiz == true,
                onClick = { emit(hasQuiz = if (filters.hasQuiz == true) null else true) },
                label = { Text("Has quiz") },
            )
        }
    }
}

/**
 * One search hit. Rendered as a lazy item by PathScreen so results and the
 * roadmap share a single scroller.
 */
@Composable
fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = Tracks.label(result.track),
                    style = MaterialTheme.typography.labelMedium,
                    color = trackColor(result.track),
                )
                Text(
                    text = "L${result.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (result.hasCode) Text("</>", style = MaterialTheme.typography.labelMedium)
                if (result.hasQuiz) Text("?", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(text = result.hook, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = result.takeaway,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun previewResults() = listOf(
    SearchResult("rust-own-014", Tracks.RUST, 1, "Why does this function not compile?", "Move by default.", true, true),
    SearchResult("sys-cache-007", Tracks.SYSTEM_DESIGN, 2, "Cache-aside done right", "Invalidate on write.", false, true),
)

@Preview(name = "Search section dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SearchSectionPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchSection(
                query = "borrow",
                filters = SearchFilters(track = Tracks.RUST, hasCode = true),
                onQueryChanged = { _, _ -> },
            )
            SearchResultRow(result = previewResults().first(), onClick = {})
        }
    }
}
