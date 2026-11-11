package com.ferrisfeed.coreui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

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

    /** Tap-the-bug: the learner picks the buggy line. */
    data class TapBug(
        override val question: String,
        val lines: List<String>,
        val buggyLineIndex: Int,
        override val explanation: String,
    ) : QuizUiModel

    /** Fill-blank: one blank, case-insensitive match against [acceptedAnswers]. */
    data class FillBlank(
        override val question: String,
        val prefix: String,
        val suffix: String,
        val acceptedAnswers: List<String>,
        override val explanation: String,
    ) : QuizUiModel

    /** Assemble code by tapping the blocks in order; any wrong tap counts as a miss. */
    data class Blocks(
        override val question: String,
        val prefix: String = "",
        val suffix: String = "",
        val targetBlocks: List<String>,
        val distractorBlocks: List<String> = emptyList(),
        override val explanation: String,
    ) : QuizUiModel
}

/** A graded answer: what the learner chose and whether it was right. */
data class QuizResult(
    val correct: Boolean,
    val choice: String,
)

/**
 * Deterministic option order for a reel. Authored content skews the correct
 * answer toward the first positions, so every reel gets a stable shuffle
 * keyed by its id: the same reel always shows the same order (swiping back
 * never reshuffles), different reels spread the answer evenly.
 */
fun shuffledOrder(size: Int, seed: Int): List<Int> = (0 until size).shuffled(Random(seed))

/**
 * The quiz page body (v3). Question, answer controls and, once answered, a
 * feedback panel that teaches: verdict + XP, the explanation, and the
 * reel's "common trap". Grading happens exactly once per reel; [result]
 * restores a previous answer so swiping back shows it instead of re-asking.
 */
@Composable
fun QuizContent(
    quiz: QuizUiModel,
    seed: Int,
    result: QuizResult?,
    onAnswer: (QuizResult) -> Unit,
    modifier: Modifier = Modifier,
    trap: String? = null,
    xpCorrect: Int = 15,
    xpWrong: Int = 5,
) {
    val haptics = LocalHapticFeedback.current
    fun submit(r: QuizResult) {
        if (result != null) return
        haptics.performHapticFeedback(if (r.correct) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
        onAnswer(r)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = quiz.question,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        when (quiz) {
            is QuizUiModel.Mcq -> McqOptions(quiz, seed, result, onPick = ::submit)
            is QuizUiModel.TapBug -> TapBugLines(quiz, result, onPick = ::submit)
            is QuizUiModel.FillBlank -> FillBlankInput(quiz, result, onSubmit = ::submit)
            is QuizUiModel.Blocks -> BlocksBuilder(quiz, seed, result, onDone = ::submit)
        }
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn(FerrisMotion.Smooth) + expandVertically(),
        ) {
            if (result != null) {
                FeedbackPanel(
                    correct = result.correct,
                    explanation = quiz.explanation,
                    trap = trap,
                    xp = if (result.correct) xpCorrect else xpWrong,
                )
            }
        }
    }
}

@Composable
private fun McqOptions(
    quiz: QuizUiModel.Mcq,
    seed: Int,
    result: QuizResult?,
    onPick: (QuizResult) -> Unit,
) {
    val order = remember(quiz, seed) { shuffledOrder(quiz.options.size, seed) }
    val answered = result != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEachIndexed { position, optionIdx ->
            val option = quiz.options[optionIdx]
            val isAnswer = optionIdx == quiz.answerIndex
            val isChosen = answered && result?.choice == option
            OptionRow(
                letter = ('A' + position).toString(),
                text = option,
                state = when {
                    !answered -> OptionState.Idle
                    isAnswer -> OptionState.Correct
                    isChosen -> OptionState.Wrong
                    else -> OptionState.Dimmed
                },
                enabled = !answered,
                onClick = { onPick(QuizResult(correct = isAnswer, choice = option)) },
            )
        }
    }
}

