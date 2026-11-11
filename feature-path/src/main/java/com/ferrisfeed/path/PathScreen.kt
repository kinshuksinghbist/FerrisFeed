package com.ferrisfeed.path

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.FerrisTheme
import com.ferrisfeed.coreui.LinearMeter
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.NumberStyle
import com.ferrisfeed.coreui.Overline
import com.ferrisfeed.coreui.ProgressRing
import com.ferrisfeed.coreui.SegmentedControl
import com.ferrisfeed.coreui.SkeletonBlock
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.coreui.trackSoftColor
import com.ferrisfeed.coreui.trackTextColor
import com.ferrisfeed.data.Curriculum

/**
 * Path tab (v3).
 *
 * The old roadmap listed ~70 positional "topics" (several named "misc") in a
 * flat column. Now the curriculum reads like a syllabus: pick a track, see
 * one clear "Up next", then numbered units, each a short timeline of real
 * topics with lesson counts and mastery. Search lives behind the header
 * button so the page stays about progress, not filters.
 *
 * Single [LazyColumn] owns all scrolling (learnings §21).
 */
@Composable
fun PathScreen(
    state: PathUiState,
    onSelectTrack: (String) -> Unit,
    onOpenTopic: (TopicUi) -> Unit,
    onSearchOpen: (Boolean) -> Unit,
    onQueryChanged: (String, SearchFilters) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (state.searchOpen) {
            SearchMode(state, onSearchOpen, onQueryChanged, onResultClick)
        } else {
            CurriculumMode(state, onSelectTrack, onOpenTopic, onSearchOpen)
        }
    }
}

@Composable
private fun CurriculumMode(
    state: PathUiState,
    onSelectTrack: (String) -> Unit,
    onOpenTopic: (TopicUi) -> Unit,
    onSearchOpen: (Boolean) -> Unit,
) {
    val tracks = listOf(Tracks.RUST, Tracks.SYSTEM_DESIGN)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomBarInset.current + 32.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Path",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                FerrisIconButton(icon = Icons.Filled.Search, contentDescription = "Search lessons", onClick = { onSearchOpen(true) })
            }
        }
        item(key = "tracks") {
            SegmentedControl(
                options = tracks.map { Tracks.label(it) },
                selectedIndex = tracks.indexOf(state.track).coerceAtLeast(0),
                onSelect = { onSelectTrack(tracks[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (!state.isLoading) {
                Text(
                    text = "${state.startedCount} of ${state.topicCount} topics started · ${state.masteredCount} mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.isLoading) {
            item(key = "loading") { PathSkeleton() }
            return@LazyColumn
        }

        item(key = "up-next") {
            val next = state.upNext
            if (next != null) {
                UpNextCard(topic = next, unit = state.upNextUnit, onStart = { onOpenTopic(next) })
            } else {
                CaughtUpCard(track = state.track)
            }
            Spacer(Modifier.height(28.dp))
        }

        state.units.forEach { unit ->
            item(key = "unit-${unit.id}") { UnitHeader(unit = unit, track = state.track) }
            itemsIndexed(unit.topics, key = { _, t -> "topic-${t.key}" }) { idx, topic ->
                TopicRow(
                    topic = topic,
                    number = idx + 1,
                    isFirst = idx == 0,
                    isLast = idx == unit.topics.lastIndex,
                    onClick = { onOpenTopic(topic) },
                )
            }
            item(key = "gap-${unit.id}") { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun UpNextCard(topic: TopicUi, unit: UnitUi?, onStart: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(trackSoftColor(topic.track), shape)
            .padding(20.dp),
    ) {
        Overline(
            text = if (unit != null) "Up next · Unit ${unit.number}" else "Up next",
            color = trackTextColor(topic.track),
        )
        Spacer(Modifier.height(8.dp))
        Text(topic.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(topic.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearMeter(
                progress = if (topic.reelCount == 0) 0f else topic.seenCount.toFloat() / topic.reelCount,
                color = trackColor(topic.track),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${topic.seenCount}/${topic.reelCount} lessons",
                style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        FerrisButton(
            text = if (topic.seenCount == 0) "Start topic" else "Continue",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CaughtUpCard(track: String) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FerrisTheme.palette.successSoft, shape)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FerrisMark(size = 48.dp, animated = false)
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Every ${Tracks.label(track)} lesson done", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Reviews keep arriving in Learn as they come due.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnitHeader(unit: UnitUi, track: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Unit ${unit.number}", color = trackTextColor(track), modifier = Modifier.weight(1f))
            Text(
                "${(unit.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            unit.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(unit.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        LinearMeter(progress = unit.progress, color = trackColor(track), height = 4.dp, modifier = Modifier.fillMaxWidth())
    }
}

/** Timeline row: status node on a rail, title + blurb, lesson count. */
@Composable
private fun TopicRow(
    topic: TopicUi,
    number: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val palette = FerrisTheme.palette
    val accent = trackColor(topic.track)
    val statusText = when (topic.status) {
        TopicStatus.Mastered -> "mastered"
        TopicStatus.InProgress -> "${topic.seenCount} of ${topic.reelCount} lessons done"
        TopicStatus.New -> "not started"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${topic.title}, ${topic.reelCount} lessons, $statusText"
            },
    ) {
        // Rail + node
        Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isFirst) Color.Transparent else palette.hairline),
                )
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isLast) Color.Transparent else palette.hairline),
                )
            }
            StatusNode(topic = topic, number = number, accent = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 68.dp)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${topic.reelCount} lessons · ${topic.blurb}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp),
        )
    }
}

