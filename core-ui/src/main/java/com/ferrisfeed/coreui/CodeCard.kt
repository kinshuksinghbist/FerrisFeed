package com.ferrisfeed.coreui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
 * Code editor palette. The editor keeps a fixed dark surface in BOTH themes
 * (code reads as code, and every span holds >= 7:1 at 13sp mono).
 */
object CodeCardTokens {
    val Container = Color(0xFF0D1117)
    val Header = Color(0xFF161B22)
    val Divider = Color(0xFF21262D)
    val Muted = Color(0xFF8B949E)
    val Body = Color(0xFFC9D1D9)
    val Output = Color(0xFFA5D6FF)
    val Keyword = Color(0xFFFF7B72)
    val Number = Color(0xFF79C0FF)
    val Macro = Color(0xFFD2A8FF)
    val TypeColor = Color(0xFFFFA657)
    val FnColor = Color(0xFFD2A8FF)
    val LineNumber = Color(0xFF6E7681)
    val Success = Color(0xFF7EE787)
}

private val CodeShape = RoundedCornerShape(14.dp)
private val CodeText = TextStyle(fontFamily = CodeFontFamily, fontSize = 13.sp, lineHeight = 20.sp)

/**
 * Code well (v3). One quiet header: language on the left, then two text
 * actions on the right: "Output" flips to the expected program output (hidden
 * when the reel has none) and "Copy". Line numbers sit in their own column so
 * copying yields clean code. Long lines scroll horizontally inside the well;
 * nothing ever wraps mid-token.
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
    var showOutput by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }
    val highlighted = remember(code, language) { highlightCode(code.trimEnd(), language) }
    val lineCount = remember(code) { code.trimEnd().lines().size }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CodeShape)
            .background(CodeCardTokens.Container, CodeShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeCardTokens.Header)
                .padding(start = 14.dp, end = 4.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (showOutput) "output" else language.lowercase(),
                style = CodeText.copy(fontSize = 12.sp),
                color = CodeCardTokens.Muted,
            )
            Spacer(Modifier.weight(1f))
            if (output != null) {
                CodeAction(
                    label = if (showOutput) "Code" else "Output",
                    icon = if (showOutput) Icons.Filled.Code else Icons.Filled.PlayArrow,
                    tint = if (showOutput) CodeCardTokens.Muted else CodeCardTokens.Success,
                    description = if (showOutput) "Show code" else "Show program output",
                    onClick = { showOutput = !showOutput },
                )
            }
            CodeAction(
                label = if (copied) "Copied" else "Copy",
                icon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                tint = if (copied) CodeCardTokens.Success else CodeCardTokens.Muted,
                description = "Copy code",
                onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("code", code))
                    copied = true
                },
            )
        }
        AnimatedContent(
            targetState = showOutput,
            transitionSpec = { fadeIn(FerrisMotion.Quick) togetherWith fadeOut(FerrisMotion.Quick) },
            label = "code-output",
        ) { outputMode ->
            if (outputMode && output != null) {
                Text(
                    text = output.trimEnd().ifBlank { "(no output)" },
                    style = CodeText,
                    color = CodeCardTokens.Output,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(
                        text = (1..lineCount).joinToString("\n"),
                        style = CodeText,
                        color = CodeCardTokens.LineNumber,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 10.dp, end = 12.dp).width(if (lineCount >= 10) 18.dp else 10.dp),
                    )
                    Text(
                        text = highlighted,
                        style = CodeText,
                        color = CodeCardTokens.Body,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(end = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = tint)
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

@Preview(name = "CodeBlock", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun CodeBlockPreview() {
    FerrisFeedTheme(darkTheme = true) {
        CodeBlock(
            code = "fn main() {\n    let s = String::from(\"hi\");\n    takes(&s); // borrow, no move\n    println!(\"{s}\");\n}\n",
            language = "rust",
            output = "hi\n",
            modifier = Modifier.padding(16.dp),
        )
    }
}
