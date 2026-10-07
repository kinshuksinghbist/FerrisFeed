package com.ferrisfeed.coreui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** Quiz variants supported by the reel schema. */
sealed interface QuizUiModel {
    val question: String
    val explanation: String

    data class Mcq(
        override val question: String,
        val options: List<String>,
        val answerIndex: Int,
        override val explanation: String,
    ) : QuizUiModel

    /** Tap-the-bug: user picks the buggy line index from [lines]. */
    data class TapBug(
        override val question: String,
        val lines: List<String>,
        val buggyLineIndex: Int,
        override val explanation: String,
    ) : QuizUiModel

    /** Fill-blank: single blank, case-insensitive match against [acceptedAnswers]. */
    data class FillBlank(
        override val question: String,
        val prefix: String,
        val suffix: String,
        val acceptedAnswers: List<String>,
        override val explanation: String,
    ) : QuizUiModel
}

/**
 * Interactive quiz card (P6 Item 38 overhaul).
 * Shows hero question in [headlineMedium], followed by options directly on the page background
 * (no outer card wrapper).
 * Grades once per question instance ([resultSent] guard), reveals answer animations with
 * error shake / mint reveal, explanation panel, and bouncing swipe cue.
 */
@Composable
fun QuizCard(
    quiz: QuizUiModel,
    onResult: (correct: Boolean, label: String) -> Unit,
    modifier: Modifier = Modifier,
    settled: Boolean = true,
) {
    var answered by remember(quiz) { mutableStateOf(false) }
    var wasCorrect by remember(quiz) { mutableStateOf(false) }
    var resultSent by remember(quiz) { mutableStateOf(false) }
    var showSwipeCue by remember(quiz) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    fun submit(correct: Boolean, label: String) {
        if (resultSent) return
        resultSent = true
        answered = true
        wasCorrect = correct
        haptics.performHapticFeedback(
            if (correct) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
        )
        onResult(correct, label)
    }

    LaunchedEffect(answered) {
        if (answered) {
            delay(600)
            showSwipeCue = true
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero question (Item 38a)
        Text(
            text = quiz.question,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = DisplayFont,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when (quiz) {
            is QuizUiModel.Mcq -> McqBody(
                quiz = quiz,
                answered = answered,
                wasCorrect = wasCorrect,
                settled = settled,
                onPick = { idx -> submit(idx == quiz.answerIndex, quiz.options[idx]) },
            )
            is QuizUiModel.TapBug -> TapBugBody(
                quiz = quiz,
                answered = answered,
                wasCorrect = wasCorrect,
                settled = settled,
                onPick = { idx -> submit(idx == quiz.buggyLineIndex, "line ${idx + 1}") },
            )
            is QuizUiModel.FillBlank -> FillBlankBody(
                quiz = quiz,
                answered = answered,
                wasCorrect = wasCorrect,
                onSubmit = { text ->
                    val ok = quiz.acceptedAnswers.any { it.equals(text.trim(), ignoreCase = true) }
                    submit(ok, text.trim())
                },
            )
        }

        // Slide-up explanation panel (Item 38d)
        AnimatedVisibility(
            visible = answered,
            enter = slideInVertically(FerrisMotion.SmoothOffset) { it / 3 } + fadeIn(FerrisMotion.Smooth),
        ) {
            ExplanationPanel(
                correct = wasCorrect,
                explanation = quiz.explanation,
            )
        }

        // Bouncing swipe cue 600ms after answering (Item 38e)
        AnimatedVisibility(
            visible = showSwipeCue,
            enter = fadeIn(FerrisMotion.Quick) + slideInVertically(FerrisMotion.QuickOffset) { it / 4 },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                SwipeCue(text = "Swipe for next")
            }
        }
    }
}

@Composable
private fun McqBody(
    quiz: QuizUiModel.Mcq,
    answered: Boolean,
    wasCorrect: Boolean,
    settled: Boolean,
    onPick: (Int) -> Unit,
) {
    var selected by remember(quiz) { mutableIntStateOf(-1) }
    var revealCorrectBorder by remember(quiz) { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(answered) {
        if (answered && !wasCorrect) {
            delay(250)
            revealCorrectBorder = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        quiz.options.forEachIndexed { idx, option ->
            val isAnswer = idx == quiz.answerIndex
            val isSelected = idx == selected
            val isShaking = answered && isSelected && !wasCorrect

            // Error shake (±8dp, 3 oscillations, 320ms per Item 38c)
            val shakeX = remember(isShaking) { Animatable(0f) }
            LaunchedEffect(isShaking) {
                if (isShaking && !reduceMotion) {
                    shakeX.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 320
                            -8f at 40
                            8f at 90
                            -6f at 150
                            6f at 210
                            -3f at 265
                            0f at 320
                        },
                    )
                }
            }

            // Colors per theme (fixes dark-only mint bug in light mode, Item 38c)
            val correctContainer = if (isDark) FerrisColors.MintContainerDark else MaterialTheme.colorScheme.tertiaryContainer
            val targetContainer = when {
                answered && isAnswer -> correctContainer
                answered && isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> glassColor()
            }
            val containerColor by animateColorAsState(
                targetValue = targetContainer,
                animationSpec = FerrisMotion.Quick,
                label = "option-bg",
            )

            val showMintBorder = answered && isAnswer && (wasCorrect || revealCorrectBorder)
            val showErrorBorder = answered && isSelected && !wasCorrect
            val borderStroke = when {
                showMintBorder -> BorderStroke(1.5.dp, FerrisColors.MintCorrect)
                showErrorBorder -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                else -> BorderStroke(0.5.dp, glassStroke())
            }

            // Non-chosen non-correct rows fade to 45% alpha (Item 38c)
            val targetAlpha = if (answered && !isAnswer && !isSelected) 0.45f else 1f
            val rowAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = FerrisMotion.Quick,
                label = "row-alpha",
            )

            val interactionSource = remember { MutableInteractionSource() }
            val usesCodeFont = option.contains("(") || option.contains("&") || option.contains("::") || option.contains("<")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeX.value * density
                        alpha = rowAlpha
                    }
                    .staggeredEntrance(idx)
                    .pressScale(interactionSource, pressed = 0.97f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(containerColor, MaterialTheme.shapes.medium)
                    .border(borderStroke, MaterialTheme.shapes.medium)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !answered,
                    ) {
                        selected = idx
                        onPick(idx)
                    }
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Letter badge (32dp circle, A-D in titleSmall or check/X icon)
                    val badgeBg = when {
                        answered && isAnswer -> FerrisColors.MintCorrect.copy(alpha = 0.2f)
                        answered && isSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (answered && isAnswer) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Correct",
                                tint = FerrisColors.MintCorrect,
                                modifier = Modifier.size(18.dp),
                            )
                        } else if (answered && isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Incorrect",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                text = ('A' + idx).toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Option text in bodyLarge (Item 38b)
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = if (usesCodeFont) CodeFontFamily else null,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Sparkle on correct answer (Item 36c + 38c)
                if (answered && isAnswer && wasCorrect && isSelected) {
                    OptionSparkle(active = true, modifier = Modifier.matchParentSize())
                }
            }
        }
    }
}

