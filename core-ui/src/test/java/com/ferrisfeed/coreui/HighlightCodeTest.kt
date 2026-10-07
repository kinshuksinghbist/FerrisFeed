package com.ferrisfeed.coreui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HighlightCodeTest {

    @Test
    fun testHighlightRustSignature() {
        val snippet = "fn main<'a>() { Vec::new(); }"
        val result = highlightCode(snippet, "rust")
        assertEquals(snippet, result.text)

        val spans = result.spanStyles

        // fn at 0..2 should be Keyword
        val fnSpan = spans.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull("fn span not found", fnSpan)
        assertEquals(CodeCardTokens.Keyword, fnSpan?.item?.color)

        // main at 3..7 should be FnColor
        val mainSpan = spans.firstOrNull { it.start == 3 && it.end == 7 }
        assertNotNull("main span not found", mainSpan)
        assertEquals(CodeCardTokens.FnColor, mainSpan?.item?.color)

        // 'a at 8..10 should be Number (lifetime token)
        val lifetimeSpan = spans.firstOrNull { it.start == 8 && it.end == 10 }
        assertNotNull("'a span not found", lifetimeSpan)
        assertEquals(CodeCardTokens.Number, lifetimeSpan?.item?.color)

        // Vec at 16..19 should be TypeColor
        val vecSpan = spans.firstOrNull { it.start == 16 && it.end == 19 }
        assertNotNull("Vec span not found", vecSpan)
        assertEquals(CodeCardTokens.TypeColor, vecSpan?.item?.color)

        // new at 21..24 should be FnColor
        val newSpan = spans.firstOrNull { it.start == 21 && it.end == 24 }
        assertNotNull("new span not found", newSpan)
        assertEquals(CodeCardTokens.FnColor, newSpan?.item?.color)
    }
}
