package com.ferrisfeed.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.FerrisSurface
import com.ferrisfeed.coreui.FerrisTheme
import com.ferrisfeed.coreui.LinearMeter
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.NumberStyle
import com.ferrisfeed.coreui.Overline
import com.ferrisfeed.coreui.SegmentedControl
import com.ferrisfeed.coreui.StatCell
import com.ferrisfeed.coreui.TrackTag
import com.ferrisfeed.coreui.Tracks
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.data.LearnerProfile
import com.ferrisfeed.feed.FeedViewModel
import com.ferrisfeed.path.FerrisStage

private val FocusOptions = listOf(Tracks.RUST, LearnerProfile.FOCUS_BOTH, Tracks.SYSTEM_DESIGN)
private val DayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * You tab (v3). Replaces the old profile hub, whose headline feature was a
 * switch between two competing visual "philosophies". Now it answers the
 * learner's questions: how am I doing, what am I good at, what should I
 * practise, where are my saved lessons, and what am I learning.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onOpenLesson: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onChangeFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomBarInset.current + 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title") {
            Text(
                "You",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 14.dp).semantics { heading() },
            )
        }
        item(key = "rank") { RankCard(state) }
        item(key = "week") { WeekCard(state) }
        item(key = "stats") {
            FerrisSurface(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp)) {
                    StatCell(value = "${state.lessonsDone}", label = "lessons done")
                    StatCell(value = state.accuracy?.let { "$it%" } ?: "–", label = "accuracy")
                    StatCell(value = "${state.quiz.total}", label = "checks taken")
                }
            }
        }

        if (state.strengths.isNotEmpty() || state.practice.isNotEmpty()) {
            item(key = "topics") {
                FerrisSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.strengths.isNotEmpty()) {
                            Overline("Strongest topics")
                            state.strengths.forEach { MasteryRow(it, onClick = { onOpenTopic(it.key) }) }
                        }
                        if (state.practice.isNotEmpty()) {
                            if (state.strengths.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            Overline("Worth practising")
                            state.practice.forEach { MasteryRow(it, onClick = { onOpenTopic(it.key) }) }
                        }
                    }
                }
            }
        }

        item(key = "saved-title") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Saved lessons",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.saved.isNotEmpty()) {
                    Text("${state.saved.size}", style = MaterialTheme.typography.labelLarge.merge(NumberStyle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.saved.isEmpty()) {
            item(key = "saved-empty") {
                Text(
                    "Tap the bookmark on any lesson, or double-tap the lesson, to keep it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.saved, key = { "saved-${it.id}" }) { lesson ->
                FerrisSurface(Modifier.fillMaxWidth(), onClick = { onOpenLesson(lesson.id) }) {
                    Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TrackTag(lesson.track, lesson.topicTitle)
                            Text(
                                lesson.hook,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FerrisIconButton(
                            icon = Icons.Filled.Bookmark,
                            contentDescription = "Remove from saved",
                            onClick = { onRemoveSaved(lesson.id) },
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item(key = "focus") {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Learning focus", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Shapes the order of new lessons in Learn. Everything stays available in Path.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedControl(
                    options = listOf("Rust", "Both", "System Design"),
                    selectedIndex = FocusOptions.indexOf(state.focus).coerceAtLeast(0),
                    onSelect = { onChangeFocus(FocusOptions[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RankCard(state: ProfileUiState) {
    val palette = FerrisTheme.palette
    val next = FerrisStage.entries.firstOrNull { it.minXp > state.xp }
    FerrisSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FerrisMark(size = 52.dp, animated = false)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Overline("Rank")
                    Text(state.stage.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${state.xp}", style = MaterialTheme.typography.headlineSmall.merge(NumberStyle), color = palette.xp)
                    Text("XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearMeter(progress = state.stageProgress, color = palette.xp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (next != null && state.xpToNext != null) "${state.xpToNext} XP to ${next.label}" else "Top rank reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekCard(state: ProfileUiState) {
    val palette = FerrisTheme.palette
    val goal = FeedViewModel.DAILY_GOAL_XP
    FerrisSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline("Streak")
                    Text(
                        text = when (state.streakDays) {
                            0 -> "Start a streak today"
                            1 -> "1 day"
                            else -> "${state.streakDays} days"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${state.todayXp.coerceAtMost(999)}/$goal",
                        style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                        color = if (state.todayXp >= goal) palette.successInk else MaterialTheme.colorScheme.onSurface,
                    )
                    Text("today's XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayLetters.forEachIndexed { i, letter ->
                    val active = state.weekActive.getOrElse(i) { false }
                    val isToday = i == state.todayIndex
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (active) palette.rust else palette.raised)
                                .border(if (isToday) 2.dp else 0.dp, if (isToday) MaterialTheme.colorScheme.onSurface else palette.raised, CircleShape)
                                .semantics { contentDescription = if (active) "Active" else "No activity" },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            letter,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Answer one quick check a day to keep the streak alive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MasteryRow(item: TopicMasteryItem, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(trackColor(item.track)))
            Spacer(Modifier.width(10.dp))
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(
                "${(item.score * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearMeter(progress = item.score, color = trackColor(item.track), height = 4.dp, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "You · dark", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 1200)
@Composable
private fun ProfilePreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(
                xp = 640,
                streakDays = 4,
                todayXp = 35,
                weekActive = listOf(true, true, false, true, true, false, false),
                todayIndex = 4,
                lessonsDone = 42,
                strengths = listOf(TopicMasteryItem("ownership", "Ownership & moves", Tracks.RUST, 0.82f)),
                practice = listOf(TopicMasteryItem("caching", "Caching", Tracks.SYSTEM_DESIGN, 0.21f)),
                saved = listOf(SavedLesson("1", Tracks.RUST, "Lifetimes", "Three elision rules explain most lifetime errors.")),
                isLoading = false,
            ),
            onOpenLesson = {},
            onOpenTopic = {},
            onRemoveSaved = {},
            onChangeFocus = {},
        )
    }
}