/** 12-particle mint sparkle effect for correct answer joy (Item 36c). */
@Composable
private fun OptionSparkle(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion || !active) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            )
        }
    }

    if (progress.value in 0.01f..0.99f) {
        Canvas(modifier = modifier) {
            val p = progress.value
            val numParticles = 12
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxDist = size.width.coerceAtLeast(size.height) * 0.45f
            val distance = p * maxDist
            val alpha = (1f - p).coerceIn(0f, 1f)
            val radius = (3.dp.toPx() * (1f - p * 0.5f)).coerceAtLeast(1f)

            for (i in 0 until numParticles) {
                val angle = (i * (360f / numParticles) + (i * 13f)) * (Math.PI.toFloat() / 180f)
                val x = center.x + cos(angle) * (distance + 10f)
                val y = center.y + sin(angle) * (distance * 0.4f + 6f)
                drawCircle(
                    color = FerrisColors.MintCorrect.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

@Composable
private fun TapBugBody(
    quiz: QuizUiModel.TapBug,
    answered: Boolean,
    wasCorrect: Boolean,
    settled: Boolean,
    onPick: (Int) -> Unit,
) {
    var selected by remember(quiz) { mutableIntStateOf(-1) }
    val reduceMotion = LocalReduceMotion.current

    // Pulsing mint outline for the bug line when wrong (2 pulses, 300ms each = 600ms total, Item 38f)
    val bugPulse = remember { Animatable(0f) }
    LaunchedEffect(answered) {
        if (answered && !wasCorrect && !reduceMotion) {
            bugPulse.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 600
                    1f at 150
                    0f at 300
                    1f at 450
                    0f at 600
                },
            )
        }
    }

    // Code panel container (Item 38f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CodeCardTokens.Container)
            .border(0.5.dp, CodeCardTokens.Border, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Hint text pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Tap the line with the bug",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DisplayFont,
                    color = CodeCardTokens.Muted,
                )
            }
            Spacer(Modifier.height(4.dp))

            quiz.lines.forEachIndexed { idx, line ->
                val isBug = idx == quiz.buggyLineIndex
                val isSelected = idx == selected
                val interactionSource = remember { MutableInteractionSource() }

                val lineBg = when {
                    answered && isSelected && isBug -> FerrisColors.MintCorrect.copy(alpha = 0.16f)
                    answered && isSelected && !isBug -> CodeCardTokens.Keyword.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
                val pulseBorder = if (answered && !wasCorrect && isBug) {
                    BorderStroke(1.5.dp, FerrisColors.MintCorrect.copy(alpha = bugPulse.value))
                } else null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(interactionSource, pressed = 0.98f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(lineBg)
                        .then(if (pulseBorder != null) Modifier.border(pulseBorder, RoundedCornerShape(8.dp)) else Modifier)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !answered,
                        ) {
                            selected = idx
                            onPick(idx)
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left 3dp accent bar for tapped line
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(
                                    when {
                                        answered && isSelected && isBug -> FerrisColors.MintCorrect
                                        answered && isSelected && !isBug -> CodeCardTokens.Keyword
                                        else -> Color.Transparent
                                    },
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        // Line number gutter (Item 34f/38f)
                        Text(
                            text = (idx + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = CodeFontFamily,
                            color = CodeCardTokens.Muted.copy(alpha = 0.6f),
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.End,
                        )
                        Spacer(Modifier.width(10.dp))
                        // Syntax-highlighted code line
                        Text(
                            text = highlightCode(line.ifBlank { " " }, isDark = true),
                            style = TextStyle(
                                fontFamily = CodeFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FillBlankBody(
    quiz: QuizUiModel.FillBlank,
    answered: Boolean,
    wasCorrect: Boolean,
    onSubmit: (String) -> Unit,
) {
    var text by remember(quiz) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Single code panel for prefix + inline input + suffix (Item 38g)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(CodeCardTokens.Container)
                .border(0.5.dp, CodeCardTokens.Border, MaterialTheme.shapes.medium)
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (quiz.prefix.isNotBlank()) {
                    Text(
                        text = highlightCode(quiz.prefix, isDark = true),
                        style = TextStyle(fontFamily = CodeFontFamily, fontSize = 13.sp, lineHeight = 20.sp),
                    )
                }

                // Glowing bottom accent line on submit (Item 38g)
                val bottomLineColor by animateColorAsState(
                    targetValue = when {
                        answered && wasCorrect -> FerrisColors.MintCorrect
                        answered && !wasCorrect -> MaterialTheme.colorScheme.error
                        text.isNotBlank() -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
                    animationSpec = FerrisMotion.Quick,
                    label = "input-bottom-border",
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (!answered) text = it },
                        enabled = !answered,
                        textStyle = TextStyle(
                            fontFamily = CodeFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = Color.White,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (!answered && text.isNotBlank()) {
                                onSubmit(text)
                            }
                        }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Column {
                                Box(modifier = Modifier.padding(vertical = 6.dp)) {
                                    if (text.isEmpty()) {
                                        Text(
                                            text = "your answer…",
                                            style = TextStyle(
                                                fontFamily = CodeFontFamily,
                                                fontSize = 14.sp,
                                                color = CodeCardTokens.Muted.copy(alpha = 0.6f),
                                            ),
                                        )
                                    }
                                    innerTextField()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(bottomLineColor),
                                )
                            }
                        },
                    )
                }

                if (quiz.suffix.isNotBlank()) {
                    Text(
                        text = highlightCode(quiz.suffix, isDark = true),
                        style = TextStyle(fontFamily = CodeFontFamily, fontSize = 13.sp, lineHeight = 20.sp),
                    )
                }
            }
        }

        // Pinned above keyboard with Modifier.imePadding() (Item 38g)
        FerrisButton(
            text = if (answered) "Answered" else "Check",
            style = FerrisButtonStyle.Filled,
            enabled = !answered && text.isNotBlank(),
            onClick = { onSubmit(text) },
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
    }
}

/** Slide-up glass explanation panel (Item 38d). */
@Composable
private fun ExplanationPanel(
    correct: Boolean,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass(shape)
            .clip(shape)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (correct) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Correct",
                        tint = FerrisColors.MintCorrect,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Correct",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = FerrisColors.MintCorrect,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Not quite",
                        tint = FerrisColors.FerrisAmber,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Not quite — here's why",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = FerrisColors.FerrisAmber,
                    )
                }
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Quiz MCQ dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun QuizMcqDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        QuizCard(
            quiz = QuizUiModel.Mcq(
                question = "What happens when s is passed to takes(s)?",
                options = listOf("s is moved", "s is copied", "s is borrowed", "Nothing"),
                answerIndex = 0,
                explanation = "String is not Copy, so passing by value moves ownership.",
            ),
            onResult = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Quiz MCQ light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun QuizMcqLightPreview() {
    FerrisFeedTheme(darkTheme = false) {
        QuizCard(
            quiz = QuizUiModel.Mcq(
                question = "What happens when s is passed to takes(s)?",
                options = listOf("s is moved", "s is copied", "s is borrowed", "Nothing"),
                answerIndex = 0,
                explanation = "String is not Copy, so passing by value moves ownership.",
            ),
            onResult = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Quiz TapBug dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun QuizTapBugDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        QuizCard(
            quiz = QuizUiModel.TapBug(
                question = "Tap the line that fails to compile.",
                lines = listOf("let s = String::from(\"hi\");", "takes(s);", "println!(\"{s}\");"),
                buggyLineIndex = 2,
                explanation = "s was moved on line 2, so line 3 uses a moved value.",
            ),
            onResult = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Quiz FillBlank dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun QuizFillBlankDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        QuizCard(
            quiz = QuizUiModel.FillBlank(
                question = "Complete the borrow statement.",
                prefix = "fn print(s: ",
                suffix = "String) {}",
                acceptedAnswers = listOf("&"),
                explanation = "A shared reference is written with &.",
            ),
            onResult = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}
