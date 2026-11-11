package com.ferrisfeed.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.ConfettiBurst
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisButtonStyle
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.FerrisTheme
import com.ferrisfeed.coreui.FerrisType
import com.ferrisfeed.coreui.LessonContent
import com.ferrisfeed.coreui.LinearMeter
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.LocalReduceMotion
import com.ferrisfeed.coreui.NumberStyle
import com.ferrisfeed.coreui.Overline
import com.ferrisfeed.coreui.ProgressRing
import com.ferrisfeed.coreui.QuizContent
import com.ferrisfeed.coreui.QuizResult
import com.ferrisfeed.coreui.QuizUiModel
import com.ferrisfeed.coreui.SkeletonBlock
import com.ferrisfeed.coreui.StreakPill
import com.ferrisfeed.coreui.TrackTag
import com.ferrisfeed.coreui.XpPill
import com.ferrisfeed.coreui.trackTextColor
import com.ferrisfeed.data.Curriculum
import com.ferrisfeed.feed.FeedPaging.Page
import kotlinx.coroutines.launch

/**
 * The Learn feed (v3).
 *
 * A full-screen vertical pager played in sets of five: lesson → quiz for each
 * reel, then a recap page that closes the set (score, XP, streak, daily goal,
 * what's next). Pages scroll internally only when their content is taller
 * than the screen; otherwise every swipe goes straight to the pager, so short
 * lessons still advance with the smallest flick and long ones never clip.
 *
 * The top bar is owned here (not by the app shell) so there is exactly one:
 * streak · set progress · XP on Learn, and back · topic · progress when the
 * feed is scoped to a topic or a single lesson.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier,
    /** The scope this route asked for; until the shared view model serves it, show loading. */
    scope: FeedScope? = null,
    onBack: (() -> Unit)? = null,
    title: String? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val stale = scope != null && state.scope != scope
    val stats by viewModel.stats.collectAsState()
    val swipeHintSeen by viewModel.swipeHintSeen.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        when {
            state.isLoading || stale -> {
                FeedTopBar(onBack = onBack, title = title, stats = stats, progress = null)
                FeedLoading()
            }
            state.reels.isEmpty() -> {
                FeedTopBar(onBack = onBack, title = title, stats = stats, progress = null)
                EmptyFeed(onRetry = viewModel::retryLoad)
            }
            else -> key(state.queueVersion) {
                FeedPager(
                    state = state,
                    stats = stats,
                    swipeHintSeen = swipeHintSeen,
                    viewModel = viewModel,
                    onBack = onBack,
                    title = title,
                )
            }
        }
    }
}