private enum class OptionState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun OptionRow(
    letter: String,
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    mono: Boolean = false,
) {
    val palette = FerrisTheme.palette
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        targetValue = when (state) {
            OptionState.Correct -> palette.successSoft
            OptionState.Wrong -> palette.dangerSoft
            else -> scheme.surface
        },
        animationSpec = FerrisMotion.QuickColor,
        label = "option-bg",
    )
    val borderColor = when (state) {
        OptionState.Correct -> palette.success
        OptionState.Wrong -> palette.danger
        else -> palette.hairline
    }
    val alpha by animateFloatAsState(if (state == OptionState.Dimmed) 0.45f else 1f, FerrisMotion.Quick, label = "option-alpha")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == OptionState.Wrong && !reduceMotion) {
            shake.animateTo(0f, keyframes {
                durationMillis = 300
                -7f at 50
                7f at 110
                -4f at 170
                4f at 230
                0f at 300
            })
        }
    }
    val parsed = remember(text, palette.raised) { parseInlineMarkdown(text, palette.raised, scheme.onSurface) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationX = shake.value * density
            }
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(if (state == OptionState.Idle || state == OptionState.Dimmed) 1.dp else 1.5.dp, borderColor), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                if (state == OptionState.Correct) stateDescription = "Correct answer"
                if (state == OptionState.Wrong) {
                    stateDescription = "Your answer, incorrect"
                    liveRegion = LiveRegionMode.Polite
                }
            }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badgeBg = when (state) {
            OptionState.Correct -> palette.success
            OptionState.Wrong -> palette.danger
            else -> palette.raised
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                OptionState.Correct -> Icon(Icons.Filled.Check, null, tint = scheme.surface, modifier = Modifier.size(16.dp))
                OptionState.Wrong -> Icon(Icons.Filled.Close, null, tint = scheme.surface, modifier = Modifier.size(16.dp))
                else -> Text(letter, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = parsed,
            style = if (mono) {
                TextStyle(fontFamily = CodeFontFamily, fontSize = 14.sp, lineHeight = 20.sp)
            } else {
                MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)
            },
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TapBugLines(
    quiz: QuizUiModel.TapBug,
    result: QuizResult?,
    onPick: (QuizResult) -> Unit,
) {
    val answered = result != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quiz.lines.forEachIndexed { idx, line ->
            val label = "line ${idx + 1}"
            val isBug = idx == quiz.buggyLineIndex
            val chosen = result?.choice == label
            OptionRow(
                letter = (idx + 1).toString(),
                text = line,
                mono = true,
                state = when {
                    !answered -> OptionState.Idle
                    isBug -> OptionState.Correct
                    chosen -> OptionState.Wrong
                    else -> OptionState.Dimmed
                },
                enabled = !answered,
                onClick = { onPick(QuizResult(correct = isBug, choice = label)) },
            )
        }
    }
}

