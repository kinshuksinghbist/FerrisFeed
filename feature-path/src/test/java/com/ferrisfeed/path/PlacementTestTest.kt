package com.ferrisfeed.path

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementTestTest {

    private fun bank() = listOf(
        PlacementTest.Candidate("r1", 1, "ownership", 0, true),
        PlacementTest.Candidate("r2", 1, "collections", 1, true),
        PlacementTest.Candidate("r3", 1, "ownership", 2, true),
        PlacementTest.Candidate("r4", 2, "lifetimes", 0, true),
        PlacementTest.Candidate("r5", 2, "generics", 1, true),
        PlacementTest.Candidate("r6", 3, "async", 0, true),
        PlacementTest.Candidate("r7", 2, "lifetimes", 5, false),
    )

    @Test
    fun selectsTwoOneTwoTwoOneThree() {
        val items = PlacementTest.selectItems(bank())
        assertEquals(5, items.size)
        assertEquals(listOf(1, 1, 2, 2, 3), items.map { it.level })
    }

    @Test
    fun skipsQuizlessReels() {
        val items = PlacementTest.selectItems(bank())
        assertFalse(items.any { it.reelId == "r7" })
    }

    @Test
    fun fourCorrectSkipsBeginner() {
        val items = PlacementTest.selectItems(bank())
        val outcome = PlacementTest.evaluate(items, listOf(true, true, true, true, false))
        assertTrue(outcome.skipBeginner)
    }

    @Test
    fun threeWithoutStretchStaysBeginner() {
        val items = PlacementTest.selectItems(bank())
        // First three correct are L1/L1/L2 — no L3 among them.
        val outcome = PlacementTest.evaluate(items, listOf(true, true, true, false, false))
        assertFalse(outcome.skipBeginner)
    }

    @Test
    fun threeWithStretchSkipsBeginner() {
        val items = PlacementTest.selectItems(bank())
        // Two basics + the L3 stretch.
        val outcome = PlacementTest.evaluate(items, listOf(true, true, false, false, true))
        assertTrue(outcome.skipBeginner)
    }
}
