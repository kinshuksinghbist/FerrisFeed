package com.ferrisfeed.path

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.ferrisfeed.data.SearchFilters as DataSearchFilters

/**
 * JVM tests for the TODO 22 IA logic: the UI→Room filter mapping lives in
 * [PathViewModel] and must be exact + lossless, and browse activation must
 * match what the screen renders ('results versus directory' resting state).
 */
class PathBrowseIaTest {

    // ---- toDataFilters: the structural IA mapping (TODO 22 core) ----

    @Test
    fun `ui topic filter maps verbatim to the room topic`() {
        val mapped = SearchFilters(topic = "ownership").toDataFilters()
        assertEquals("ownership", mapped.topic)
    }

    @Test
    fun `unset level spans the full range so browsing a topic shows all levels`() {
        val mapped = SearchFilters(topic = "drills").toDataFilters()
        assertEquals(1, mapped.minLevel)
        assertEquals(4, mapped.maxLevel)
    }

    @Test
    fun `level chip narrows to that single level`() {
        val mapped = SearchFilters(topic = "concepts", level = 2).toDataFilters()
        assertEquals(2, mapped.minLevel)
        assertEquals(2, mapped.maxLevel)
    }

    @Test
    fun `track and content filters survive the mapping`() {
        val mapped = SearchFilters(track = "rust", hasCode = true, hasQuiz = true).toDataFilters()
        assertEquals("rust", mapped.track)
        assertEquals(true, mapped.hasCode)
        assertEquals(true, mapped.hasQuiz)
    }

    @Test
    fun `empty filters map to the room defaults that browse everything`() {
        val mapped = SearchFilters().toDataFilters()
        assertEquals(DataSearchFilters(), mapped)
    }

    // ---- hasActiveBrowse: which state the screen renders ----

    @Test
    fun `resting state is unscoped - directory shows`() {
        assertFalse(hasActiveBrowse("", SearchFilters()))
        assertFalse(hasActiveBrowse("   ", SearchFilters()))
    }

    @Test
    fun `any chip alone activates browse`() {
        assertTrue(hasActiveBrowse("", SearchFilters(topic = "ownership")))
        assertTrue(hasActiveBrowse("", SearchFilters(track = "rust")))
        assertTrue(hasActiveBrowse("", SearchFilters(level = 2)))
        assertTrue(hasActiveBrowse("", SearchFilters(hasCode = true)))
        assertTrue(hasActiveBrowse("", SearchFilters(hasQuiz = false)))
    }

    @Test
    fun `typed text alone activates browse`() {
        assertTrue(hasActiveBrowse("borrow", SearchFilters()))
    }

    @Test
    fun `query text does not count as a scope filter`() {
        assertFalse(SearchFilters().isScoped())
    }

    // ---- cleared(): the clear-filters affordance resets scope, not text ----

    @Test
    fun `cleared removes every filter`() {
        val scoped = SearchFilters(track = "rust", level = 3, hasCode = true, hasQuiz = false, topic = "unsafe")
        assertEquals(SearchFilters(), scoped.cleared())
        assertTrue(scoped.cleared() != scoped)
    }

    // ---- displayTopic: shared chip/row caption formatting ----

    @Test
    fun `multi-word labels title-case per word`() {
        assertEquals("Data Systems", "data systems".displayTopic())
        assertEquals("Crates & Tooling", "crates & tooling".displayTopic())
    }

    @Test
    fun `single word and legacy key still read well`() {
        assertEquals("Ownership", "ownership".displayTopic())
        assertEquals("Drills", "drills".displayTopic())
    }
}
