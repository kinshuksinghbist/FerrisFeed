package com.ferrisfeed.path

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferrisfeed.coreui.AnimatedCounter
import com.ferrisfeed.coreui.ConfettiBurst
import com.ferrisfeed.coreui.DisplayFont
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisButtonStyle
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.LocalReduceMotion
import com.ferrisfeed.coreui.PathNodeSkeleton
import com.ferrisfeed.coreui.ProgressRing
import com.ferrisfeed.coreui.glass
import com.ferrisfeed.coreui.glassStroke
import com.ferrisfeed.coreui.pressScale
import com.ferrisfeed.coreui.staggeredEntrance
import com.ferrisfeed.coreui.trackBrush

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
 * Roadmap tab (Spec v2 S7 + P6 Item 40 journey overhaul).
 * One [LazyColumn] owns everything: header parallax, continue hero, search pill + filters,
 * and the continuous vertical journey roadmap.
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
    val isDark = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // Parallax header offset (Item 40a)
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 400
        }
    }

    // Continue target: highest mastery that is < 0.8 (Item 40b bug fix)
    val continueNode = remember(nodes) {
        val byId = nodes.associateBy { it.id }
        val unlocked = nodes.filter { node ->
            node.requires.isEmpty() || node.requires.all { prereqId -> (byId[prereqId]?.mastery ?: 0f) >= 0.6f }
        }
        val inProgress = unlocked.filter { it.mastery in 0.01f..0.799f }
        if (inProgress.isNotEmpty()) {
            inProgress.maxByOrNull { it.mastery }
        } else {
            unlocked.firstOrNull { it.mastery < 0.8f } ?: nodes.firstOrNull()
        }
    }

    val overall = if (nodes.isEmpty()) 0f else nodes.map { it.mastery }.average().toFloat()
    val allMastered = nodes.isNotEmpty() && nodes.all { it.mastery >= 0.8f }
    var confettiTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(allMastered) {
        if (allMastered) confettiTrigger++
    }

    // Locked tap shake feedback (Item 40d)
    var shakingLockedId by remember { mutableStateOf<String?>(null) }
    val lockedShakeX = remember { Animatable(0f) }
    LaunchedEffect(shakingLockedId) {
        if (shakingLockedId != null && !reduceMotion) {
            lockedShakeX.snapTo(0f)
            lockedShakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 240
                    -6f at 40
                    6f at 90
                    -4f at 140
                    4f at 190
                    0f at 240
                },
            )
            shakingLockedId = null
        }
    }

    val columns = remember(nodes) { layoutByDepth(nodes) }
    val isSearchingOrFiltering = hasActiveBrowse(state.query, state.filters)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 56.dp, // below stat bar
                end = 16.dp,
                bottom = LocalBottomBarInset.current + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: FerrisMark (48dp) + "Your path" in displaySmall + parallax (Item 40a)
            item(key = "header") {
                val headerAlpha = (1f - (scrollOffset / 200f)).coerceIn(0f, 1f)
                val headerTranslationY = scrollOffset * 0.4f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationY = headerTranslationY
                        }
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FerrisMark(size = 48.dp, animated = !reduceMotion)
                    Column {
                        Text(
                            text = "Your path",
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = DisplayFont,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Pick up where you left off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Continue hero card: full-width card with ProgressRing (72dp) + Resume button (Item 40b)
            if (!isSearchingOrFiltering && continueNode != null) {
                item(key = "continue-hero") {
                    val heroShape = MaterialTheme.shapes.large
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0)
                            .clip(heroShape)
                            .background(trackBrush(continueNode.track, isDark), heroShape)
                            .border(0.5.dp, glassStroke(), heroShape)
                            .padding(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // 72dp ProgressRing (Item 40b)
                                ProgressRing(
                                    progress = continueNode.mastery,
                                    size = 72.dp,
                                    strokeWidth = 8.dp,
                                    label = {
                                        AnimatedCounter(
                                            value = (continueNode.mastery * 100).toInt(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = DisplayFont,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CONTINUE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = DisplayFont,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.2.sp,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = continueNode.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = DisplayFont,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${continueNode.reelCount} reels · L${continueNode.level}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                FerrisButton(
                                    text = "Resume",
                                    style = FerrisButtonStyle.Filled,
                                    onClick = { onTopicClick(continueNode) },
                                )
                            }

                            // Overall mastery bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Overall progress",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "${(overall * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = DisplayFont,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(overall.coerceIn(0.01f, 1f))
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!isSearchingOrFiltering && allMastered) {
                item(key = "all-caught-up") {
                    val heroShape = MaterialTheme.shapes.large
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(heroShape)
                            .glass(heroShape)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "All caught up! 🎉",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = DisplayFont,
                                color = FerrisColors.MintCorrect,
                            )
                            Text(
                                text = "Every topic on this path is mastered.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Search section: pill search field + collapsed chip row (Item 40e)
            item(key = "search-section") {
                SearchSection(
                    query = state.query,
                    filters = state.filters,
                    topics = state.nodes
                        .filter { state.filters.track == null || it.track == state.filters.track }
                        .map { it.id },
                    onQueryChanged = onQueryChanged,
                )
            }

            // Results branch when active browse / search
            if (isSearchingOrFiltering) {
                val resultsCaption: String =
                    if (state.isSearching) {
                        "Searching…"
                    } else if (state.results.isEmpty()) {
                        "Nothing matches those filters"
                    } else if (state.filters.topic != null) {
                        "${state.results.size} reels in ${state.filters.topic.displayTopic()}"
                    } else {
                        "${state.results.size} results"
                    }

                item(key = "results-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = resultsCaption,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DisplayFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.results.isNotEmpty()) {
                            AnimatedCounter(
                                value = state.results.size,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = DisplayFont,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                if (state.results.isEmpty() && !state.isSearching) {
                    // Empty search state (Item 40h)
                    item(key = "empty-search") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                FerrisMark(size = 56.dp, animated = false)
                                Text(
                                    text = "Nothing matches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = DisplayFont,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Try clearing a filter or searching for another term.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FerrisButton(
                                    text = "Clear filters",
                                    style = FerrisButtonStyle.Tonal,
                                    onClick = { onQueryChanged("", state.filters.cleared()) },
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = state.results,
                        key = { _, result -> "result-${result.id}" },
                    ) { idx, result ->
                        SearchResultRow(
                            result = result,
                            onClick = { onResultClick(result) },
                            index = idx,
                        )
                    }
                }
            } else {
                // Skeletons during loading (Item 40h)
                if (state.isLoading) {
                    item(key = "loading-skeletons") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(5) {
                                PathNodeSkeleton()
                            }
                        }
                    }
                }

                // Vertical Journey Roadmap Timeline (Item 40c & 40d)
                val byId = remember(nodes) { nodes.associateBy { it.id } }

                columns.forEachIndexed { stageIndex, stageNodes ->
                    // Stage divider header (Item 40c)
                    item(key = "stage-header-$stageIndex") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "STAGE ${stageIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = DisplayFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            )
                        }
                    }

                    stageNodes.forEachIndexed { nodeIndexInStage, node ->
                        item(key = "node-${node.id}") {
                            val prereqMastery = node.requires.map { byId[it]?.mastery ?: 0f }
                            val isUnlocked = node.requires.isEmpty() || prereqMastery.all { it >= 0.6f }
                            val isCurrent = node.id == continueNode?.id
                            val isShaking = node.id == shakingLockedId
                            val prereqTitle = node.requires.firstOrNull()?.let { byId[it]?.title } ?: "prerequisites"

                            // Timeline item with continuous 3dp vertical line at x = 28dp (Item 40c)
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        val lineX = 28.dp.toPx()
                                        val nodeCenterY = size.height / 2f
                                        val ringRadius = 28.dp.toPx()

                                        val topEnd = nodeCenterY - ringRadius
                                        val bottomStart = nodeCenterY + ringRadius

                                        // Top segment
                                        if (topEnd > 0) {
                                            drawLine(
                                                color = if (isUnlocked) primaryColor else outlineColor,
                                                start = Offset(lineX, 0f),
                                                end = Offset(lineX, topEnd),
                                                strokeWidth = 3.dp.toPx(),
                                                pathEffect = if (!isUnlocked) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null,
                                            )
                                        }

                                        // Bottom segment
                                        if (bottomStart < size.height) {
                                            drawLine(
                                                color = if (isUnlocked) primaryColor else outlineColor,
                                                start = Offset(lineX, bottomStart),
                                                end = Offset(lineX, size.height),
                                                strokeWidth = 3.dp.toPx(),
                                                pathEffect = if (!isUnlocked) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null,
                                            )
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                // 56dp Ring node on the line (Item 40c)
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isCurrent && !reduceMotion) {
                                        // Pulsing halo for current continue node (Item 40c)
                                        val infiniteTransition = rememberInfiniteTransition(label = "halo")
                                        val haloScale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.18f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200),
                                                repeatMode = RepeatMode.Reverse,
                                            ),
                                            label = "halo-scale",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .graphicsLayer {
                                                    scaleX = haloScale
                                                    scaleY = haloScale
                                                }
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = 0.15f)),
                                        )
                                    }

                                    val ringLabel = if (node.mastery > 0f) {
                                        "${(node.mastery * 100).toInt()}%"
                                    } else {
                                        node.title.firstOrNull()?.uppercase() ?: "•"
                                    }
                                    ProgressRing(
                                        progress = node.mastery,
                                        size = 56.dp,
                                        strokeWidth = 6.dp,
                                        label = ringLabel,
                                    )
                                }

                                    // Node Card (Item 40d)
                                    val nodeShape = MaterialTheme.shapes.medium
                                    val interactionSource = remember { MutableInteractionSource() }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                if (isShaking) {
                                                    translationX = lockedShakeX.value * density
                                                }
                                            }
                                            .staggeredEntrance(nodeIndexInStage)
                                            .pressScale(interactionSource, pressed = 0.97f)
                                            .clip(nodeShape)
                                            .then(
                                                if (isUnlocked) Modifier.background(trackBrush(node.track, isDark), nodeShape)
                                                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), nodeShape)
                                            )
                                            .border(0.5.dp, glassStroke(), nodeShape)
                                            .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = {
                                                if (isUnlocked) {
                                                    onTopicClick(node)
                                                } else {
                                                    // Locked tap shake feedback (Item 40d)
                                                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                                    shakingLockedId = node.id
                                                }
                                            },
                                        )
                                        .defaultMinSize(minHeight = 64.dp)
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = node.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = DisplayFont,
                                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (isUnlocked) "${node.reelCount} reels · L${node.level}"
                                                else "Master $prereqTitle first",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            )
                                        }

                                        // Status chip (Item 40d: Locked / Mastered / n%)
                                        val statusBg = when {
                                            !isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            node.mastery >= 0.8f -> FerrisColors.MintCorrect.copy(alpha = 0.16f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        }
                                        val statusColor = when {
                                            !isUnlocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                            node.mastery >= 0.8f -> FerrisColors.MintCorrect
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(statusBg)
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                when {
                                                    !isUnlocked -> {
                                                        Icon(
                                                            imageVector = Icons.Filled.Lock,
                                                            contentDescription = "Locked",
                                                            tint = statusColor,
                                                            modifier = Modifier.size(13.dp),
                                                        )
                                                        Text(
                                                            text = "Locked",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = DisplayFont,
                                                            color = statusColor,
                                                        )
                                                    }
                                                    node.mastery >= 0.8f -> {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Mastered",
                                                            tint = statusColor,
                                                            modifier = Modifier.size(13.dp),
                                                        )
                                                        Text(
                                                            text = "Mastered",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = DisplayFont,
                                                            color = statusColor,
                                                        )
                                                    }
                                                    else -> {
                                                        Text(
                                                            text = "${(node.mastery * 100).toInt()}%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = DisplayFont,
                                                            fontWeight = FontWeight.Bold,
                                                            color = statusColor,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confetti burst for all-mastered celebration (Item 40b)
        ConfettiBurst(
            trigger = confettiTrigger,
            modifier = Modifier.fillMaxSize(),
        )
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

@Preview(name = "Path screen dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun PathScreenPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        PathScreen(
            state = PathUiState(
                nodes = listOf(
                    PathNode("toolchain", "Toolchain", "rust", 1, 0.9f, 5),
                    PathNode("variables", "Variables", "rust", 1, 0.45f, 5, listOf("toolchain")),
                    PathNode("ownership", "Ownership", "rust", 1, 0f, 5, listOf("variables")),
                ),
            ),
            onQueryChanged = { _, _ -> },
            onResultClick = {},
            onTopicClick = {},
        )
    }
}

@Preview(name = "Path screen light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun PathScreenPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        PathScreen(
            state = PathUiState(
                nodes = listOf(
                    PathNode("toolchain", "Toolchain", "rust", 1, 0.9f, 5),
                    PathNode("variables", "Variables", "rust", 1, 0.45f, 5, listOf("toolchain")),
                    PathNode("ownership", "Ownership", "rust", 1, 0f, 5, listOf("variables")),
                ),
            ),
            onQueryChanged = { _, _ -> },
            onResultClick = {},
            onTopicClick = {},
        )
    }
}