@Composable
private fun FeedPager(
    state: FeedUiState,
    stats: FeedStats,
    swipeHintSeen: Boolean,
    viewModel: FeedViewModel,
    onBack: (() -> Unit)?,
    title: String?,
) {
    val reels = state.reels
    val reelCount = reels.size
    val pageCount = FeedPaging.pageCount(reelCount)
    val pagerState = rememberPagerState(
        initialPage = FeedPaging.lessonPageOf(state.currentIndex).coerceIn(0, maxOf(0, pageCount - 1)),
        pageCount = { pageCount },
    )
    LaunchedEffect(pagerState, reelCount) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onPageChanged(FeedPaging.reelOf(FeedPaging.pageAt(page, reelCount), reelCount))
        }
    }

    val currentPage = FeedPaging.pageAt(pagerState.currentPage.coerceIn(0, maxOf(0, pageCount - 1)), reelCount)
    val scoped = onBack != null
    val heading = title ?: Curriculum.titleOf(reels.first().topic)
    FeedTopBar(
        onBack = onBack,
        title = heading,
        stats = stats,
        progress = {
            if (scoped) {
                val reel = FeedPaging.reelOf(currentPage, reelCount)
                LinearMeter(
                    progress = (reel + 1f) / reelCount,
                    modifier = Modifier.fillMaxWidth(),
                    height = 4.dp,
                )
            } else {
                SetProgress(page = currentPage, reels = reels, answers = state.answers)
            }
        },
        subtitle = if (scoped) {
            "Lesson ${FeedPaging.reelOf(currentPage, reelCount) + 1} of $reelCount"
        } else {
            null
        },
    )

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var burstAt by remember { mutableStateOf<Offset?>(null) }
    val burst = remember { Animatable(0f) }
    fun doubleTapSave(reelId: String, at: Offset) {
        val nowSaved = viewModel.onToggleSave(reelId)
        if (nowSaved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            burstAt = at
            scope.launch {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(650))
                burstAt = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2,
            key = { page -> pageKey(FeedPaging.pageAt(page, reelCount), reels) },
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.2f),
        ) { pageIndex ->
            when (val page = FeedPaging.pageAt(pageIndex, reelCount)) {
                is Page.Lesson -> {
                    val reel = reels[page.reel]
                    LessonPage(
                        reel = reel,
                        saved = reel.id in state.savedIds,
                        emphasizeHint = !swipeHintSeen && pageIndex == 0,
                        onToggleSave = { viewModel.onToggleSave(reel.id) },
                        onDoubleTap = { at -> doubleTapSave(reel.id, at) },
                    )
                }
                is Page.Quiz -> {
                    val reel = reels[page.reel]
                    QuizPage(
                        reel = reel,
                        number = page.reel % FeedPaging.SET_SIZE + 1,
                        ofCount = FeedPaging.reelsInSet(FeedPaging.setOf(page.reel), reelCount),
                        record = state.answers[reel.id],
                        isLastInSet = page.reel == FeedPaging.reelRange(FeedPaging.setOf(page.reel), reelCount).last,
                        onAnswer = { r -> viewModel.onGrade(reel.id, r.correct, r.choice) },
                    )
                }
                is Page.Recap -> {
                    val range = FeedPaging.reelRange(page.set, reelCount)
                    RecapPage(
                        setReels = range.map { reels[it] },
                        answers = state.answers,
                        stats = stats,
                        nextReel = reels.getOrNull(range.last + 1),
                        isFinal = page.set == FeedPaging.setCount(reelCount) - 1,
                        scopedTitle = if (scoped) heading else null,
                        settled = pagerState.settledPage == pageIndex,
                        onBack = onBack,
                    )
                }
            }
        }

        val at = burstAt
        if (at != null) {
            val density = LocalDensity.current
            val half = with(density) { 36.dp.toPx() }
            val t = burst.value
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .offset { IntOffset((at.x - half).toInt(), (at.y - half).toInt()) }
                    .size(72.dp)
                    .graphicsLayer {
                        val s = if (t < 0.35f) 0.5f + t / 0.35f * 0.7f else 1.2f - (t - 0.35f) * 0.3f
                        scaleX = s
                        scaleY = s
                        alpha = if (t < 0.6f) 1f else (1f - (t - 0.6f) / 0.4f)
                    },
            )
        }
    }
}

private fun pageKey(page: Page, reels: List<Reel>): String = when (page) {
    is Page.Lesson -> "L-" + reels[page.reel].id
    is Page.Quiz -> "Q-" + reels[page.reel].id
    is Page.Recap -> "R-" + page.set
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun FeedTopBar(
    onBack: (() -> Unit)?,
    title: String?,
    stats: FeedStats,
    progress: (@Composable () -> Unit)?,
    subtitle: String? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = if (onBack != null) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FerrisIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                XpPill(stats.xp, Modifier.padding(end = 12.dp))
            } else {
                StreakPill(stats.streakDays)
                Box(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    progress?.invoke()
                }
                XpPill(stats.xp)
            }
        }
        if (onBack != null && progress != null) {
            Box(Modifier.padding(horizontal = 16.dp)) { progress() }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Five segments for the current set, coloured by each quiz result. */
@Composable
private fun SetProgress(
    page: Page,
    reels: List<Reel>,
    answers: Map<String, AnswerRecord>,
) {
    val palette = FerrisTheme.palette
    val reelCount = reels.size
    val set = when (page) {
        is Page.Recap -> page.set
        else -> FeedPaging.setOf(FeedPaging.reelOf(page, reelCount))
    }
    val range = FeedPaging.reelRange(set, reelCount)
    val currentReel = FeedPaging.reelOf(page, reelCount)
    val position = currentReel - range.first + 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (page is Page.Recap) "Set complete" else "Lesson $position of ${range.count()} in this set"
            },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        range.forEach { i ->
            val record = answers[reels[i].id]
            val color = when {
                record?.correct == true -> palette.success
                record?.correct == false -> palette.danger
                i == currentReel && page !is Page.Recap -> MaterialTheme.colorScheme.onBackground
                i < currentReel || page is Page.Recap -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> palette.hairline
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------

@Composable
private fun LessonPage(
    reel: Reel,
    saved: Boolean,
    emphasizeHint: Boolean,
    onToggleSave: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(reel.id) { detectTapGestures(onDoubleTap = onDoubleTap) },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = LocalBottomBarInset.current + 20.dp),
        ) {
            LessonContent(
                track = reel.track.id,
                topicTitle = Curriculum.titleOf(reel.topic),
                level = reel.level,
                hook = reel.hook,
                body = reel.bodyMd,
                takeaway = reel.takeaway,
                code = reel.code,
                language = reel.language,
                output = reel.output,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NextHint(text = "Swipe up for the quick check", emphasize = emphasizeHint, modifier = Modifier.weight(1f))
                FerrisIconButton(
                    icon = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (saved) "Remove from saved" else "Save lesson",
                    tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleSave()
                    },
                )
            }
        }
    }
}

