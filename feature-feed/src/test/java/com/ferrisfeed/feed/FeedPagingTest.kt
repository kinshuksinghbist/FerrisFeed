package com.ferrisfeed.feed

import com.ferrisfeed.feed.FeedPaging.Page
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedPagingTest {

    @Test
    fun `full sets are lesson-quiz pairs followed by one recap`() {
        val reels = 10
        assertEquals(22, FeedPaging.pageCount(reels))
        assertEquals(Page.Lesson(0), FeedPaging.pageAt(0, reels))
        assertEquals(Page.Quiz(0), FeedPaging.pageAt(1, reels))
        assertEquals(Page.Quiz(4), FeedPaging.pageAt(9, reels))
        assertEquals(Page.Recap(0), FeedPaging.pageAt(10, reels))
        assertEquals(Page.Lesson(5), FeedPaging.pageAt(11, reels))
        assertEquals(Page.Recap(1), FeedPaging.pageAt(21, reels))
    }

    @Test
    fun `a partial last set still ends with a recap`() {
        val reels = 7
        assertEquals(7 * 2 + 2, FeedPaging.pageCount(reels))
        assertEquals(Page.Lesson(6), FeedPaging.pageAt(13, reels))
        assertEquals(Page.Quiz(6), FeedPaging.pageAt(14, reels))
        assertEquals(Page.Recap(1), FeedPaging.pageAt(15, reels))
        assertEquals(5..6, FeedPaging.reelRange(1, reels))
    }

    @Test
    fun `lesson page of a reel round trips through pageAt`() {
        val reels = 23
        for (reel in 0 until reels) {
            assertEquals(Page.Lesson(reel), FeedPaging.pageAt(FeedPaging.lessonPageOf(reel), reels))
        }
    }

    @Test
    fun `every page maps back to a reel inside the queue`() {
        val reels = 12
        for (i in 0 until FeedPaging.pageCount(reels)) {
            val reel = FeedPaging.reelOf(FeedPaging.pageAt(i, reels), reels)
            assert(reel in 0 until reels) { "page $i -> reel $reel" }
        }
    }

    @Test
    fun `empty queue has no pages`() {
        assertEquals(0, FeedPaging.pageCount(0))
        assertEquals(0, FeedPaging.setCount(0))
    }
}