@Composable
private fun FillBlankInput(
    quiz: QuizUiModel.FillBlank,
    result: QuizResult?,
    onSubmit: (QuizResult) -> Unit,
) {
    var text by remember(quiz) { mutableStateOf(result?.choice.orEmpty()) }
    fun check() {
        val guess = text.trim()
        if (guess.isEmpty()) return
        onSubmit(QuizResult(quiz.acceptedAnswers.any { it.trim().equals(guess, ignoreCase = true) }, guess))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (quiz.prefix.isNotBlank() || quiz.suffix.isNotBlank()) {
            Text(
                text = "${quiz.prefix} ____ ${quiz.suffix}".trim(),
                style = TextStyle(fontFamily = CodeFontFamily, fontSize = 14.sp, lineHeight = 20.sp),
                color = CodeCardTokens.Body,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CodeCardTokens.Container)
                    .padding(14.dp),
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { if (result == null) text = it },
            enabled = result == null,
            singleLine = true,
            label = { Text("Your answer") },
            textStyle = TextStyle(fontFamily = CodeFontFamily, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { check() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (result == null) {
            FerrisButton(text = "Check", onClick = { check() }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth())
        } else if (!result.correct) {
            Text(
                text = "Accepted: ${quiz.acceptedAnswers.joinToString(" · ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BlocksBuilder(
    quiz: QuizUiModel.Blocks,
    seed: Int,
    result: QuizResult?,
    onDone: (QuizResult) -> Unit,
) {
    val palette = FerrisTheme.palette
    val haptics = LocalHapticFeedback.current
    val tray = remember(quiz, seed) { (quiz.targetBlocks + quiz.distractorBlocks).shuffled(Random(seed)) }
    val placed = remember(quiz) {
        mutableStateListOf<String>().apply { if (result != null) addAll(quiz.targetBlocks) }
    }
    var misses by remember(quiz) { mutableIntStateOf(0) }
    var lastMiss by remember(quiz) { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val assembled = buildString {
            if (quiz.prefix.isNotBlank()) append(quiz.prefix).append(' ')
            placed.forEach { append(it).append(' ') }
            if (placed.size < quiz.targetBlocks.size) append("▁▁▁ ")
            if (quiz.suffix.isNotBlank()) append(quiz.suffix)
        }.trim()
        Text(
            text = assembled,
            style = TextStyle(fontFamily = CodeFontFamily, fontSize = 15.sp, lineHeight = 22.sp),
            color = CodeCardTokens.Body,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CodeCardTokens.Container)
                .padding(16.dp),
        )
        if (result == null) {
            Text(
                text = if (lastMiss != null) "“$lastMiss” doesn't go there. Try another block." else "Tap the blocks in order.",
                style = MaterialTheme.typography.bodySmall,
                color = if (lastMiss != null) palette.dangerInk else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            val remaining = remember(placed.size, tray) {
                val used = placed.toMutableList()
                tray.filter { block -> !used.remove(block) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                remaining.forEach { block ->
                    Text(
                        text = block,
                        style = TextStyle(fontFamily = CodeFontFamily, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) {
                                val expected = quiz.targetBlocks.getOrNull(placed.size)
                                if (block == expected) {
                                    placed.add(block)
                                    lastMiss = null
                                    if (placed.size == quiz.targetBlocks.size) {
                                        onDone(QuizResult(correct = misses == 0, choice = placed.joinToString(" ")))
                                    }
                                } else {
                                    misses++
                                    lastMiss = block
                                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                }
                            }
                            .defaultMinSize(minHeight = 44.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackPanel(
    correct: Boolean,
    explanation: String,
    trap: String?,
    xp: Int,
) {
    val palette = FerrisTheme.palette
    val ink = if (correct) palette.successInk else palette.dangerInk
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (correct) palette.successSoft else palette.dangerSoft, shape)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (correct) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (correct) "Correct" else "Not quite",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+$xp XP",
                style = MaterialTheme.typography.labelLarge.merge(NumberStyle),
                color = palette.xp,
            )
        }
        if (explanation.isNotBlank()) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (!trap.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Overline("Common trap", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = trap,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Quiz · answered", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 760)
@Composable
private fun QuizPreviewAnswered() {
    FerrisFeedTheme(darkTheme = true) {
        QuizContent(
            quiz = QuizUiModel.Mcq(
                question = "What happens to `a` after `let b = a;` when `a` is a String?",
                options = listOf("It is moved and can no longer be used", "It is copied bit for bit", "It becomes a reference to b", "It is dropped immediately"),
                answerIndex = 0,
                explanation = "String owns heap memory, so assignment moves ownership to b and invalidates a.",
            ),
            seed = 7,
            result = QuizResult(correct = false, choice = "It is copied bit for bit"),
            onAnswer = {},
            trap = "Assuming every type is Copy like integers are.",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(name = "Quiz · fresh light", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360)
@Composable
private fun QuizPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        QuizContent(
            quiz = QuizUiModel.Mcq(
                question = "Where should a read check first in cache-aside?",
                options = listOf("The cache", "The primary database", "A read replica", "The CDN edge"),
                answerIndex = 0,
                explanation = "Cache-aside reads the cache first and only falls back to the database on a miss.",
            ),
            seed = 3,
            result = null,
            onAnswer = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
