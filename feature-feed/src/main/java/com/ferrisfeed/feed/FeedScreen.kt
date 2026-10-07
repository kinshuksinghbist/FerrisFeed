package com.ferrisfeed.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ferrisfeed.coreui.ConfettiBurst
import com.ferrisfeed.coreui.DisplayFont
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisButtonStyle
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.LocalReduceMotion
import com.ferrisfeed.coreui.QuizCard
import com.ferrisfeed.coreui.QuizUiModel
import com.ferrisfeed.coreui.ReelCard
import com.ferrisfeed.coreui.ReelSkeleton
import com.ferrisfeed.coreui.SpeakCard
import com.ferrisfeed.coreui.SpeakState
import com.ferrisfeed.coreui.SwipeCue
import com.ferrisfeed.coreui.glass
import com.ferrisfeed.coreui.staggeredEntrance
import com.ferrisfeed.coreui.trackColor
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onBack: (() -> Unit)? = null,
    title: String? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val speakDismissed by viewModel.speakDismissed.collectAsState()
    val swipeHintSeen by viewModel.swipeHintSeen.collectAsState()

    if (state.isLoading && state.reels.isEmpty()) {
        FeedLoading(modifier = modifier)
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

    var confettiTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewModel) {
        viewModel.mastered.collect {
            confettiTrigger++
        }
    }

    // Double-tap save burst feedback (P6 33i)
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var bookmarkBurstOffset by remember { mutableStateOf<Offset?>(null) }
    val bookmarkBurstScale = remember { Animatable(0.4f) }
    val bookmarkBurstAlpha = remember { Animatable(1f) }

    fun handleDoubleTapSave(reelId: String, isSaved: Boolean, tapOffset: Offset) {
        val willSave = !isSaved
        viewModel.onToggleSave(reelId)
        if (willSave) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            bookmarkBurstOffset = tapOffset
            coroutineScope.launch {
                bookmarkBurstScale.snapTo(0.4f)
                bookmarkBurstAlpha.snapTo(1f)
                launch {
                    bookmarkBurstScale.animateTo(1.2f, FerrisMotion.Bouncy)
                    bookmarkBurstScale.animateTo(1f, FerrisMotion.Snappy)
                }
                launch {
                    delay(200)
                    bookmarkBurstAlpha.animateTo(0f, tween(400, easing = LinearOutSlowInEasing))
                    bookmarkBurstOffset = null
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
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

            // Per-page 3D transform (P6 33b)
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val absOffset = kotlin.math.abs(pageOffset)
            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f - 0.06f * absOffset
                        scaleY = 1f - 0.06f * absOffset
                        alpha = 1f - 0.5f * absOffset
                        rotationX = -6f * pageOffset
                        cameraDistance = 12f * density
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    },
            ) {
                if (page % 2 == 0) {
                    InfoPage(
                        reel = reel,
                        isSaved = isSaved,
                        isLiked = isLiked,
                        onLike = { viewModel.onLike(reel.id, !isLiked) },
                        onSave = { viewModel.onSave(reel.id, !isSaved) },
                        onDoubleTapSave = { offset -> handleDoubleTapSave(reel.id, isSaved, offset) },
                        onInteract = { viewModel.onInteract() },
                        onRecognition = { id, heard -> viewModel.onRecognition(id, heard) },
                        showSpeak = page == 0 && !speakDismissed,
                        onDismissSpeak = { viewModel.dismissSpeakPrompt() },
                        settled = pagerState.settledPage == page,
                    )
                } else {
                    QuizPage(
                        reel = reel,
                        isSaved = isSaved,
                        isLiked = isLiked,
                        settled = pagerState.settledPage == page,
                        onLike = { viewModel.onLike(reel.id, !isLiked) },
                        onSave = { viewModel.onSave(reel.id, !isSaved) },
                        onDoubleTapSave = { offset -> handleDoubleTapSave(reel.id, isSaved, offset) },
                        onGrade = { correct, label -> viewModel.onGrade(reel.id, correct, label) },
                        onInteract = { viewModel.onInteract() },
                    )
                }
            }
        }

        // 5-segment session queue progress indicator under stat bar (P6 33a)
        SessionProgressBar(
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        )

        // Top-left back chip when route provides onBack (P6 33d)
        if (onBack != null && title != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FerrisIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 44.dp,
                )
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .glass(CircleShape)
                        .clip(CircleShape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = DisplayFont,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // First-run swipe cue on lesson page (P6 33e)
        AnimatedVisibility(
            visible = !swipeHintSeen && pagerState.currentPage == 0,
            enter = fadeIn(animationSpec = FerrisMotion.Quick),
            exit = fadeOut(animationSpec = FerrisMotion.Quick),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = LocalBottomBarInset.current + 12.dp),
        ) {
            SwipeCue(text = "Swipe up")
        }

        // Double-tap bookmark burst (P6 33i)
        if (bookmarkBurstOffset != null) {
            val offset = bookmarkBurstOffset!!
            val density = LocalDensity.current
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (offset.x - with(density) { 48.dp.toPx() }).toInt(),
                            y = (offset.y - with(density) { 48.dp.toPx() }).toInt(),
                        )
                    }
                    .size(96.dp)
                    .scale(bookmarkBurstScale.value)
                    .alpha(bookmarkBurstAlpha.value),
            )
        }

        ConfettiBurst(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
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
    onDoubleTapSave: (Offset) -> Unit,
    onInteract: () -> Unit,
    onRecognition: (reelId: String, heard: Boolean) -> Unit = { _, _ -> },
    showSpeak: Boolean = true,
    onDismissSpeak: () -> Unit = {},
    settled: Boolean = true,
) {
    val context = LocalContext.current
    val trackColor = trackColor(reel.track.id)
    var speakState: SpeakState by remember(reel.id) { mutableStateOf(SpeakState.Prompt) }
    var recognizer: android.speech.SpeechRecognizer? by remember(reel.id) {
        mutableStateOf(null)
    }
    // Dismissed prompt stays dismissed for this reel instance only.
    var speakHidden: Boolean by remember(reel.id) { mutableStateOf(false) }
    var heardCollapsed: Boolean by remember(reel.id) { mutableStateOf(false) }

    LaunchedEffect(speakState) {
        if (speakState is SpeakState.Heard) {
            delay(2500L)
            heardCollapsed = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            speakState = SpeakState.Listening(0f)
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
                onLevel = { level ->
                    speakState = SpeakState.Listening(level)
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
            speakState = SpeakState.Listening(0f)
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
                onLevel = { level ->
                    speakState = SpeakState.Listening(level)
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
            .drawBehind {
                // Full-bleed track gradient behind lesson card (P6 33c)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            trackColor.copy(alpha = 0.22f),
                            trackColor.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                )
            }
            .pointerInput(reel.id) {
                detectTapGestures(onDoubleTap = { offset -> onDoubleTapSave(offset) })
            },
    ) {
        // Content column: info with the code well contained.
        // No scroll: everything must fit, the pager handles all motion.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 56.dp, bottom = LocalBottomBarInset.current + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                settled = settled,
            )
            ReelActionRow(
                topic = reel.topic,
                isSaved = isSaved,
                isLiked = isLiked,
                onLike = { onInteract(); onLike() },
                onSave = { onInteract(); onSave() },
            )
        }

        // Overlay bottom dock card for speaker prompt (P6 Item 37a, 33h)
        val showDock = showSpeak && !speakHidden && !heardCollapsed
        AnimatedVisibility(
            visible = showDock,
            enter = slideInVertically(
                animationSpec = FerrisMotion.BouncyOffset,
                initialOffsetY = { it },
            ) + fadeIn(animationSpec = FerrisMotion.Quick),
            exit = slideOutVertically(
                animationSpec = FerrisMotion.SmoothOffset,
                targetOffsetY = { it },
            ) + fadeOut(animationSpec = FerrisMotion.Quick),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = LocalBottomBarInset.current + 16.dp,
                ),
        ) {
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
    }
}