@Composable
private fun StatusNode(topic: TopicUi, number: Int, accent: Color) {
    val palette = FerrisTheme.palette
    val bg = MaterialTheme.colorScheme.background
    when (topic.status) {
        TopicStatus.Mastered -> Box(
            Modifier.size(32.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = bg, modifier = Modifier.size(18.dp))
        }
        TopicStatus.InProgress -> Box(Modifier.size(32.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (topic.reelCount == 0) 0f else topic.seenCount.toFloat() / topic.reelCount,
                size = 32.dp,
                strokeWidth = 3.dp,
                color = accent,
            ) {
                Text("$number", style = MaterialTheme.typography.labelMedium.merge(NumberStyle), color = MaterialTheme.colorScheme.onBackground)
            }
        }
        TopicStatus.New -> Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bg)
                .border(1.5.dp, palette.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium.merge(NumberStyle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchMode(
    state: PathUiState,
    onSearchOpen: (Boolean) -> Unit,
    onQueryChanged: (String, SearchFilters) -> Unit,
    onResultClick: (SearchResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = LocalBottomBarInset.current + 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "field") {
            SearchField(
                query = state.query,
                onQueryChange = { onQueryChanged(it, state.filters) },
                onClose = { onSearchOpen(false) },
            )
        }
        item(key = "filters") {
            SearchFilterRow(filters = state.filters, onFiltersChange = { onQueryChanged(state.query, it) })
        }
        val active = hasActiveBrowse(state.query, state.filters)
        if (!active) {
            item(key = "hint") {
                Text(
                    "Search every lesson by keyword, or pick a track and level to browse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        } else if (!state.isSearching && state.results.isEmpty()) {
            item(key = "empty") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FerrisMark(size = 56.dp, animated = false)
                    Spacer(Modifier.height(12.dp))
                    Text("No lessons match", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "Try a shorter word or clear a filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            item(key = "count") {
                Text(
                    text = if (state.isSearching && state.results.isEmpty()) "Searching…" else "${state.results.size} lessons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(state.results, key = { it.id }) { result ->
                SearchResultRow(result = result, onClick = { onResultClick(result) })
            }
        }
    }
}

@Composable
private fun PathSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(190.dp), RoundedCornerShape(20.dp))
        Spacer(Modifier.height(8.dp))
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(32.dp), CircleShape)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBlock(Modifier.width(180.dp).height(16.dp))
                    SkeletonBlock(Modifier.width(120.dp).height(12.dp))
                }
            }
        }
    }
}

private fun previewState(): PathUiState {
    fun t(key: String, seen: Int, total: Int, m: Float) = TopicUi(
        key = key,
        title = Curriculum.titleOf(key),
        blurb = Curriculum.topic(key)?.blurb.orEmpty(),
        track = Tracks.RUST,
        reelCount = total,
        seenCount = seen,
        mastery = m,
    )
    val u1 = UnitUi("rust-1", 1, "Getting started", "Install, build, and write your first real Rust.", listOf(
        t("cargo-toolchain", 11, 11, 0.9f), t("variables-types", 5, 12, 0.3f), t("control-flow", 0, 7, 0f),
    ))
    val u2 = UnitUi("rust-2", 2, "Ownership & borrowing", "The one idea that makes Rust Rust.", listOf(
        t("ownership", 0, 6, 0f), t("borrowing", 0, 6, 0f),
    ))
    return PathUiState(units = listOf(u1, u2), upNext = u1.topics[1], upNextUnit = u1, isLoading = false)
}

@Preview(name = "Path · dark", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 900)
@Composable
private fun PathPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        PathScreen(previewState(), {}, {}, {}, { _, _ -> }, {})
    }
}

@Preview(name = "Path · light", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360, heightDp = 900)
@Composable
private fun PathPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        PathScreen(previewState(), {}, {}, {}, { _, _ -> }, {})
    }
}
