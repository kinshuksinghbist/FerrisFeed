package com.ferrisfeed.path

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferrisfeed.coreui.DisplayFont
import com.ferrisfeed.coreui.FerrisChip
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.glass
import com.ferrisfeed.coreui.glassStroke
import com.ferrisfeed.coreui.pressScale
import com.ferrisfeed.coreui.staggeredEntrance
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.coreui.trackTextColor

/**
 * Node titles: label chunks title-cased per word. Splits spaces plus the
 * legacy `_`/`-` keys so pre-21 packs caption identically to path rows
 * (same algorithm as prettyTopic in PathViewModel).
 */
internal fun String.displayTopic(): String = split(" ", "_", "-")
    .filter { it.isNotBlank() }
    .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/** Searchable reel summary (projection of the full reel; Room FTS returns these). */
data class SearchResult(
    val id: String,
    val track: String,
    val level: Int,
    /** Exact topic this reel belongs to — same stored string the roadmap keys on (TODO 21). */
    val topic: String,
    val hook: String,
    val takeaway: String,
    val hasCode: Boolean,
    val hasQuiz: Boolean,
    val snippet: String = "",
)

/**
 * Browse filters (TODO 22). null = no constraint anywhere (browse everything);
 * any set value narrows. [topic] is an exact-match on the same stored topic
 * string the path nodes and Route.TopicFeed use, so chip, roadmap node, and
 * topic feed all address the same object.
 */
data class SearchFilters(
    val track: String? = null, // null = all
    val level: Int? = null,
    val hasCode: Boolean? = null, // null = either
    val hasQuiz: Boolean? = null,
    val topic: String? = null, // null = all topics
) {
    /** True when at least one filter is set (an empty query alone is not). */
    fun isScoped(): Boolean =
        track != null || level != null || hasCode != null || hasQuiz != null || topic != null

    /** Copy with every filter cleared (query text untouched). */
    fun cleared(): SearchFilters = SearchFilters()
}

/**
 * Search field + filter chips (Spec v2, S7; TODO 40e redesign).
 * Pill search field + horizontally scrolling collapsed chip row with expandable filters panel.
 */
