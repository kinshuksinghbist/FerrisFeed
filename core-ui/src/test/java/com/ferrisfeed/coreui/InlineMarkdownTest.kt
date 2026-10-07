package com.ferrisfeed.coreui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineMarkdownTest {

    private val testBg = Color(0x1AFFFFFF)
    private val testFg = Color(0xFFFFB59E)

    @Test
    fun testEmptyString() {
        val result = parseInlineMarkdown("", testBg, testFg)
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testPlainText() {
        val input = "Hello world from Ferris"
        val result = parseInlineMarkdown(input, testBg, testFg)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testOneCodeSpan() {
        val result = parseInlineMarkdown("Use `let` here", testBg, testFg)
        assertEquals("Use let here", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0]
        assertEquals(4, span.start)
        assertEquals(7, span.end)
        assertEquals(CodeFontFamily, span.item.fontFamily)
        assertEquals(testBg, span.item.background)
        assertEquals(testFg, span.item.color)
    }

    @Test
    fun testTwoCodeSpans() {
        val result = parseInlineMarkdown("Use `let` and `mut` together", testBg, testFg)
        assertEquals("Use let and mut together", result.text)
        assertEquals(2, result.spanStyles.size)

        val span1 = result.spanStyles[0]
        assertEquals(4, span1.start)
        assertEquals(7, span1.end)
        assertEquals(CodeFontFamily, span1.item.fontFamily)

        val span2 = result.spanStyles[1]
        assertEquals(12, span2.start)
        assertEquals(15, span2.end)
        assertEquals(CodeFontFamily, span2.item.fontFamily)
    }

    @Test
    fun testBold() {
        val result = parseInlineMarkdown("This is **important** point", testBg, testFg)
        assertEquals("This is important point", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0]
        assertEquals(8, span.start)
        assertEquals(17, span.end)
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }

    @Test
    fun testUnclosedBacktick() {
        val input = "Unclosed `backtick here"
        val result = parseInlineMarkdown(input, testBg, testFg)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testAdjacentCodeSpans() {
        val result = parseInlineMarkdown("`a``b`", testBg, testFg)
        assertEquals("ab", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(1, result.spanStyles[0].end)
        assertEquals(1, result.spanStyles[1].start)
        assertEquals(2, result.spanStyles[1].end)
    }
}
