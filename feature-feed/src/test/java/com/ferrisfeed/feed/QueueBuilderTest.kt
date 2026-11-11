package com.ferrisfeed.feed

import com.ferrisfeed.data.LearnerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueBuilderTest {

    private fun reel(
        id: String,
        track: Track,
        topic: String,
        level: Int = 1,
        order: Int = 0,
        isNew: Boolean = true,
        due: Long = 0L,
    ) = Reel(
        id = id,
        track = track,
        level = level,
        hook = id,
        bodyMd = "",
        code = null,
        language = "rust",
        output = null,
        takeaway = "",
        trap = "",
        trapCompilerMessage = null,
        topic = topic,
        quiz = QuizModel(type = QuizType.MCQ, question = "q", options = listOf("a", "b"), explanation = ""),
        dueAtEpochMs = due,
        isNew = isNew,
        pathOrder = order,
    )

    private val library = listOf(
        // Deliberately shuffled input: advanced topics first, like pack files.
        reel("r-unsafe", Track.RUST, "unsafe-ffi", level = 3),
        reel("r-traits", Track.RUST, "traits-generics", level = 2),
        reel("r-own-2", Track.RUST, "ownership", order = 2),
        reel("r-cargo", Track.RUST, "cargo-toolchain"),
        reel("r-own-1", Track.RUST, "ownership", order = 1),
        reel("r-vars", Track.RUST, "variables-types"),
        reel("s-cache", Track.SYSTEM_DESIGN, "caching"),
        reel("s-api", Track.SYSTEM_DESIGN, "apis-protocols"),
        reel("s-db", Track.SYSTEM_DESIGN, "databases"),
    )

    @Test
    fun `new rust learner starts at the beginning of the curriculum`() {
        val q = QueueBuilder.build(library, nowMs = 0, profile = LearnerProfile(trackFocus = "rust", experience = 0))
        assertEquals(listOf("r-cargo", "r-vars", "r-own-1", "r-own-2", "r-traits", "r-unsafe"), q.take(6).map { it.id })
        // The other track still exists, after the focus track.
        assertTrue(q.drop(6).all { it.track == Track.SYSTEM_DESIGN })
    }

    @Test
    fun `experienced rust learner starts at traits and keeps basics for later`() {
        val q = QueueBuilder.build(library, nowMs = 0, profile = LearnerProfile(trackFocus = "rust", experience = 2))
        assertEquals("r-traits", q.first().id)
        assertTrue(q.indexOfFirst { it.id == "r-cargo" } > q.indexOfFirst { it.id == "r-unsafe" })
    }

    @Test
    fun `both tracks alternate three rust then two system design`() {
        val q = QueueBuilder.build(library, nowMs = 0, profile = LearnerProfile(trackFocus = "both"))
        assertEquals(
            listOf(Track.RUST, Track.RUST, Track.RUST, Track.SYSTEM_DESIGN, Track.SYSTEM_DESIGN),
            q.take(5).map { it.track },
        )
        assertEquals("s-api", q[3].id)
    }

    @Test
    fun `due reviews take at most two slots per set`() {
        val dues = (1..6).map { reel("due-$it", Track.RUST, "ownership", isNew = false, due = it.toLong()) }
        val fresh = (1..10).map { reel("new-$it", Track.RUST, "borrowing", order = it) }
        val q = QueueBuilder.build(dues + fresh, nowMs = 100, profile = LearnerProfile(trackFocus = "rust"))
        q.take(10).chunked(FeedPaging.SET_SIZE).forEach { set ->
            assertTrue(set.count { it.id.startsWith("due") } <= QueueBuilder.DUE_PER_SET)
        }
        assertEquals(16, q.size)
    }

    @Test
    fun `topic order follows level then authored order`() {
        val reels = listOf(
            reel("b", Track.RUST, "ownership", level = 2, order = 0),
            reel("a2", Track.RUST, "ownership", level = 1, order = 2),
            reel("a1", Track.RUST, "ownership", level = 1, order = 1),
        )
        assertEquals(listOf("a1", "a2", "b"), QueueBuilder.topicOrder(reels).map { it.id })
    }
}
