package com.ferrisfeed.feed

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import com.ferrisfeed.coreui.TrapCard
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

/**
 * Doomscroll feed.
 *
 * - Full-screen [VerticalPager], one reel per page.
 * - Prefetch: ViewModel warms next 5 reels on every page change; UI shows [ReelSkeleton]
 *   while [FeedUiState.isLoading] is true.
 * - Double-tap: toggles save (heart/save burst handled by ReelCard state).
 * - Long-press: peeks the quiz answer overlay without grading.
 * - Swipe-left: per-reel [HorizontalPager] page 1 is the quiz variant.
 * - Deep Dive: modal bottom sheet with full body + code + trap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onRunCode: (reelId: String, code: String) -> Unit,
    modifier: Modifier = Modifier,
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

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 5, // prefetch next 5 compositions
    ) { page ->
        val reel = state.reels.getOrNull(page) ?: return@VerticalPager
        val isSaved = state.savedIds.contains(reel.id)
        val isLiked = state.likedIds.contains(reel.id)
        ReelPage(
            reel = reel,
            isSaved = isSaved,
            isLiked = isLiked,
            peekActive = state.peekReelId == reel.id,
            onLike = { viewModel.onLike(reel.id, !isLiked) },
            onSave = { viewModel.onSave(reel.id, !isSaved) },
            onDoubleTapSave = { viewModel.onToggleSave(reel.id) },
            onLongPressPeek = { viewModel.onPeek(reel.id) },
            onPeekRelease = { viewModel.onPeek(null) },
            onDeepDive = { viewModel.onDeepDive(reel.id) },
            onGrade = { correct, label -> viewModel.onGrade(reel.id, correct, label) },
            onInteract = { viewModel.onInteract() },
            onRunCode = { onRunCode(reel.id, it) },
        )
    }

    // Deep Dive bottom sheet
    val deepDiveReel = state.reels.firstOrNull { it.id == state.deepDiveReelId }
    if (deepDiveReel != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDeepDive(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            DeepDiveContent(
                reel = deepDiveReel,
                onRunCode = { onRunCode(deepDiveReel.id, it) },
            )
        }
    }
}

@Composable
private fun ReelPage(
    reel: Reel,
    isSaved: Boolean,
    isLiked: Boolean,
    peekActive: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDoubleTapSave: () -> Unit,
    onLongPressPeek: () -> Unit,
    onPeekRelease: () -> Unit,
    onDeepDive: () -> Unit,
    onGrade: (Boolean, String) -> Unit,
    onInteract: () -> Unit,
    onRunCode: (String) -> Unit,
) {
    // Inner horizontal pager: 0 = explainer, 1 = quiz variant.
    val quizPager = rememberPagerState(pageCount = { 2 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapSave() },
                    onLongPress = {
                        onLongPressPeek()
                    },
                    onPress = {
                        tryAwaitRelease()
                        onPeekRelease()
                    },
                )
            },
    ) {
        HorizontalPager(
            state = quizPager,
            modifier = Modifier.fillMaxSize(),
        ) { qPage ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 20.dp),
            ) {
                if (qPage == 0) {
                    ExplainerContent(
                        reel = reel,
                        isSaved = isSaved,
                        isLiked = isLiked,
                        onLike = { onInteract(); onLike() },
                        onSave = { onInteract(); onSave() },
                        onDeepDive = { onInteract(); onDeepDive() },
                        onRunCode = { onInteract(); onRunCode(it) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "← Swipe for quiz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    val quizUi = reel.toQuizUi()
                    QuizCard(
                        quiz = quizUi,
                        onResult = { correct, label -> onInteract(); onGrade(correct, label) },
                    )
                    if (peekActive) {
                        Spacer(Modifier.height(8.dp))
                        PeekAnswerOverlay(answerText = peekText(reel))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Swipe → back to explainer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplainerContent(
    reel: Reel,
    isSaved: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDeepDive: () -> Unit,
    onRunCode: (String) -> Unit,
) {
    ReelCard(
        track = reel.track.id,
        level = reel.level,
        readTimeSec = estimateReadSeconds(reel),
        hook = reel.hook,
        body = reel.bodyMd,
        takeaway = reel.takeaway,
        isLiked = isLiked,
        isSaved = isSaved,
        onLike = onLike,
        onSave = onSave,
        onDeepDive = onDeepDive,
        onGotIt = {},
        onFuzzy = {},
    )
    if (reel.code != null) {
        Spacer(Modifier.height(12.dp))
        CodeCard(code = reel.code, language = reel.language, onRun = onRunCode)
    }
    Spacer(Modifier.height(12.dp))
    TrapCard(trap = reel.trap, compilerMessage = reel.trapCompilerMessage)
}

/** Deep Dive sheet: full-bleed body, code, trap, and quiz. */
@Composable
private fun DeepDiveContent(
    reel: Reel,
    onRunCode: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(text = reel.hook, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(text = reel.bodyMd, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        if (reel.code != null) {
            CodeCard(code = reel.code, language = reel.language, onRun = onRunCode)
            Spacer(Modifier.height(12.dp))
        }
        TrapCard(trap = reel.trap, compilerMessage = reel.trapCompilerMessage, initiallyExpanded = true)
        Spacer(Modifier.height(12.dp))
        QuizCard(quiz = reel.toQuizUi(), onResult = { _, _ -> })
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PeekAnswerOverlay(answerText: String) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Text(
            text = "Peek: $answerText",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**~200wpm → seconds, min 20s so a reel never claims to be instant. */
fun estimateReadSeconds(reel: Reel): Int {
    val words = (reel.hook + " " + reel.bodyMd + " " + reel.takeaway)
        .split(Regex("\\s+")).count { it.isNotBlank() }
    val codeLines = reel.code?.lines()?.size ?: 0
    return maxOf(20, (words / 200.0 * 60).toInt() + codeLines * 3)
}

private fun peekText(reel: Reel): String = when (reel.quiz.type) {
    QuizType.MCQ -> reel.quiz.options.getOrNull(reel.quiz.answerIndex) ?: ""
    QuizType.TAP_BUG -> "line ${reel.quiz.buggyLineIndex + 1}"
    QuizType.FILL_BLANK -> reel.quiz.acceptedAnswers.firstOrNull().orEmpty()
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

// Keep SheetValue import referenced for predictive-back customization hook.
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
private fun isSheetExpandedHack(v: SheetValue): Boolean = v == SheetValue.Expanded

@Preview(name = "Feed explainer", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun FeedScreenPreviewLite() {
    FerrisFeedTheme(darkTheme = true) {
        ExplainerContent(
            reel = previewReel(),
            isSaved = true,
            isLiked = false,
            onLike = {},
            onSave = {},
            onDeepDive = {},
            onRunCode = {},
        )
    }
}

private fun previewReel() = Reel(
    id = "rust-own-014",
    track = Track.RUST,
    level = 1,
    hook = "Why does this simple function not compile?",
    bodyMd = "Ownership moves values. Pass a String by value and the caller loses it.",
    code = "fn main() {\n    let s = String::from(\"hi\");\n}",
    language = "rust",
    takeaway = "Move by default; borrow with & to keep ownership.",
    trap = "Using s after move.",
    trapCompilerMessage = "error[E0382]: borrow of moved value",
    quiz = QuizModel(
        type = QuizType.MCQ,
        question = "What happens?",
        options = listOf("move", "copy"),
        answerIndex = 0,
        explanation = "String moves.",
    ),
)