@Composable
private fun QuizPage(
    reel: Reel,
    number: Int,
    ofCount: Int,
    record: AnswerRecord?,
    isLastInSet: Boolean,
    onAnswer: (QuizResult) -> Unit,
) {
    val quiz = remember(reel.id) { reel.toQuizUi() }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = LocalBottomBarInset.current + 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Quick check", color = trackTextColor(reel.track.id), modifier = Modifier.weight(1f))
            Text(
                text = "$number / $ofCount",
                style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = reel.hook,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(18.dp))
        QuizContent(
            quiz = quiz,
            seed = reel.id.hashCode(),
            result = record?.let { QuizResult(it.correct, it.choice) },
            onAnswer = onAnswer,
            trap = reel.trap.ifBlank { null },
            xpCorrect = FeedViewModel.XP_CORRECT,
            xpWrong = FeedViewModel.XP_WRONG,
        )
        if (record != null) {
            Spacer(Modifier.height(20.dp))
            NextHint(text = if (isLastInSet) "Swipe up to see how you did" else "Swipe up for the next lesson", emphasize = false)
        }
    }
}

@Composable
private fun RecapPage(
    setReels: List<Reel>,
    answers: Map<String, AnswerRecord>,
    stats: FeedStats,
    nextReel: Reel?,
    isFinal: Boolean,
    scopedTitle: String?,
    settled: Boolean,
    onBack: (() -> Unit)?,
) {
    val palette = FerrisTheme.palette
    val answered = setReels.mapNotNull { answers[it.id] }
    val correct = answered.count { it.correct }
    val xpEarned = answered.sumOf { if (it.correct) FeedViewModel.XP_CORRECT else FeedViewModel.XP_WRONG }
    val topics = setReels.map { Curriculum.titleOf(it.topic) }.distinct()
    val headline = when {
        answered.isEmpty() -> "Set finished"
        correct == setReels.size -> "Flawless set"
        correct * 5 >= setReels.size * 4 -> "Strong set"
        correct * 2 >= setReels.size -> "Good progress"
        else -> "Worth another look"
    }
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(settled) {
        if (settled && answered.isNotEmpty() && correct * 5 >= setReels.size * 4) confetti++
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = LocalBottomBarInset.current + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(
                progress = if (setReels.isEmpty()) 0f else correct.toFloat() / setReels.size,
                size = 112.dp,
                strokeWidth = 10.dp,
                color = palette.success,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$correct/${setReels.size}",
                        style = MaterialTheme.typography.headlineMedium.merge(NumberStyle),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text("correct", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (answered.size < setReels.size) {
                    "You answered ${answered.size} of ${setReels.size} checks. Skipped ones come back later."
                } else {
                    "Missed questions return sooner so they stick."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 16.dp),
            ) {
                RecapStat(value = "+$xpEarned", label = "XP earned", color = palette.xp, modifier = Modifier.weight(1f))
                RecapStat(value = "${stats.streakDays}", label = "day streak", color = palette.rust, modifier = Modifier.weight(1f))
                RecapStat(value = "${stats.xp}", label = "total XP", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            val goal = FeedViewModel.DAILY_GOAL_XP
            Column(Modifier.fillMaxWidth()) {
                Row {
                    Text("Daily goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        text = if (stats.todayXp >= goal) "Done · ${stats.todayXp} XP" else "${stats.todayXp} / $goal XP",
                        style = MaterialTheme.typography.labelMedium.merge(NumberStyle),
                        color = if (stats.todayXp >= goal) palette.successInk else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearMeter(
                    progress = stats.todayXp.toFloat() / goal,
                    color = if (stats.todayXp >= goal) palette.success else palette.xp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))
            Overline("Covered", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                topics.forEach { t ->
                    Text(
                        text = t,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.raised)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            if (isFinal || nextReel == null) {
                Text(
                    text = if (scopedTitle != null) "That's every lesson in $scopedTitle." else "You've reached the end of your queue.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                if (onBack != null) {
                    Spacer(Modifier.height(16.dp))
                    FerrisButton(text = "Back", onClick = onBack, style = FerrisButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                ) {
                    Overline("Up next")
                    Spacer(Modifier.height(8.dp))
                    TrackTag(track = nextReel.track.id, topicTitle = Curriculum.titleOf(nextReel.topic))
                    Spacer(Modifier.height(6.dp))
                    Text(nextReel.hook, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(20.dp))
                NextHint(text = "Swipe up for the next set", emphasize = true)
            }
        }
        ConfettiBurst(trigger = confetti, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun RecapStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.merge(NumberStyle), color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** "↑ Swipe up …" cue. Bounces gently when [emphasize] (first run, recap). */
@Composable
private fun NextHint(text: String, emphasize: Boolean, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val lift by if (emphasize && !reduceMotion) {
        rememberInfiniteTransition(label = "hint").animateFloat(
            initialValue = 0f,
            targetValue = -5f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "hint-lift",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Row(
        modifier = modifier.graphicsLayer { translationY = lift * density },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Loading / empty
// ---------------------------------------------------------------------------

@Composable
private fun FeedLoading() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = "Loading lessons" },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(Modifier.width(160.dp).height(14.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(30.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(30.dp))
        Spacer(Modifier.height(4.dp))
        repeat(3) { SkeletonBlock(Modifier.fillMaxWidth().height(16.dp)) }
        SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(16.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(140.dp), RoundedCornerShape(14.dp))
    }
}

@Composable
private fun EmptyFeed(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FerrisMark(size = 72.dp, animated = false)
        Spacer(Modifier.height(20.dp))
        Text(
            "No lessons yet",
            style = FerrisType.Hook,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Lessons are still being unpacked on this device. Give it a moment, then try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FerrisButton(text = "Try again", onClick = onRetry)
    }
}

// ---------------------------------------------------------------------------
// Mapping
// ---------------------------------------------------------------------------

private fun Reel.toQuizUi(): QuizUiModel = when (quiz.type) {
    QuizType.MCQ -> QuizUiModel.Mcq(
        question = quiz.question,
        options = quiz.options,
        answerIndex = quiz.answerIndex,
        explanation = quiz.explanation,
    )
    QuizType.TAP_BUG -> QuizUiModel.TapBug(
        question = quiz.question,
        lines = quiz.codeLines,
        buggyLineIndex = quiz.buggyLineIndex,
        explanation = quiz.explanation,
    )
    QuizType.FILL_BLANK -> QuizUiModel.FillBlank(
        question = quiz.question,
        prefix = quiz.prefix,
        suffix = quiz.suffix,
        acceptedAnswers = quiz.acceptedAnswers,
        explanation = quiz.explanation,
    )
    QuizType.BLOCKS -> QuizUiModel.Blocks(
        question = quiz.question,
        prefix = quiz.prefix,
        suffix = quiz.suffix,
        targetBlocks = quiz.targetBlocks.ifEmpty { quiz.acceptedAnswers },
        distractorBlocks = quiz.distractorBlocks.ifEmpty { quiz.options },
        explanation = quiz.explanation,
    )
}

@Preview(name = "Empty feed", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 640)
@Composable
private fun EmptyFeedPreview() {
    FerrisFeedTheme(darkTheme = true) {
        EmptyFeed(onRetry = {})
    }
}

@Preview(name = "Loading feed", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360, heightDp = 640)
@Composable
private fun LoadingFeedPreview() {
    FerrisFeedTheme(darkTheme = false) {
        FeedLoading()
    }
}
