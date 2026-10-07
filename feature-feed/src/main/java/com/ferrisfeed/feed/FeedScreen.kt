package com.ferrisfeed.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.QuizCard
import com.ferrisfeed.coreui.QuizUiModel
import com.ferrisfeed.coreui.ReelCard
import com.ferrisfeed.coreui.ReelSkeleton
import com.ferrisfeed.coreui.SpeakCard
import com.ferrisfeed.coreui.SpeakState
import androidx.compose.ui.tooling.preview.Preview

/**
 * Doomscroll feed (Spec v2 + TODO 24 speaker opening).
 *
 * - Full-screen [VerticalPager], one reel per page. No inner vertical scroll
 *   anywhere: the pager owns all vertical motion, and a low snap threshold
 *   means even a small swipe commits to the next page.
 * - Two pages per reel: the lesson (speaker prompt ([SpeakCard]) on the
 *   first reel only -> info ([ReelCard]) with the code well contained
 *   inside it, flip-to-output included) and then the quiz on its own page
 *   ([QuizCard] with the hook as the cue). Quiz answers are the sole SRS
 *   signal via [FeedViewModel.onGrade]. The lesson text composes
 *   immediately; speech only gates the difficulty reward motion
 *   (`animateDifficulty`).
 * - Like/save float on a translucent right rail over the content edge
 *   (48dp targets); double-tap anywhere toggles save. No buttons, sheets,
 *   or hints inside the content column.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier,
    /** Deep-link / search entry: jump to this reel once the queue loads. */
    focusedReelId: String? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val speakDismissed by viewModel.speakDismissed.collectAsState()

    if (state.isLoading && state.reels.isEmpty()) {
        Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ReelSkeleton()
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
        }
        return
    }

    // Loaded but the database yielded nothing (seeding failed or was wiped).
    if (state.reels.isEmpty()) {
        EmptyFeed(onRetry = { viewModel.retryLoad() }, modifier = modifier)
        return
    }

    // Two pages per reel: even = lesson (info + contained code), odd = quiz.
    // The ViewModel still thinks in reels; this screen maps page <-> reel.
    val pageCount = state.reels.size * 2
    val pagerState = rememberPagerState(
        initialPage = (state.currentIndex * 2).coerceIn(0, maxOf(0, pageCount - 1)),
        pageCount = { pageCount },
    )

    // ViewModel <- pager position, deduped per reel so lesson -> quiz on the
    // same reel reports once instead of logging a phantom skip on its lesson.
    var lastReportedReel by remember { mutableStateOf(-1) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val reelIndex = page / 2
            if (reelIndex != lastReportedReel) {
                lastReportedReel = reelIndex
                viewModel.onPageChanged(reelIndex)
            }
        }
    }
    // Pager <- ViewModel restores (e.g. process recreation keeps DataStore index).
    LaunchedEffect(state.currentIndex) {
        val target = state.currentIndex * 2
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }
    // Deep-link / search entry point: jump once the queue is loaded.
    LaunchedEffect(focusedReelId) {
        if (focusedReelId != null) viewModel.focusReel(focusedReelId)
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 5, // prefetch next 5 compositions
        // Low positional threshold: small drags still commit to next page.
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = 0.25f,
        ),
    ) { page ->
        val reel = state.reels.getOrNull(page / 2) ?: return@VerticalPager
        val isSaved = state.savedIds.contains(reel.id)
        val isLiked = state.likedIds.contains(reel.id)
        if (page % 2 == 0) {
            InfoPage(
                reel = reel,
                isSaved = isSaved,
                isLiked = isLiked,
                onLike = { viewModel.onLike(reel.id, !isLiked) },
                onSave = { viewModel.onSave(reel.id, !isSaved) },
                onDoubleTapSave = { viewModel.onToggleSave(reel.id) },
                onInteract = { viewModel.onInteract() },
                onRecognition = { id, heard -> viewModel.onRecognition(id, heard) },
                showSpeak = page == 0 && !speakDismissed,
                onDismissSpeak = { viewModel.dismissSpeakPrompt() },
            )
        } else {
            QuizPage(
                reel = reel,
                isSaved = isSaved,
                isLiked = isLiked,
                onLike = { viewModel.onLike(reel.id, !isLiked) },
                onSave = { viewModel.onSave(reel.id, !isSaved) },
                onDoubleTapSave = { viewModel.onToggleSave(reel.id) },
                onGrade = { correct, label -> viewModel.onGrade(reel.id, correct, label) },
                onInteract = { viewModel.onInteract() },
            )
        }
    }
}

/**
 * Lesson page: speaker prompt (first reel only) + info card with the code
 * well contained inside it. No quiz here — it lives on [QuizPage].
 */
