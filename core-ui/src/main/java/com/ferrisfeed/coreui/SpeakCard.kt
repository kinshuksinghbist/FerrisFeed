package com.ferrisfeed.coreui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Speaker-opening states for the bottom dock card (P6 Item 37).
 */
sealed interface SpeakState {
    /** Resting: invite one tap to speak the hook aloud. */
    data object Prompt : SpeakState

    /** Capture in flight (permission granted, recognizer listening with level 0..1). */
    data class Listening(val level: Float = 0f) : SpeakState

    /** Recognizer returned text — the reward state. */
    data class Heard(val transcript: String) : SpeakState

    /** Recognizer unavailable / failed / denied — content stays readable. */
    data class Unavailable(val reason: String) : SpeakState
}

/**
 * Redesigned speaker prompt bottom dock card (P6 Item 37).
 * Anchored above LocalBottomBarInset without shifting card content.
 * Glass capsule with soft pulse ring on mic, 5-bar RMS audio visualizer,
 * pop check on heard, and auto-collapse after 2.5s.
 */
@Composable
fun SpeakCard(
    state: SpeakState,
    phrase: String,
    onSpeak: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glass(shape)
            .clip(shape)
            .animateContentSize(animationSpec = FerrisMotion.SmoothSize)
            .semantics { contentDescription = "Speak to start" }
            .padding(16.dp),
    ) {
        when (state) {
            is SpeakState.Prompt -> PromptContent(
                onSpeak = onSpeak,
                onDismiss = onDismiss,
            )

            is SpeakState.Listening -> ListeningContent(
                level = state.level,
            )

            is SpeakState.Heard -> HeardContent(
                transcript = state.transcript,
                onRetry = onRetry,
            )

            is SpeakState.Unavailable -> UnavailableContent(
                reason = state.reason,
                onRetry = onRetry,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun PromptContent(
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val darkPrimary = lerp(primaryColor, Color.Black, 0.12f)
    val micGradient = Brush.verticalGradient(listOf(primaryColor, darkPrimary))

    // Soft pulse ring (expanding 56dp -> 72dp and fading, 1600ms infinite)
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseProgress by if (reduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "PulseProgress",
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 56dp circular mic button with pulse ring
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Expanding soft pulse ring
                if (!reduceMotion) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val currentRadius = (28.dp.toPx()) + (8.dp.toPx() * pulseProgress)
                        val ringAlpha = (1f - pulseProgress).coerceIn(0f, 0.6f)
                        drawCircle(
                            color = primaryColor.copy(alpha = ringAlpha),
                            radius = currentRadius,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }

                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .pressScale(interactionSource, pressed = 0.94f)
                        .clip(CircleShape)
                        .background(micGradient)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onSpeak,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speak the hook aloud",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Say it first",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Read the headline aloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }

        FerrisButton(
            text = "Skip",
            style = FerrisButtonStyle.Ghost,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ListeningContent(
    level: Float,
) {
    val reduceMotion = LocalReduceMotion.current
    val smoothLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = FerrisMotion.Snappy,
        label = "smoothLevel",
    )

    // 5-bar audio-level visualizer (Item 37c)
    val infiniteTransition = rememberInfiniteTransition(label = "IdleBar")
    val idlePhase by if (reduceMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "IdlePhase",
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 5-bar visualizer
        Canvas(modifier = Modifier.size(width = 46.dp, height = 28.dp)) {
            val barWidth = 6.dp.toPx()
            val gap = 4.dp.toPx()
            val maxHeight = 28.dp.toPx()
            val minHeight = 4.dp.toPx()
            val barMultipliers = floatArrayOf(0.4f, 0.8f, 1.0f, 0.7f, 0.45f)
            val barColor = FerrisColors.FerrisOrange

            for (i in 0..4) {
                val x = i * (barWidth + gap)
                val targetH = if (smoothLevel > 0.05f) {
                    (minHeight + (maxHeight - minHeight) * barMultipliers[i] * smoothLevel)
                } else if (!reduceMotion) {
                    val wave = (kotlin.math.sin(idlePhase + i * 0.8f) + 1f) / 2f
                    minHeight + (maxHeight * 0.45f - minHeight) * wave
                } else {
                    minHeight + (maxHeight * 0.3f) * barMultipliers[i]
                }
                val y = (size.height - targetH) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, targetH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Listening… read the headline",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Speak clearly into the microphone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun HeardContent(
    transcript: String,
    onRetry: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val checkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) FerrisMotion.Quick else FerrisMotion.Bouncy,
        label = "checkScale",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FerrisColors.MintCorrect.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Heard",
                    tint = FerrisColors.MintCorrect,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(checkScale),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Heard you",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "\u201C$transcript\u201D",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                )
            }
        }

        FerrisIconButton(
            icon = Icons.Filled.Refresh,
            contentDescription = "Try again",
            onClick = onRetry,
            size = 40.dp,
        )
    }
}

@Composable
private fun UnavailableContent(
    reason: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = "Microphone unavailable",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice off — reading works the same",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = DisplayFont,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FerrisButton(
                text = "Hide",
                style = FerrisButtonStyle.Ghost,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(8.dp))
            FerrisButton(
                text = "Retry",
                style = FerrisButtonStyle.Tonal,
                onClick = onRetry,
            )
        }
    }
}

/** The phrase the learner speaks: the hook itself (no schema work, TODO 22d). */
fun speakPhraseFor(hook: String): String = hook

@Preview(name = "Speak prompt dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SpeakPromptDarkPreview() {
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

@Preview(name = "Speak listening dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SpeakListeningDarkPreview() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp)) {
            SpeakCard(
                state = SpeakState.Listening(level = 0.7f),
                phrase = "Why does this function not compile?",
                onSpeak = {},
                onRetry = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Speak heard light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun SpeakHeardLightPreview() {
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
