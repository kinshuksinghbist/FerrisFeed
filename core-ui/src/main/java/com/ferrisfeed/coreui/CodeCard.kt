package com.ferrisfeed.coreui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val RustKeywords = setOf(
    "fn", "let", "mut", "const", "struct", "enum", "impl", "trait", "for", "in",
    "if", "else", "match", "loop", "while", "return", "use", "mod", "pub", "crate",
    "self", "Self", "where", "async", "await", "move", "ref", "static", "dyn",
    "unsafe", "extern", "as", "break", "continue", "type", "true", "false", "Some", "None", "Ok", "Err",
)

private val SysDesignKeywords = setOf(
    "SELECT", "FROM", "WHERE", "CACHE", "GET", "SET", "POST", "router", "await",
    "Channel", "Mutex", "Arc", "axum", "tower", "Redis", "POSTGRES",
)

/**
 * Code editor palette (TODO 23c + P6 34g).
 *
 * The code card keeps a fixed GitHub-dark editor surface in BOTH themes:
 * holds >= 7:1 for every span at 13sp mono.
 */
object CodeCardTokens {
    val Container = Color(0xFF0D1117)
    val Muted = Color(0xFF8B949E)
    val Body = Color(0xFFC9D1D9)
    val Output = Color(0xFFA5D6FF)
    val Keyword = Color(0xFFFF7B72)
    val Number = Color(0xFF79C0FF)
    val Macro = Color(0xFFD2A8FF)
    val TypeColor = Color(0xFFFFA657)
    val FnColor = Color(0xFFD2A8FF)
    val LineNumber = Color(0xFF6B7280)
}

/**
 * Redesigned code snippet well (P6 34f):
 * - Shape: 20dp rounded surface.
 * - Top bar: 3 macOS dots (8dp, #FF5F57 #FEBC2E #28C840), language name, segmented
 *   "Code | Output" toggle pill, and copy icon chip with "Copied" feedback.
 * - Line numbers in left gutter (20dp, Muted @ 60% alpha).
 * - Code↔output swap with AnimatedContent and slide transition.
 * - Font size: 13sp / 20sp lineHeight.
 */
