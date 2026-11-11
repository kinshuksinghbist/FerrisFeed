package com.ferrisfeed.coreui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * One lesson, laid out like a page of a good technical book (v3):
 *
 *   ● Rust · Ownership & moves                ▂▄▆ Beginner
 *   Hook — the biggest words on screen
 *   Body — 2-4 sentences with `code` and **bold** rendered inline
 *   [code well with Output / Copy]
 *   ▍KEY IDEA
 *   ▍The takeaway, one line you can repeat in an interview
 *
 * No card container: content sits directly on the page so a 360dp phone
 * gets the full width, and the code well is the only boxed element. The
 * caller owns scrolling (the feed page scrolls only when content overflows).
 */
@Composable
fun LessonContent(
    track: String,
    topicTitle: String?,
    level: Int,
    hook: String,
    body: String,
    takeaway: String,
    code: String?,
    language: String,
    output: String?,
    modifier: Modifier = Modifier,
) {
    val palette = FerrisTheme.palette
    val inkColor = trackTextColor(track)
    val inlineBg = palette.raised
    val parsedBody = remember(body, inlineBg, inkColor) { parseInlineMarkdown(body, inlineBg, inkColor) }
    val bigText = LocalDensity.current.fontScale > 1.3f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackTag(track = track, topicTitle = topicTitle, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            LevelMeter(level = level)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = hook,
            style = if (bigText) MaterialTheme.typography.headlineSmall else FerrisType.Hook,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = parsedBody,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
        )
        if (!code.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))
            CodeBlock(code = code, language = language, output = output)
        }
        if (takeaway.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            KeyIdea(text = takeaway, track = track)
        }
    }
}

/** Left-ruled "Key idea" block: the sentence to remember. */
@Composable
fun KeyIdea(
    text: String,
    track: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(trackColor(track), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
            Overline("Key idea", color = trackTextColor(track))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview(name = "Lesson · dark", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun LessonPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        LessonContent(
            track = Tracks.RUST,
            topicTitle = "Ownership & moves",
            level = 1,
            hook = "let b = a; kills a — the most surprising line for newcomers.",
            body = "Assigning a `String` **moves** it. The old binding is invalid, so the heap buffer has exactly one owner and is freed exactly once.",
            takeaway = "Assignment of heap values moves ownership.",
            code = "let a = String::from(\"hi\");\nlet b = a;\nprintln!(\"{b}\");",
            language = "rust",
            output = "hi",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(name = "Lesson · light", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360)
@Composable
private fun LessonPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        LessonContent(
            track = Tracks.SYSTEM_DESIGN,
            topicTitle = "Caching",
            level = 2,
            hook = "Cache-aside: check Redis before you touch Postgres.",
            body = "Reads hit the cache first. On a miss, load from the database and refill the cache with a **TTL**. Writes delete the key.",
            takeaway = "Read cache, fall back to DB, invalidate on write.",
            code = null,
            language = "sql",
            output = null,
            modifier = Modifier.padding(20.dp),
        )
    }
}
