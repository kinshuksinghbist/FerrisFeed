package com.ferrisfeed.coreui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
 * Code editor palette (TODO 23c `design-token-audit`).
 *
 * The code card keeps a fixed GitHub-dark editor surface in BOTH themes —
 * that is deliberate, not drift: a light editor surface at 12.5sp mono
 * fails the 23a contrast audit for keyword hues, while the dark surface
 * holds >= 7:1 for every span. These tokens are the single home for that
 * palette; do not inline new hex values in this file.
 */
object CodeCardTokens {
    val Container = Color(0xFF0D1117)
    val Muted = Color(0xFF8B949E)
    val Body = Color(0xFFC9D1D9)
    val Output = Color(0xFFA5D6FF)
    val Keyword = Color(0xFFFF7B72)
    val Number = Color(0xFF79C0FF)
    val Macro = Color(0xFFD2A8FF)
}

/**
 * Code snippet well (user review 2026-10-07): the snippet lives INSIDE the
 * info card, not as a separate card. [CodeBlock] is a self-contained dark
 * well — header (language label + copy + flip) over highlighted code or
 * output — with no outer Card and no elevation, so it sits inside
 * [ReelCard] without a card-in-card look.
 *
 * Header has copy + flip and nothing else: no Run button, no font slider
 * (fixed 12.5sp mono), no second copy row. The flip button appears only
 * when [output] is non-null and swaps the body between the highlighted
 * code and the expected result, with a small icon rotation for affordance.
 * Copy always copies the code.
 */
@Composable
fun CodeBlock(
    code: String,
    language: String,
    output: String?,
    modifier: Modifier = Modifier,
) {
    // Platform clipboard (android.content.ClipboardManager): Compose's own
    // ClipboardManager.setText API is deprecated, and LocalClipboard's
    // ClipEntry plumbing is host-version sensitive. The framework API here is
    // available on every supported API level with no deprecation warning.
    val context = LocalContext.current
    var copied by remember(code) { mutableStateOf(false) }
    var flipped by remember(code) { mutableStateOf(false) }
    val flipRotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "flip-rotation",
    )
    val highlighted = remember(code, language) { highlightCode(code, language) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = CodeCardTokens.Container,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: language + copy + flip (flip only when output exists).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (flipped) "output" else language.lowercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = CodeFontFamily),
                    color = CodeCardTokens.Muted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (copied) "Copied!" else "${code.lines().size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) FerrisColors.MintCorrect else CodeCardTokens.Muted,
                )
                if (output != null) {
                    IconButton(onClick = { flipped = !flipped }) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = if (flipped) "Show code" else "Show output",
                            tint = CodeCardTokens.Body,
                            modifier = Modifier.rotate(flipRotation),
                        )
                    }
                }
                IconButton(onClick = {
                    context.getSystemService(Context.CLIPBOARD_SERVICE)
                        ?.let { it as? ClipboardManager }
                        ?.setPrimaryClip(ClipData.newPlainText("code", code))
                    copied = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = CodeCardTokens.Body,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (flipped && output != null) {
                Text(
                    text = output,
                    fontFamily = CodeFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.5.sp,
                    color = CodeCardTokens.Output,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                )
            } else {
                Text(
                    text = highlighted,
                    fontFamily = CodeFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                )
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
        val keywordStyle = SpanStyle(color = CodeCardTokens.Keyword, fontWeight = FontWeight.SemiBold)
        val stringStyle = SpanStyle(color = CodeCardTokens.Output)
        val commentStyle = SpanStyle(color = CodeCardTokens.Muted)
        val numberStyle = SpanStyle(color = CodeCardTokens.Number)
        val macroStyle = SpanStyle(color = CodeCardTokens.Macro)
        val defaultStyle = SpanStyle(color = CodeCardTokens.Body, fontFamily = CodeFontFamily)

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
