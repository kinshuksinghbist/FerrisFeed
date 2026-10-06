package com.ferrisfeed.coreui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * Code snippet card with lightweight syntax highlighting (regex/token based, no tree-sitter
 * on device), copy button, font-size slider, and a Run stub.
 *
 * The Run button invokes [onRun] with the raw code. The default playground wiring
 * (embedded WASM interpreter for beginner snippets, Rust Playground link for advanced)
 * is implemented by the caller in :feature-feed — this card only provides the affordance
 * and the [runLabel]/[runEnabled] states.
 */
@Composable
fun CodeCard(
    code: String,
    language: String,
    onRun: (String) -> Unit,
    modifier: Modifier = Modifier,
    runLabel: String = "Run",
    runEnabled: Boolean = true,
    initialFontSizeSp: Float = 13f,
) {
    val clipboard = LocalClipboardManager.current
    var fontSizeSp by remember { mutableFloatStateOf(initialFontSizeSp) }
    var copied by remember { mutableStateOf(false) }
    val highlighted = remember(code, language) { highlightCode(code, language) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: language + copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = language.lowercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = CodeFontFamily),
                    color = Color(0xFF8B949E),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (copied) "Copied!" else "${code.lines().size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) FerrisColors.MintCorrect else Color(0xFF8B949E),
                )
                IconButton(onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
                    copied = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFC9D1D9),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Code body with horizontal scroll for long lines.
            Text(
                text = highlighted,
                fontFamily = CodeFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 6).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Color.Transparent)
                    .padding(vertical = 6.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Font-size slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A-", color = Color(0xFF8B949E), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = fontSizeSp,
                    onValueChange = { fontSizeSp = it },
                    valueRange = 10f..20f,
                    steps = 5,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text("A+", color = Color(0xFF8B949E), style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onRun(code) },
                    enabled = runEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(runLabel)
                }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (copied) "Copied ✓" else "Copy")
                }
            }
        }
    }
}

/** Simplified highlighter: keywords orange, strings green, comments gray, numbers blue. */
fun highlightCode(code: String, language: String): AnnotatedString {
    val keywords = when (language.lowercase()) {
        "rust", "rs" -> RustKeywords
        else -> RustKeywords + SysDesignKeywords
    }
    return buildAnnotatedString {
        val keywordStyle = SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.SemiBold)
        val stringStyle = SpanStyle(color = Color(0xFFA5D6FF))
        val commentStyle = SpanStyle(color = Color(0xFF8B949E))
        val numberStyle = SpanStyle(color = Color(0xFF79C0FF))
        val macroStyle = SpanStyle(color = Color(0xFFD2A8FF))
        val defaultStyle = SpanStyle(color = Color(0xFFC9D1D9), fontFamily = CodeFontFamily)

        var i = 0
        var tokenStart = -1
        fun flushToken(end: Int) {
            if (tokenStart >= 0 && end > tokenStart) {
                val token = code.substring(tokenStart, end)
                when {
                    token in keywords -> withStyle(keywordStyle) { append(token) }
                    token.endsWith("!") && token.dropLast(1).all { it.isLetterOrDigit() || it == '_' } ->
                        withStyle(macroStyle) { append(token) }
                    token.firstOrNull()?.isDigit() == true && token.all { it.isDigit() || it == '_' || it == '.' } ->
                        withStyle(numberStyle) { append(token) }
                    else -> withStyle(defaultStyle) { append(token) }
                }
                tokenStart = -1
            }
        }

        while (i < code.length) {
            val c = code[i]
            // Line comment
            if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
                flushToken(i)
                val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                withStyle(commentStyle) { append(code.substring(i, end)) }
                i = end
                continue
            }
            // String literal
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

@Preview(name = "CodeCard dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun CodeCardPreview() {
    FerrisFeedTheme(darkTheme = true) {
        CodeCard(
            code = "fn main() {\n    let mut s = String::from(\"hi\");\n    takes(&s); // borrow, no move\n    println!(\"{s}\");\n}\n",
            language = "rust",
            onRun = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
