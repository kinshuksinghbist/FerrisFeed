package com.ferrisfeed.feed

/**
 * Page layout of the feed (v3).
 *
 * The queue is played in SETS of [SET_SIZE] reels. Every reel is two pages
 * (lesson, then its quiz) and every set ends with a recap page, so a session
 * has a beginning, a middle and an end instead of an endless scroll:
 *
 *   set 0: L0 Q0 L1 Q1 L2 Q2 L3 Q3 L4 Q4 R0 | set 1: L5 Q5 ... R1 | ...
 *
 * The final set may be partial; it still ends with a recap. Pure functions
 * so the mapping is pinned by JVM tests.
 */
object FeedPaging {
    const val SET_SIZE = 5
    private const val SET_STRIDE = SET_SIZE * 2 + 1

    sealed interface Page {
        data class Lesson(val reel: Int) : Page
        data class Quiz(val reel: Int) : Page
        data class Recap(val set: Int) : Page
    }

    fun setCount(reels: Int): Int = if (reels <= 0) 0 else (reels + SET_SIZE - 1) / SET_SIZE

    fun pageCount(reels: Int): Int = if (reels <= 0) 0 else reels * 2 + setCount(reels)

    fun pageAt(index: Int, reels: Int): Page {
        val set = index / SET_STRIDE
        val within = index % SET_STRIDE
        val reelsInSet = reelsInSet(set, reels)
        return if (within < reelsInSet * 2) {
            val reel = set * SET_SIZE + within / 2
            if (within % 2 == 0) Page.Lesson(reel) else Page.Quiz(reel)
        } else {
            Page.Recap(set)
        }
    }

    fun lessonPageOf(reel: Int): Int = (reel / SET_SIZE) * SET_STRIDE + (reel % SET_SIZE) * 2

    fun setOf(reel: Int): Int = reel / SET_SIZE

    fun reelsInSet(set: Int, reels: Int): Int = (reels - set * SET_SIZE).coerceIn(0, SET_SIZE)

    /** Reel indices (into the queue) that belong to [set]. */
    fun reelRange(set: Int, reels: Int): IntRange {
        val start = set * SET_SIZE
        return start until start + reelsInSet(set, reels)
    }

    /** The reel a page belongs to; recap pages report the last reel of their set. */
    fun reelOf(page: Page, reels: Int): Int = when (page) {
        is Page.Lesson -> page.reel
        is Page.Quiz -> page.reel
        is Page.Recap -> reelRange(page.set, reels).last
    }
}
