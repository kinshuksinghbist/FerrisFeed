package com.ferrisfeed.coreui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/**
 * Parses inline markdown supporting exactly `code` and **bold** (P6 34d).
 * Unclosed markers render literally. No other markdown is interpreted.
 */
fun parseInlineMarkdown(
    text: String,
    codeBg: Color,
    codeFg: Color,
    boldWeight: FontWeight = FontWeight.Bold,
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")

    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val closeIndex = text.indexOf("**", i + 2)
                if (closeIndex != -1 && closeIndex > i + 2) {
                    val boldContent = text.substring(i + 2, closeIndex)
                    withStyle(SpanStyle(fontWeight = boldWeight)) {
                        append(boldContent)
                    }
                    i = closeIndex + 2
                } else {
                    append("**")
                    i += 2
                }
            } else if (text[i] == '`') {
                val closeIndex = text.indexOf('`', i + 1)
                if (closeIndex != -1 && closeIndex > i + 1) {
                    val codeContent = text.substring(i + 1, closeIndex)
                    withStyle(
                        SpanStyle(
                            fontFamily = CodeFontFamily,
                            background = codeBg,
                            color = codeFg,
                            fontSize = 0.92.em,
                        )
                    ) {
                        append(codeContent)
                    }
                    i = closeIndex + 1
                } else {
                    append('`')
                    i += 1
                }
            } else {
                append(text[i])
                i += 1
            }
        }
    }
}
