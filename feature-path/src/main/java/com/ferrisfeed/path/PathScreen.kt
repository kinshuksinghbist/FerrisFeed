package com.ferrisfeed.path

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** DAG from TODO item 32: Ownership -> Lifetimes -> Async -> Axum -> Rate Limiter. */
fun defaultPathNodes(): List<PathNode> = listOf(
    PathNode("ownership", "Ownership & Borrowing", Tracks.RUST, 1, 0.62f, 45),
    PathNode("lifetimes", "Lifetimes", Tracks.RUST, 2, 0.34f, 22, requires = listOf("ownership")),
    PathNode("async", "Async & Tokio", Tracks.RUST, 2, 0.18f, 35, requires = listOf("lifetimes")),
    PathNode("axum", "Axum Services", Tracks.SYSTEM_DESIGN, 3, 0.05f, 30, requires = listOf("async")),
    PathNode("rate-limit", "Rate Limiter Blueprint", Tracks.SYSTEM_DESIGN, 3, 0f, 8, requires = listOf("axum")),
    PathNode("wasm-bindgen", "wasm-bindgen Hello", Tracks.WASM, 1, 0.71f, 25),
    PathNode("wasm-dom", "DOM + Canvas", Tracks.WASM, 2, 0.22f, 30, requires = listOf("wasm-bindgen")),
    PathNode("hashing", "Consistent Hashing", Tracks.SYSTEM_DESIGN, 2, 0.48f, 12),
    PathNode("raft", "Raft in 60s", Tracks.SYSTEM_DESIGN, 3, 0.1f, 10, requires = listOf("hashing")),
)

/**
 * Roadmap graph UI. Nodes are grouped into columns by DAG depth; edges are drawn
 * on a [Canvas] behind the nodes. Mastery lights the node: dim outline when locked
 * (< all prereqs started), track color when in progress, mint ring when >= 80%.
 */
@Composable
fun PathScreen(
    nodes: List<PathNode>,
    onNodeClick: (PathNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = remember(nodes) { layoutByDepth(nodes) }
    // Node center positions are approximated from column/row for edge drawing.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
        Spacer(Modifier.height(8.dp))

        // Overall resume bar
        val overall = if (nodes.isEmpty()) 0f else nodes.map { it.mastery }.average().toFloat()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Continue ${nodes.maxByOrNull { it.mastery }?.title ?: "—"} ${(overall * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
        }
        LinearProgressIndicator(
            progress = { overall },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Spacer(Modifier.height(16.dp))

        columns.forEachIndexed { depth, col ->
            DepthColumn(
                depth = depth,
                nodes = col,
                all = nodes,
                onNodeClick = onNodeClick,
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .then(
                        Modifier.let {
                            it // keep chain readable
                        },
                    ),
            ) {
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

@Preview(name = "Path dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun PathScreenPreview() {
    FerrisFeedTheme(darkTheme = true) {
        PathScreen(nodes = defaultPathNodes(), onNodeClick = {})
    }
}

@Preview(name = "Path light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun PathScreenLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        PathScreen(nodes = defaultPathNodes(), onNodeClick = {})
    }
}
