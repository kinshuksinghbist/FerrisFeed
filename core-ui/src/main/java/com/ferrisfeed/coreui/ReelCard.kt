package com.ferrisfeed.coreui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Redesigned lesson card (P6 34):
 * - Surface: 32dp shape, track gradient background, 0.5dp glass stroke border, inner padding 24dp.
 * - Header: track chip (dot + track label) and three-bar difficulty indicator.
 * - Hook: 28sp Display Bold headlineMedium, onSurface.
 * - Body: inline markdown formatting for `code` and **bold**, bodyLarge.
 * - Takeaway: 3dp vertical accent bar in trackTextColor with 12dp padding, titleSmall.
 * - Contained CodeBlock with line numbers and Code|Output toggle.
 * - Coordinated entrance choreography when settled.
 */
@Composable
fun ReelCard(
    track: String,
    level: Int,
    hook: String,
    body: String,
    takeaway: String,
    code: String? = null,
    language: String = "rust",
    output: String? = null,
    modifier: Modifier = Modifier,
    animateDifficulty: Boolean = true,
    settled: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val takeawayColor = trackTextColor(track)
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current

    val codeBg = if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)
    val codeFg = if (isDark) {
        if (track == Tracks.RUST) Color(0xFFFFB59E) else Color(0xFF8FDCF7)
    } else {
        takeawayColor
    }

    val parsedBody = remember(body, isDark, track) {
        parseInlineMarkdown(body, codeBg, codeFg)
    }

    // Entrance progress animation (plays when settled page, skipped on reduce-motion)
    val progress = remember(settled) {
        Animatable(if (settled && !reduceMotion) 0f else 1f)
    }

    LaunchedEffect(settled) {
        if (settled && !reduceMotion) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 400))
        }
    }

    val p = progress.value
    val headerAlpha = if (reduceMotion) 1f else (p / 0.3f).coerceIn(0f, 1f)
    val hookP = if (reduceMotion) 1f else ((p - 0.15f) / 0.35f).coerceIn(0f, 1f)
    val bodyP = if (reduceMotion) 1f else ((p - 0.30f) / 0.35f).coerceIn(0f, 1f)
    val takeP = if (reduceMotion) 1f else ((p - 0.45f) / 0.35f).coerceIn(0f, 1f)
    val codeP = if (reduceMotion) 1f else ((p - 0.60f) / 0.40f).coerceIn(0f, 1f)

    val hookStyle = if (density.fontScale > 1.3f) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.headlineMedium
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.large, depth = 8.dp)
            .clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(trackBrush(track, isDark))
                .padding(24.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = if (density.fontScale > 1.3f) Modifier.verticalScroll(scrollState) else Modifier,
            ) {
                // Header row: Track chip (left) + Difficulty indicator (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = headerAlpha },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Track chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(trackColor(track).copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(trackColor(track))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = Tracks.label(track),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DisplayFont,
                            color = takeawayColor,
                        )
                    }

                    // Difficulty indicator
                    DifficultyLabel(
                        level = level,
                        animated = animateDifficulty,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Hook
                Text(
                    text = hook,
                    style = hookStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        alpha = hookP
                        translationY = (16.dp * (1f - hookP)).toPx()
                    },
                )

                Spacer(Modifier.height(14.dp))

                // Body with inline markdown
                Text(
                    text = parsedBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.graphicsLayer {
                        alpha = bodyP
                        translationY = (16.dp * (1f - bodyP)).toPx()
                    },
                )

                // Takeaway key point strip with vertical accent bar
                if (takeaway.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = takeP
                                translationY = (16.dp * (1f - takeP)).toPx()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .clip(CircleShape)
                                .background(takeawayColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = takeaway,
                            style = MaterialTheme.typography.titleSmall,
                            color = takeawayColor,
                        )
                    }
                }

                // Contained CodeBlock
                if (!code.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = codeP
                            translationY = (16.dp * (1f - codeP)).toPx()
                        }
                    ) {
                        CodeBlock(
                            code = code,
                            language = language,
                            output = output,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ReelCard rust easy", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ReelCardRustPreview() {
    FerrisFeedTheme(darkTheme = true) {
        ReelCard(
            track = Tracks.RUST,
            level = 1,
            hook = "Why does this simple function not compile?",
            body = "Ownership moves values by default. When you pass a `String` to a function, the caller loses it. Borrow with `&` to keep using it afterwards.",
            takeaway = "Move by default; borrow with & to keep ownership.",
            code = "fn takes(s: &String) {\n    println!(\"{s}\");\n}",
            output = "hi\n",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "ReelCard sysdesign medium", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun ReelCardSysDesignPreview() {
    FerrisFeedTheme(darkTheme = false) {
        ReelCard(
            track = Tracks.SYSTEM_DESIGN,
            level = 2,
            hook = "Cache-aside: check Redis before Postgres",
            body = "Reads hit the fast cache first. On miss, load from Postgres and refill the cache with a **TTL**. Writes delete the cache key to avoid stale reads.",
            takeaway = "Read from cache, fall back to DB, invalidate on write.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Worst case 360x640", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 640)
@Composable
private fun ReelCardWorstCasePreview() {
    FerrisFeedTheme(darkTheme = true) {
        ReelCard(
            track = Tracks.RUST,
            level = 3,
            hook = "Why does this function require an explicit lifetime parameter for output?",
            body = "When returning a reference derived from multiple input arguments, Rust's borrow checker cannot infer which lifetime bounds apply without **explicit lifetime annotations** like `'a` on all references.",
            takeaway = "Elision fails for multiple reference arguments; annotate 'a.",
            code = "fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {\n    if x.len() > y.len() { x } else { y }\n}\nfn main() {\n    let a = \"hello\";\n    let b = \"world\";\n    println!(\"{}\", longest(a, b));\n}",
            output = "hello\n",
            modifier = Modifier.padding(16.dp),
        )
    }
}
