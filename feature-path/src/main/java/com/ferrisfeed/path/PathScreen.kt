package com.ferrisfeed.path

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.ProgressRing
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor

/** Single roadmap node. [requires] lists topic ids that must be started first (DAG edges). */
data class PathNode(
    val id: String,
    val title: String,
    val track: String,
    val level: Int,
    val mastery: Float, // 0..1, decays with skips (computed in rust-core / data layer)
    val reelCount: Int,
    val requires: List<String> = emptyList(),
)

/**
 * Roadmap tab (Spec v2, S7).
 *
 * One [LazyColumn] owns everything: the search section on top (query + filter
 * chips, with results rendered as lazy items right below it), then the live
 * roadmap. Nothing here nests a scroller, so results and the DAG share one
 * fling without fighting.
 *
 * Nodes come from [PathUiState.nodes] (real Room topics + persisted mastery —
 * see [PathViewModel]); [previewPathNodes] exists only for @Preview.
 * Mastery lights a node: track color in progress, mint ring when >= 80%,
 * dimmed and non-clickable while every prerequisite is still unstarted.
 */
@Composable
fun PathScreen(
    state: PathUiState,
    onQueryChanged: (String, SearchFilters) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onTopicClick: (PathNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nodes = state.nodes
    val columns = remember(nodes) { layoutByDepth(nodes) }
    val overall = if (nodes.isEmpty()) 0f else nodes.map { it.mastery }.average().toFloat()
    val continueNode = nodes.maxByOrNull { it.mastery }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "Your path",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Nodes light up as you master prerequisites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "resume") {
            // Resume bar: overall mastery + the node most worth continuing.
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Continue ${continueNode?.title ?: "—"} ${(overall * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { overall },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        item(key = "search") {
            // TODO 22: the browse chip row is fed from the same roadmap topics
            // the nodes render, so a chip never leads to an empty shelf.
            SearchSection(
                query = state.query,
                filters = state.filters,
                topics = state.nodes
                    .filter { state.filters.track == null || it.track == state.filters.track }
                    .map { it.id },
                onQueryChanged = onQueryChanged,
            )
        }

        if (hasActiveBrowse(state.query, state.filters)) {
            item(key = "results-header") {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            state.isSearching -> "Searching…"
                            // TODO 22 (UX-writing): say why the list is empty
                            // and what narrows it — never a bare “0 results”.
                            state.results.isEmpty() -> "Nothing matches those filters yet — clear one to widen the net."
                            state.filters.topic != null -> "${state.results.size} reels in ${state.filters.topic.displayTopic()}"
                            else -> "${state.results.size} results",
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(items = state.results, key = { result -> "result-${result.id}" }) { result ->
                SearchResultRow(result = result, onClick = { onResultClick(result) })
            }
        } else {
            // TODO 22 resting state: topic-first directory instead of a list
            // that starts blank. Reuses roadmap facts (count + min level) —
            // zero extra queries; counts appear as the user narrows.
            item(key = "topic-directory") {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Browse by topic",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pick a topic to browse its reels, or search above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    nodes
                        .filter { state.filters.track == null || it.track == state.filters.track }
                        .forEach { node ->
                            BrowseByTopicRow(
                                title = node.title,
                                track = node.track,
                                caption = "${node.reelCount} reels · entry level L${node.level} · ${node.mastery.toIntPercent()} mastered",
                                locked = node.reelCount == 0,
                                onClick = { onTopicClick(node) },
                            )
                        }
                }
            }
        }

        if (state.isLoading) {
            item(key = "loading") {
                Text(
                    text = "Loading your path…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }

        item(key = "roadmap-spacer") { Spacer(Modifier.height(12.dp)) }

        itemsIndexed(items = columns, key = { depth, _ -> "stage-$depth" }) { depth, col ->
            DepthColumn(
                depth = depth,
                nodes = col,
                all = nodes,
                onNodeClick = onTopicClick,
            )
            if (depth < columns.lastIndex) {
                ConnectorLine()
            }
        }
    }
}

private fun layoutByDepth(nodes: List<PathNode>): List<List<PathNode>> {
    val byId = nodes.associateBy { it.id }
    val depthMemo = mutableMapOf<String, Int>()
    fun depth(n: PathNode): Int {
        depthMemo[n.id]?.let { return it }
        val d = if (n.requires.isEmpty()) 0
        else (n.requires.map { byId[it]?.let(::depth) ?: 0 }.maxOrNull() ?: 0) + 1
        depthMemo[n.id] = d
        return d
    }
    return nodes.groupBy(::depth).toSortedMap().values.toList()
}

@Composable
private fun DepthColumn(
    depth: Int,
    nodes: List<PathNode>,
    all: List<PathNode>,
    onNodeClick: (PathNode) -> Unit,
) {
    val byId = all.associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Stage ${depth + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        nodes.forEach { node ->
            val prereqMastery = node.requires.map { byId[it]?.mastery ?: 0f }
            val locked = node.requires.isNotEmpty() && (prereqMastery.isEmpty() || prereqMastery.all { it <= 0f })
            PathNodeRow(node = node, locked = locked, onClick = { if (!locked) onNodeClick(node) })
        }
    }
}

@Composable
private fun PathNodeRow(
    node: PathNode,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val accent = trackColor(node.track)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (locked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (locked) 0.dp else 2.dp,
        border = if (!locked) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(progress = node.mastery)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${Tracks.label(node.track)} · L${node.level} · ${node.reelCount} reels" +
                        if (locked) " · Locked" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Status dot
            Box(modifier = Modifier.size(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = when {
                            locked -> Color.Gray.copy(alpha = 0.4f)
                            node.mastery >= 0.8f -> FerrisColors.MintCorrect
                            else -> accent
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseByTopicRow(
    title: String,
    track: String,
    caption: String,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val accent = trackColor(track)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(8.dp)) {
                Canvas(Modifier.fillMaxSize()) { drawCircle(color = accent) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Float.toIntPercent(): Int = (this * 100).toInt().coerceIn(0, 100)

@Composable
private fun ConnectorLine() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        val x = size.width / 2
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
    }
}

/**
 * Sample roadmap for @Preview only. Production nodes are built from Room by
 * [PathViewModel]; the old `defaultPathNodes()` demo list is gone so a fresh
 * install honestly shows 0% everywhere instead of fake mastery.
 */
private fun previewPathNodes(): List<PathNode> = listOf(
    PathNode("ownership", "Ownership", Tracks.RUST, 1, 0.62f, 45),
    PathNode("lifetimes", "Lifetimes", Tracks.RUST, 2, 0.34f, 22, requires = listOf("ownership")),
    PathNode("async", "Async", Tracks.RUST, 2, 0.18f, 35, requires = listOf("lifetimes")),
    PathNode("axum", "Axum", Tracks.RUST, 3, 0.05f, 30, requires = listOf("async")),
    PathNode("rate-limiter", "Rate Limiter", Tracks.SYSTEM_DESIGN, 3, 0f, 8, requires = listOf("axum")),
)

private fun previewPathState(): PathUiState = PathUiState(
    nodes = previewPathNodes(),
    isLoading = false,
    query = "borrow",
    filters = SearchFilters(track = Tracks.RUST),
    results = listOf(
        SearchResult("rust-own-014", Tracks.RUST, 1, "ownership", "Why does this function not compile?", "Move by default.", true, true),
    ),
)

@Preview(name = "Path dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun PathScreenPreview() {
    FerrisFeedTheme(darkTheme = true) {
        PathScreen(
            state = previewPathState(),
            onQueryChanged = { _, _ -> },
            onResultClick = {},
            onTopicClick = {},
        )
    }
}

@Preview(name = "Path light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun PathScreenLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        PathScreen(
            state = previewPathState().copy(query = "", results = emptyList()),
            onQueryChanged = { _, _ -> },
            onResultClick = {},
            onTopicClick = {},
        )
    }
}