/**
 * Quiz reel: the question gets its own page (user review 2026-10-07), with
 * the hook as the retrieval cue. Same action row + double-tap-save as
 * the lesson page so gestures never change meaning mid-reel.
 */
@Composable
private fun QuizPage(
    reel: Reel,
    isSaved: Boolean,
    isLiked: Boolean,
    settled: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onDoubleTapSave: (Offset) -> Unit,
    onGrade: (Boolean, String) -> Unit,
    onInteract: () -> Unit,
) {
    val trackColor = trackColor(reel.track.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(reel.id) {
                detectTapGestures(onDoubleTap = { offset -> onDoubleTapSave(offset) })
            }
            .drawBehind {
                // Faint radial glow behind question (P6 33c)
                val radius = size.width * 0.70f
                val center = Offset(size.width / 2f, size.height * 0.30f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(trackColor.copy(alpha = 0.18f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 56.dp,
                    bottom = LocalBottomBarInset.current + 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // PROVE IT overline (P6 38a)
            Text(
                text = "PROVE IT",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.primary,
            )
            // Hook as quiet retrieval cue (P6 38a)
            Text(
                text = reel.hook,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            // Quiz options & variants
            QuizCard(
                quiz = reel.toQuizUi(),
                settled = settled,
                onResult = { correct, label -> onInteract(); onGrade(correct, label) },
                modifier = Modifier.weight(1f, fill = false),
            )
            // Action row below card (P6 36a)
            ReelActionRow(
                topic = reel.topic,
                isSaved = isSaved,
                isLiked = isLiked,
                onLike = { onInteract(); onLike() },
                onSave = { onInteract(); onSave() },
            )
        }
    }
}

/**
 * Horizontal action row placed below the card (P6 34i, 36a, 36b):
 * Left group = like FerrisIconButton + save FerrisIconButton with Confirm haptic;
 * Right group = glass topic capsule chip.
 */
@Composable
private fun ReelActionRow(
    topic: String,
    isSaved: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val topicLabel = remember(topic) {
        topic.replace('_', ' ').replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FerrisIconButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onLike()
                },
                size = 44.dp,
                active = isLiked,
                activeTint = MaterialTheme.colorScheme.error,
            )
            FerrisIconButton(
                icon = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isSaved) "Unsave" else "Save",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave()
                },
                size = 44.dp,
                active = isSaved,
                activeTint = MaterialTheme.colorScheme.primary,
            )
        }

        Box(
            modifier = Modifier
                .height(32.dp)
                .glass(CircleShape)
                .clip(CircleShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = topicLabel,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * 5-segment session queue progress indicator under stat bar (P6 33a).
 * Shows position in 5-reel session queue window.
 */
@Composable
private fun SessionProgressBar(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val currentReel = currentPage / 2
    val isQuiz = currentPage % 2 == 1
    val segmentInWindow = currentReel % 5

    val currentFill by animateFloatAsState(
        targetValue = if (isQuiz) 1f else 0.5f,
        animationSpec = FerrisMotion.Smooth,
        label = "segmentFill",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val futureColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

        for (i in 0 until 5) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(futureColor),
            ) {
                when {
                    i < segmentInWindow -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(primaryColor),
                        )
                    }
                    i == segmentInWindow -> {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = currentFill)
                                .background(primaryColor),
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Centered pulsing FerrisMark loading state with ReelSkeleton (P6 33f).
 * One indicator only — spinner removed.
 */
@Composable
private fun FeedLoading(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "feedLoadingPulse")
    val pulseScale by if (reduceMotion) {
        remember { mutableFloatStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            ) {
                FerrisMark(size = 56.dp)
                Text(
                    text = "Warming up your feed\u2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            ReelSkeleton()
        }
    }
}

/**
 * Redesigned empty state (P6 33g):
 * 96dp Canvas crab claw illustration, headline, body, filled pill FerrisButton.
 */
@Composable
private fun EmptyFeed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 96dp illustration drawn with Canvas: stylized crab claw composition
        Box(
            modifier = Modifier
                .size(96.dp)
                .staggeredEntrance(0),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(96.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f),
                    radius = size.width * 0.46f,
                    center = center,
                )
                drawArc(
                    color = primaryColor,
                    startAngle = 140f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    ),
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width * 0.28f, size.height * 0.38f),
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width * 0.72f, size.height * 0.38f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.staggeredEntrance(1),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "The curriculum is still seeding into the local database, " +
                "or seeding failed. Wait a moment and retry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEntrance(2),
        )

        Spacer(Modifier.height(24.dp))

        FerrisButton(
            text = "Try again",
            style = FerrisButtonStyle.Filled,
            onClick = onRetry,
            modifier = Modifier.staggeredEntrance(3),
        )
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
