package com.ferrisfeed.path

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.TrackPill
import com.ferrisfeed.coreui.Tracks

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
 * Full-text search UI. Backed by Room FTS (`reels_fts`) in :data; this composable owns
 * only query + filter state and calls [onQueryChanged] so the caller can re-query.
 * Filters: track / level / has-code / has-quiz (per TODO 34).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    results: List<SearchResult>,
    onQueryChanged: (String, SearchFilters) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var track by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableStateOf<Int?>(null) }
    var hasCode by remember { mutableStateOf<Boolean?>(null) }
    var hasQuiz by remember { mutableStateOf<Boolean?>(null) }

    fun emit() = onQueryChanged(query, SearchFilters(track, level, hasCode, hasQuiz))

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; emit() },
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
                selected = track == null,
                onClick = { track = null; emit() },
                label = { Text("All tracks") },
            )
            listOf(Tracks.RUST, Tracks.SYSTEM_DESIGN).forEach { t ->
                FilterChip(
                    selected = track == t,
                    onClick = { track = if (track == t) null else t; emit() },
                    label = { Text(Tracks.label(t)) },
                )
            }
            // Level filter
            (1..4).forEach { lv ->
                FilterChip(
                    selected = level == lv,
                    onClick = { level = if (level == lv) null else lv; emit() },
                    label = { Text("L$lv") },
                )
            }
            FilterChip(
                selected = hasCode == true,
                onClick = { hasCode = if (hasCode == true) null else true; emit() },
                label = { Text("Has code") },
            )
            FilterChip(
                selected = hasQuiz == true,
                onClick = { hasQuiz = if (hasQuiz == true) null else true; emit() },
                label = { Text("Has quiz") },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "${results.size} results",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { r ->
                SearchRow(result = r, onClick = { onResultClick(r) })
            }
        }
    }
}

@Composable
private fun SearchRow(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackPill(result.track)
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
    SearchResult("rust-own-014", Tracks.RUST, 1, "Why does this function not compile?", "Move by default; borrow to keep.", true, true),
    SearchResult("sys-cache-007", Tracks.SYSTEM_DESIGN, 2, "Cache-aside done right", "Invalidate on write.", false, true),
)

@Preview(name = "Search dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SearchScreenPreview() {
    FerrisFeedTheme(darkTheme = true) {
        SearchScreen(results = previewResults(), onQueryChanged = { _, _ -> }, onResultClick = {})
    }
}