@Composable
fun SearchSection(
    query: String,
    filters: SearchFilters,
    topics: List<String>,
    onQueryChanged: (String, SearchFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showAdvancedFilters by remember { mutableStateOf(false) }

    fun emit(
        track: String? = filters.track,
        level: Int? = filters.level,
        hasCode: Boolean? = filters.hasCode,
        hasQuiz: Boolean? = filters.hasQuiz,
        topic: String? = filters.topic,
    ) = onQueryChanged(query, SearchFilters(track, level, hasCode, hasQuiz, topic))

    val advancedCount = (if (filters.level != null) 1 else 0) +
        (if (filters.hasCode != null) 1 else 0) +
        (if (filters.hasQuiz != null) 1 else 0) +
        (if (filters.topic != null) 1 else 0)

    val focusBorderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else glassStroke(),
        animationSpec = FerrisMotion.QuickColor,
        label = "search-focus-ring",
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Pill search field: 52dp height, CircleShape, .glass, search icon, clear button (Item 40e)
        val shape = CircleShape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .glass(shape)
                .border(if (isFocused) 1.5.dp else 0.5.dp, focusBorderColor, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search reels and topics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { onQueryChanged(it, filters) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(
                            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* Search committed */ }),
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChanged("", filters) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Horizontally scrolling single chip row with primary filters (Item 40e)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item(key = "track-all") {
                FerrisChip(
                    label = "All tracks",
                    selected = filters.track == null,
                    onClick = { emit(track = null, topic = null) },
                )
            }
            item(key = "track-rust") {
                FerrisChip(
                    label = Tracks.label(Tracks.RUST),
                    selected = filters.track == Tracks.RUST,
                    onClick = {
                        emit(
                            track = if (filters.track == Tracks.RUST) null else Tracks.RUST,
                            topic = null,
                        )
                    },
                    leadingDot = FerrisColors.FerrisOrange,
                )
            }
            item(key = "track-system-design") {
                FerrisChip(
                    label = Tracks.label(Tracks.SYSTEM_DESIGN),
                    selected = filters.track == Tracks.SYSTEM_DESIGN,
                    onClick = {
                        emit(
                            track = if (filters.track == Tracks.SYSTEM_DESIGN) null else Tracks.SYSTEM_DESIGN,
                            topic = null,
                        )
                    },
                    leadingDot = FerrisColors.SkyBlue,
                )
            }
            item(key = "advanced-filters-toggle") {
                val filtersLabel = if (advancedCount > 0) "Filters · $advancedCount" else "Filters"
                FerrisChip(
                    label = filtersLabel,
                    selected = showAdvancedFilters || advancedCount > 0,
                    onClick = { showAdvancedFilters = !showAdvancedFilters },
                )
            }
            if (filters.isScoped()) {
                item(key = "clear-filters") {
                    FerrisChip(
                        label = "Clear filters",
                        selected = false,
                        onClick = { onQueryChanged(query, filters.cleared()) },
                    )
                }
            }
        }

        // Collapsible advanced filters panel (Level, Has code, Has quiz, Topics)
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically(FerrisMotion.QuickOffset) + fadeIn(FerrisMotion.Quick),
            exit = shrinkVertically(FerrisMotion.QuickOffset) + fadeOut(FerrisMotion.Quick),
        ) {
            val panelShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .glass(panelShape)
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Level filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Level",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DisplayFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp),
                        )
                        (1..4).forEach { lv ->
                            FerrisChip(
                                label = "L$lv",
                                selected = filters.level == lv,
                                onClick = { emit(level = if (filters.level == lv) null else lv) },
                            )
                        }
                    }

                    // Content type filters
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DisplayFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp),
                        )
                        FerrisChip(
                            label = "Has code",
                            selected = filters.hasCode == true,
                            onClick = { emit(hasCode = if (filters.hasCode == true) null else true) },
                        )
                        FerrisChip(
                            label = "Has quiz",
                            selected = filters.hasQuiz == true,
                            onClick = { emit(hasQuiz = if (filters.hasQuiz == true) null else true) },
                        )
                    }

                    // Topic chips row
                    if (topics.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Topic",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DisplayFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (filters.track != null) {
                                    item(key = "topic-all") {
                                        FerrisChip(
                                            label = "All topics",
                                            selected = filters.topic == null,
                                            onClick = { emit(topic = null) },
                                        )
                                    }
                                }
                                items(topics, key = { it }) { t ->
                                    FerrisChip(
                                        label = t.displayTopic(),
                                        selected = filters.topic == t,
                                        onClick = { emit(topic = if (filters.topic == t) null else t) },
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

/**
 * One search hit (Item 40g overhaul).
 * Shape medium, glass, track chip, L{n}, topic, real 16dp icons for Code/Quiz, titleMedium hook,
 * bodyMedium takeaway.
 */
@Composable
fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(index)
            .pressScale(interactionSource, pressed = 0.97f)
            .clip(shape)
            .glass(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Track chip (capsule 12dp, track color 16% alpha fill, 6dp track dot, label, trackTextColor)
                val dotColor = trackColor(result.track)
                val tColor = trackTextColor(result.track)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(dotColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Text(
                            text = Tracks.label(result.track),
                            style = MaterialTheme.typography.labelMedium,
                            color = tColor,
                        )
                    }
                }

                // Level label
                Text(
                    text = "L${result.level}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Topic label
                Text(
                    text = result.topic.displayTopic(),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.weight(1f))

                // Trailing icons: 16dp real icons (Item 40g)
                if (result.hasCode) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Has code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (result.hasQuiz) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "Has quiz",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Text(
                text = result.hook,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = result.takeaway,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun previewResults() = listOf(
    SearchResult("rust-own-014", Tracks.RUST, 1, "ownership", "Why does this function not compile?", "Move by default.", true, true),
    SearchResult("sys-cache-007", Tracks.SYSTEM_DESIGN, 2, "concepts", "Cache-aside done right", "Invalidate on write.", false, true),
)

@Preview(name = "Search section dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SearchSectionPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchSection(
                query = "borrow",
                filters = SearchFilters(track = Tracks.RUST, hasCode = true),
                topics = listOf("toolchain", "ownership", "collections", "drills"),
                onQueryChanged = { _, _ -> },
            )
            SearchResultRow(result = previewResults().first(), onClick = {})
        }
    }
}
