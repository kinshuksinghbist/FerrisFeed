package com.ferrisfeed.coreui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
 * Interactive quiz card. Shows [quiz], grades locally, then reveals the explanation.
 * Emits a haptic tick on correct (LongPress tick) vs incorrect (VirtualKey buzz).
 * Calls [onResult] exactly once per question instance with (correct, selectedLabel).
 */
@Composable
fun QuizCard(
    quiz: QuizUiModel,
    onResult: (correct: Boolean, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var answered by remember(quiz) { mutableStateOf(false) }
    var wasCorrect by remember(quiz) { mutableStateOf(false) }
    var resultSent by remember(quiz) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    fun submit(correct: Boolean, label: String) {
        if (resultSent) return
        resultSent = true
        answered = true
        wasCorrect = correct
        haptics.performHapticFeedback(
            // NB: HapticFeedbackType has no error/deny member across the
            // Compose versions we resolve (BOM + transitive bumps), so the
            // incorrect buzz uses VirtualKey, present since Compose 1.0.
            if (correct) HapticFeedbackType.LongPress else HapticFeedbackType.VirtualKey,
        )
        onResult(correct, label)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Check yourself",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = quiz.question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            when (quiz) {
                is QuizUiModel.Mcq -> McqBody(
                    quiz = quiz,
                    answered = answered,
                    onPick = { idx -> submit(idx == quiz.answerIndex, quiz.options[idx]) },
                )
                is QuizUiModel.TapBug -> TapBugBody(
                    quiz = quiz,
                    answered = answered,
                    onPick = { idx -> submit(idx == quiz.buggyLineIndex, "line ${idx + 1}") },
                )
                is QuizUiModel.FillBlank -> FillBlankBody(
                    quiz = quiz,
                    answered = answered,
                    onSubmit = { text ->
                        val ok = quiz.acceptedAnswers.any { it.equals(text.trim(), ignoreCase = true) }
                        submit(ok, text.trim())
                    },
                )
            }

            if (answered) {
                Spacer(Modifier.height(12.dp))
                ExplanationRow(correct = wasCorrect, explanation = quiz.explanation)
            }
        }
    }
}

@Composable
private fun McqBody(
    quiz: QuizUiModel.Mcq,
    answered: Boolean,
    onPick: (Int) -> Unit,
) {
    var selected by remember(quiz) { mutableIntStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quiz.options.forEachIndexed { idx, option ->
            val isAnswer = idx == quiz.answerIndex
            val isSelected = idx == selected
            val container = when {
                answered && isAnswer -> FerrisColors.MintContainerDark
                answered && isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val border = when {
                answered && isAnswer -> BorderStroke(1.dp, FerrisColors.MintCorrect)
                answered && isSelected -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                else -> null
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !answered) {
                        selected = idx
                        onPick(idx)
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = container),
                border = border,
            ) {
                Text(
                    text = "${'A' + idx}. $option",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (option.contains("(") || option.contains("&")) CodeFontFamily else null,
                )
            }
        }
    }
}

@Composable
private fun TapBugBody(
    quiz: QuizUiModel.TapBug,
    answered: Boolean,
    onPick: (Int) -> Unit,
) {
    var selected by remember(quiz) { mutableIntStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        quiz.lines.forEachIndexed { idx, line ->
            val isBug = idx == quiz.buggyLineIndex
            val container = when {
                answered && isBug -> FerrisColors.MintContainerDark
                answered && idx == selected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !answered) {
                        selected = idx
                        onPick(idx)
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = container),
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "${idx + 1}  ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = CodeFontFamily,
                    )
                    Text(
                        text = line.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = CodeFontFamily,
                    )
                }
            }
        }
        if (!answered) {
            Text(
                text = "Tap the line with the bug.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun FillBlankBody(
    quiz: QuizUiModel.FillBlank,
    answered: Boolean,
    onSubmit: (String) -> Unit,
) {
    var text by remember(quiz) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = quiz.prefix,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = CodeFontFamily,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = !answered,
            label = { Text("your answer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (quiz.suffix.isNotBlank()) {
            Text(
                text = quiz.suffix,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = CodeFontFamily,
            )
        }
        Button(
            onClick = { onSubmit(text) },
            enabled = !answered && text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(if (answered) "Answered" else "Check")
        }
    }
}

@Composable
private fun ExplanationRow(correct: Boolean, explanation: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (correct) FerrisColors.MintContainerDark
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (correct) "Correct ✓ — nice." else "Not quite — here's why:",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Quiz MCQ dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun QuizMcqPreview() {
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

@Preview(name = "Quiz TapBug light", showBackground = true)
@Composable
private fun QuizTapBugPreview() {
    FerrisFeedTheme(darkTheme = false) {
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
