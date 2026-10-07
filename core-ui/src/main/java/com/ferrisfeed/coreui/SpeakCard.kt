package com.ferrisfeed.coreui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Speaker-opening prompt for the redesigned reel card (TODO 24).
 *
 * Interaction spec (`reel-card-composition` + `micro-interaction-spec`):
 * phrase prompt -> capture gesture -> recognized-text reveal -> hook + body
 * -> code -> quiz. The lesson text is ALWAYS composed (capture never blocks
 * readiness, `doherty-threshold`); this row only gates the [DifficultyLabel]
 * reward motion and the spoken-retrieval warm-up, never the content.
 *
 * Motion budget (docs/motion.md): expand/collapse via
 * `animateContentSize(tween(300))`, no other animation here. No inner
 * scroll — this is a fixed-height header row.
 *
 * Micro-copy (`ux-writing`): one verb per state, no exclamation, no
 * skeleton loops. Empty/retry shapes reuse the feed empty-state pattern:
 * name the cause, offer one action.
 */
sealed interface SpeakState {
    /** Resting: invite one tap to speak the hook aloud. */
    data object Prompt : SpeakState

    /** Capture in flight (permission granted, recognizer listening). */
    data object Listening : SpeakState

    /** Recognizer returned text — the reward state. */
    data class Heard(val transcript: String) : SpeakState

    /** Recognizer unavailable / failed / denied — content stays readable. */
    data class Unavailable(val reason: String) : SpeakState
}

@Composable
fun SpeakCard(
    state: SpeakState,
    phrase: String,
    onSpeak: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300))
            .semantics { contentDescription = "Speak to start" },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            when (state) {
                is SpeakState.Prompt -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Speak the hook aloud",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Say it first",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Tap the mic and read the headline aloud. Reading still works if you skip.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Skip")
                        }
                    }
                }

                is SpeakState.Listening -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Listening",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Listening… read the headline",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                is SpeakState.Heard -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Heard",
                            tint = FerrisColors.MintCorrect,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Heard you",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "\u201C${state.transcript}\u201D",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            )
                        }
                        IconButton(onClick = onRetry, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Try again",
                            )
                        }
                    }
                }

                is SpeakState.Unavailable -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Voice off — reading works the same",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Hide")
                        }
                    }
                }
            }
        }
    }
}

/** The phrase the learner speaks: the hook itself (no schema work, TODO 22d). */
fun speakPhraseFor(hook: String): String = hook

@Preview(name = "Speak prompt", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SpeakPromptPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp)) {
            SpeakCard(
                state = SpeakState.Prompt,
                phrase = "Why does this function not compile?",
                onSpeak = {},
                onRetry = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Speak heard", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun SpeakHeardPreview() {
    FerrisFeedTheme(darkTheme = false) {
        Column(Modifier.padding(16.dp)) {
            SpeakCard(
                state = SpeakState.Heard("Why does this function not compile"),
                phrase = "Why does this function not compile?",
                onSpeak = {},
                onRetry = {},
                onDismiss = {},
            )
        }
    }
}