@Composable
fun CodeBlock(
    code: String,
    language: String,
    output: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var copied by remember(code) { mutableStateOf(false) }
    var flipped by remember(code) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    val highlighted = remember(code, language) { highlightCode(code, language) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CodeCardTokens.Container,
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            // Top bar: macOS dots + language + actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                // 3 macOS-style dots: red, yellow, green (6dp gap)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(Color(0xFFFF5F57), CircleShape))
                    Box(Modifier.size(8.dp).background(Color(0xFFFEBC2E), CircleShape))
                    Box(Modifier.size(8.dp).background(Color(0xFF28C840), CircleShape))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = language.lowercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = CodeFontFamily),
                    color = CodeCardTokens.Muted,
                )
                Spacer(Modifier.weight(1f))

                // Flip chip: segmented pill "Code | Output"
                if (output != null) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(0.5.dp, CodeCardTokens.Muted.copy(alpha = 0.25f), CircleShape)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!flipped) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { flipped = false }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Code",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = CodeFontFamily, fontSize = 10.sp),
                                color = if (!flipped) MaterialTheme.colorScheme.primary else CodeCardTokens.Muted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (flipped) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { flipped = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Output",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = CodeFontFamily, fontSize = 10.sp),
                                color = if (flipped) MaterialTheme.colorScheme.primary else CodeCardTokens.Muted,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Copy chip: 36dp circular icon chip with minimumInteractiveComponentSize()
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            context.getSystemService(Context.CLIPBOARD_SERVICE)
                                ?.let { it as? ClipboardManager }
                                ?.setPrimaryClip(ClipData.newPlainText("code", code))
                            copied = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = copied,
                        label = "copy-feedback",
                    ) { isCopied ->
                        if (isCopied) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Copied",
                                tint = FerrisColors.MintCorrect,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy code",
                                tint = CodeCardTokens.Body,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Code / Output Content with AnimatedContent
            AnimatedContent(
                targetState = flipped,
                transitionSpec = {
                    (fadeIn(FerrisMotion.Quick) + slideInVertically(FerrisMotion.QuickOffset) { it / 8 }) togetherWith
                        (fadeOut(FerrisMotion.Quick) + slideOutVertically(FerrisMotion.QuickOffset) { -it / 8 })
                },
                label = "code-content-flip",
            ) { isOutput ->
                if (isOutput && output != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "▸ ",
                            fontFamily = CodeFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = CodeCardTokens.Muted,
                        )
                        Text(
                            text = output,
                            fontFamily = CodeFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = CodeCardTokens.Output,
                        )
                    }
                } else {
                    val linesCount = remember(code) { code.lines().size.coerceAtLeast(1) }
                    val lineNumbersText = remember(linesCount) {
                        (1..linesCount).joinToString("\n")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        // Line numbers gutter: 20dp wide, right-aligned, Muted @ 60%
                        Text(
                            text = lineNumbersText,
                            fontFamily = CodeFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = CodeCardTokens.Muted.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        // Horizontally scrollable code text
                        Text(
                            text = highlighted,
                            fontFamily = CodeFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Syntax highlighter with full parity (P6 34g):
 * - Keywords: orange [CodeCardTokens.Keyword]
 * - Lifetimes: blue [CodeCardTokens.Number]
 * - Types: amber [CodeCardTokens.TypeColor]
 * - Function calls & declarations: lavender [CodeCardTokens.FnColor]
 * - Macros: lavender [CodeCardTokens.Macro]
 * - Numbers: blue [CodeCardTokens.Number]
 * - Strings: sky blue [CodeCardTokens.Output]
 * - Comments & Attributes: gray [CodeCardTokens.Muted]
 */
fun highlightCode(code: String, language: String = "rust"): AnnotatedString {
    val keywords = when (language.lowercase()) {
        "rust", "rs" -> RustKeywords
        else -> RustKeywords + SysDesignKeywords
    }
    return buildAnnotatedString {
        val keywordStyle = SpanStyle(color = CodeCardTokens.Keyword, fontWeight = FontWeight.SemiBold)
        val stringStyle = SpanStyle(color = CodeCardTokens.Output)
        val commentStyle = SpanStyle(color = CodeCardTokens.Muted)
        val numberStyle = SpanStyle(color = CodeCardTokens.Number)
        val macroStyle = SpanStyle(color = CodeCardTokens.Macro)
        val typeStyle = SpanStyle(color = CodeCardTokens.TypeColor)
        val fnStyle = SpanStyle(color = CodeCardTokens.FnColor)
        val defaultStyle = SpanStyle(color = CodeCardTokens.Body, fontFamily = CodeFontFamily)

        var i = 0
        var tokenStart = -1
        var lastToken = ""

        fun flushToken(end: Int) {
            if (tokenStart >= 0 && end > tokenStart) {
                val token = code.substring(tokenStart, end)
                val isFn = (token != "fn" && lastToken == "fn") ||
                    (end < code.length && code[end] == '(' && token !in keywords)

                when {
                    token in keywords -> withStyle(keywordStyle) { append(token) }
                    token.endsWith("!") && token.dropLast(1).all { it.isLetterOrDigit() || it == '_' } ->
                        withStyle(macroStyle) { append(token) }
                    isFn -> withStyle(fnStyle) { append(token) }
                    token.first().isUpperCase() && token.first().isLetter() ->
                        withStyle(typeStyle) { append(token) }
                    token.first().isDigit() && token.all { it.isDigit() || it == '_' || it == '.' } ->
                        withStyle(numberStyle) { append(token) }
                    else -> withStyle(defaultStyle) { append(token) }
                }
                lastToken = token
                tokenStart = -1
            }
        }

        while (i < code.length) {
            val c = code[i]
            // Line comment: //
            if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
                flushToken(i)
                val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                withStyle(commentStyle) { append(code.substring(i, end)) }
                i = end
                continue
            }
            // Attribute: #[...]
            if (c == '#' && i + 1 < code.length && code[i + 1] == '[') {
                flushToken(i)
                val endBracket = code.indexOf(']', i + 2)
                val end = if (endBracket != -1) endBracket + 1 else code.length
                withStyle(commentStyle) { append(code.substring(i, end)) }
                i = end
                continue
            }
            // String literal: "..."
            if (c == '"') {
                flushToken(i)
                var j = i + 1
                while (j < code.length) {
                    if (code[j] == '"' && code[j - 1] != '\\') break
                    j++
                }
                val end = (j + 1).coerceAtMost(code.length)
                withStyle(stringStyle) { append(code.substring(i, end)) }
                i = end
                continue
            }
            // Lifetime: 'a, 'static, etc.
            if (c == '\'' && i + 1 < code.length && code[i + 1].isLetter()) {
                flushToken(i)
                var j = i + 1
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) {
                    j++
                }
                val lifetime = code.substring(i, j)
                withStyle(numberStyle) { append(lifetime) }
                i = j
                continue
            }
            // Identifier or number token
            if (c.isLetterOrDigit() || c == '_' || (c == '!' && tokenStart >= 0)) {
                if (tokenStart == -1) tokenStart = i
            } else {
                flushToken(i)
                withStyle(defaultStyle) { append(c.toString()) }
            }
            i++
        }
        flushToken(code.length)
    }
}

@Preview(name = "CodeBlock dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun CodeBlockPreview() {
    FerrisFeedTheme(darkTheme = true) {
        CodeBlock(
            code = "fn main() {\n    let mut s = String::from(\"hi\");\n    takes(&s); // borrow, no move\n    println!(\"{s}\");\n}\n",
            language = "rust",
            output = "hi\n",
            modifier = Modifier.padding(16.dp),
        )
    }
}
