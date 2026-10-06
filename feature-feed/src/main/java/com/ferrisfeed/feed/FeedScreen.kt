package com.ferrisfeed.feed

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.CodeCard
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.QuizCard
import com.ferrisfeed.coreui.QuizUiModel
import com.ferrisfeed.coreui.ReelCard
import com.ferrisfeed.coreui.ReelSkeleton
import androidx.compose.ui.tooling.preview.Preview

/**
 * Doomscroll feed (Spec v2).
 *
 * - Full-screen [VerticalPager], one reel per page. No inner vertical scroll
 *   anywhere: the pager owns all vertical motion, and a low snap threshold
 *   means even a small swipe commits to the next page.
 * - Each page is ONE unified card stack: info ([ReelCard]) -> code
 *   ([CodeCard] with flip-to-output) -> quiz inline ([QuizCard]). Quiz
 *   answers are the sole SRS signal via [FeedViewModel.onGrade].
 * - Like/save live on an Instagram-style right rail (48dp targets);
 *   double-tap anywhere toggles save. No buttons, sheets, or hints inside
 *   the content column.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier,
    /** Deep-link / search entry: jump to this reel once the queue loads. */
    focusedReelId: String? = null,
) {
    val state by viewModel.uiState.collectAsState()

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

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex.coerceIn(0, maxOf(0, state.reels.size - 1)),
        pageCount = { state.reels.size },
    )

    // ViewModel <- pager position (skip first emission which is the restore).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }
    // Pager <- ViewModel restores (e.g. process recreation keeps DataStore index).
    LaunchedEffect(state.currentIndex) {
        if (pagerState.currentPage != state.currentIndex) {
            pagerState.scrollToPage(state.currentIndex)
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
        val reel = state.reels.getOrNull(page) ?: return@VerticalPager
        val isSaved = state.savedIds.contains(reel.id)
        val isLiked = state.likedIds.contains(reel.id)
        ReelPage(
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

@Composable
private fun ReelPage(
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
        // Content column: info -> code -> quiz. No scroll: everything must
        // fit, the pager handles all motion.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 68.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReelCard(
                track = reel.track.id,
                level = reel.level,
                hook = reel.hook,
                body = reel.bodyMd,
                takeaway = reel.takeaway,
            )
            if (reel.code != null) {
                CodeCard(
                    code = reel.code,
                    language = reel.language,
                    output = reel.output,
                )
            }
            QuizCard(
                quiz = reel.toQuizUi(),
                onResult = { correct, label -> onInteract(); onGrade(correct, label) },
            )
        }
        // Right action rail, vertically centered like Reels/TikTok.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = { onInteract(); onLike() },
                modifier = Modifier.size(48.dp),
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
                onClick = { onInteract(); onSave() },
                modifier = Modifier.size(48.dp),
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