@Composable
private fun InfoPage(
    reel: Reel,
    isSaved: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDoubleTapSave: () -> Unit,
    onInteract: () -> Unit,
    onRecognition: (reelId: String, heard: Boolean) -> Unit = { _, _ -> },
    showSpeak: Boolean = true,
    onDismissSpeak: () -> Unit = {},
) {
    val context = LocalContext.current
    var speakState: SpeakState by remember(reel.id) { mutableStateOf(SpeakState.Prompt) }
    var recognizer: android.speech.SpeechRecognizer? by remember(reel.id) {
        mutableStateOf(null)
    }
    // Dismissed prompt stays dismissed for this reel instance only.
    var speakHidden: Boolean by remember(reel.id) { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            speakState = SpeakState.Listening
            onRecognition(reel.id, false)
            recognizer = SpeechRecognition.listenOnce(
                context,
                onHeard = { transcript ->
                    speakState = SpeakState.Heard(transcript)
                    onRecognition(reel.id, true)
                },
                onUnavailable = { reason ->
                    speakState = SpeakState.Unavailable(reason)
                },
            )
        } else {
            speakState = SpeakState.Unavailable(
                "Microphone permission is off — reading works the same.",
            )
        }
    }
    DisposableEffect(reel.id) {
        onDispose {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }
    fun startSpeak() {
        if (!SpeechRecognition.isAvailable(context)) {
            speakState = SpeakState.Unavailable(
                "Speech recognition is not available on this device.",
            )
            return
        }
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            speakState = SpeakState.Listening
            onRecognition(reel.id, false)
            recognizer = SpeechRecognition.listenOnce(
                context,
                onHeard = { transcript ->
                    speakState = SpeakState.Heard(transcript)
                    onRecognition(reel.id, true)
                },
                onUnavailable = { reason ->
                    speakState = SpeakState.Unavailable(reason)
                },
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // Recognition complete gates ONLY the difficulty reward motion (TODO 24b).
    val animateDifficulty = speakState is SpeakState.Heard
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(onDoubleTap = { onDoubleTapSave() })
            },
    ) {
        // Content column: speak -> info with the code well contained.
        // No scroll: everything must fit, the pager handles all motion.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The prompt opens the feed once (first page) instead of nagging
            // on every reel; hiding it persists across launches.
            if (showSpeak && !speakHidden) {
                SpeakCard(
                    state = speakState,
                    phrase = reel.hook,
                    onSpeak = { startSpeak() },
                    onRetry = { startSpeak() },
                    onDismiss = {
                        speakHidden = true
                        onDismissSpeak()
                    },
                )
            }
            ReelCard(
                track = reel.track.id,
                level = reel.level,
                hook = reel.hook,
                body = reel.bodyMd,
                takeaway = reel.takeaway,
                code = reel.code,
                language = reel.language,
                output = reel.output,
                animateDifficulty = animateDifficulty,
            )
        }
        ReelRail(
            isSaved = isSaved,
            isLiked = isLiked,
            onLike = { onInteract(); onLike() },
            onSave = { onInteract(); onSave() },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/**
 * Quiz reel: the question gets its own page (user review 2026-10-07), with
 * the hook as the retrieval cue. Same floating rail + double-tap-save as
 * the lesson page so gestures never change meaning mid-reel.
 */
@Composable
private fun QuizPage(
    reel: Reel,
    isSaved: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDoubleTapSave: () -> Unit,
    onGrade: (Boolean, String) -> Unit,
    onInteract: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(onDoubleTap = { onDoubleTapSave() })
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Prove it",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = reel.hook,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            QuizCard(
                quiz = reel.toQuizUi(),
                onResult = { correct, label -> onInteract(); onGrade(correct, label) },
            )
        }
        ReelRail(
            isSaved = isSaved,
            isLiked = isLiked,
            onLike = { onInteract(); onLike() },
            onSave = { onInteract(); onSave() },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/**
 * Shared floating rail: overlays the content edge like Reels/TikTok instead
 * of reserving its own column, with a translucent scrim so the glyphs read
 * over any card tint. 48dp targets, 28dp glyphs.
 */
@Composable
private fun ReelRail(
    isSaved: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onLike,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            onClick = onSave,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isSaved) "Unsave" else "Save",
                tint = if (isSaved) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

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
}

/** Shown when loading finished but Room returned zero reels. */
@Composable
private fun EmptyFeed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No reels yet",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "The curriculum is still seeding into the local database, " +
                "or seeding failed. Wait a moment and retry.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Preview(name = "Feed explainer", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun FeedScreenPreviewLite() {
    FerrisFeedTheme(darkTheme = true) {
        ReelCard(
            track = "rust",
            level = 1,
            hook = "Why does this simple function not compile?",
            body = "Ownership moves values. Pass a String by value and the caller loses it.",
            takeaway = "Move by default; borrow with & to keep ownership.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
