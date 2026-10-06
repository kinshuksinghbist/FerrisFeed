package com.ferrisfeed.path

import com.ferrisfeed.data.ReelEntity
import kotlin.math.min

/**
 * Learning path engine: prerequisite DAG, placement test, Daily Mix,
 * and resume-where-left-off.
 *
 * The canonical chain for the backend-rust spine is:
 * Ownership -> Lifetimes -> Async -> Axum -> RateLimiter.
 * The full graph ([TOPIC_GRAPH]) adds the parallel system-design branch
 * branches; [unlockedTopics] gates a topic until every prerequisite has
 * mastery >= [MASTERY_UNLOCK_THRESHOLD].
 *
 * Pure Kotlin, no Android dependencies, so `cargo test`-equivalent JUnit
 * tests can cover the DAG + mix logic on the JVM.
 */
class LearningPathEngine {

    /**
     * Builds the Daily Mix: 7 new + 8 due reviews + 5 weak spots = 20 reels.
     * Falls back gracefully when a bucket is short (new users have no dues).
     */
    fun buildDailyMix(
        newCards: List<ReelEntity>,
        dueCards: List<ReelEntity>,
        weakCards: List<ReelEntity>,
        unlocked: Set<String>,
    ): DailyMix {
        val allowedNew = newCards.filter { it.topic in unlocked }.take(NEW_COUNT)
        // Weak cards that are also due should not appear twice.
        val weakIds = weakCards.map { it.id }.toSet()
        val allowedDue = dueCards.filterNot { it.id in weakIds }.take(DUE_COUNT)
        // Weak bucket excludes anything already picked.
        val pickedIds = (allowedNew.map { it.id } + allowedDue.map { it.id }).toSet()
        val allowedWeak = weakCards.filterNot { it.id in pickedIds }.take(WEAK_COUNT)

        // Interleave so the session alternates new/review/weak instead of
        // front-loading all new cards (better for attention + retention).
        val interleaved = interleave(allowedNew, allowedDue, allowedWeak)
        return DailyMix(
            newCards = allowedNew,
            dueCards = allowedDue,
            weakCards = allowedWeak,
            ordered = interleaved,
        )
    }

    private fun interleave(
        newCards: List<ReelEntity>,
        dueCards: List<ReelEntity>,
        weakCards: List<ReelEntity>,
    ): List<ReelEntity> {
        val out = ArrayList<ReelEntity>(NEW_COUNT + DUE_COUNT + WEAK_COUNT)
        val ni = newCards.iterator()
        val di = dueCards.iterator()
        val wi = weakCards.iterator()
        // Pattern per 4 slots: new, due, due, weak-ish rotation.
        while (ni.hasNext() || di.hasNext() || wi.hasNext()) {
            if (ni.hasNext()) out.add(ni.next())
            if (di.hasNext()) out.add(di.next())
            if (di.hasNext()) out.add(di.next())
            if (wi.hasNext()) out.add(wi.next()) else if (ni.hasNext()) out.add(ni.next())
        }
        return out
    }

    /**
     * Scores the 15-question placement test. >= [PLACEMENT_PASS_SCORE] skips
     * beginner (level 1) reels and starts the user at Ownership+; otherwise
     * they start at the toolchain intro.
     */
    fun scorePlacement(answersCorrect: Int, total: Int = PLACEMENT_TOTAL): PlacementResult {
        require(total == PLACEMENT_TOTAL) { "Placement test must be $PLACEMENT_TOTAL questions" }
        val skipBeginner = answersCorrect >= PLACEMENT_PASS_SCORE
        return PlacementResult(
            correct = answersCorrect,
            total = total,
            skipBeginner = skipBeginner,
            startTopic = if (skipBeginner) "ownership" else "toolchain",
            startLevel = if (skipBeginner) 2 else 1,
        )
    }

    /**
     * Topics whose prerequisites are all mastered. Always includes roots
     * (topics with no prerequisites).
     */
    fun unlockedTopics(masteryByTopic: Map<String, Float>): Set<String> {
        return TOPIC_GRAPH.filter { node ->
            node.prerequisites.all { (masteryByTopic[it] ?: 0f) >= MASTERY_UNLOCK_THRESHOLD }
        }.map { it.id }.toSet()
    }

    /** Next recommended topic: lowest-mastery unlocked topic, spine order first. */
    fun nextTopic(masteryByTopic: Map<String, Float>): String? {
        val unlocked = unlockedTopics(masteryByTopic)
        return TOPIC_GRAPH
            .filter { it.id in unlocked }
            .minByOrNull { (masteryByTopic[it.id] ?: 0f) * 1000 + it.spineOrder }
            ?.id
    }

    /**
     * Resume target: most recent in-progress topic below mastery threshold,
     * else [nextTopic]. Backed by [com.ferrisfeed.data.ProgressStore]
     * scroll position + per-topic mastery in the ViewModel.
     */
    fun resumeTopic(
        masteryByTopic: Map<String, Float>,
        lastSeenTopic: String?,
    ): ResumeTarget {
        if (lastSeenTopic != null) {
            val mastery = masteryByTopic[lastSeenTopic] ?: 0f
            if (mastery < MASTERY_COMPLETE_THRESHOLD) {
                return ResumeTarget(topic = lastSeenTopic, masteryPct = (mastery * 100).toInt(), isNew = false)
            }
        }
        val next = nextTopic(masteryByTopic) ?: "toolchain"
        return ResumeTarget(
            topic = next,
            masteryPct = ((masteryByTopic[next] ?: 0f) * 100).toInt(),
            isNew = (masteryByTopic[next] ?: 0f) <= 0f,
        )
    }

    companion object {
        const val NEW_COUNT = 7
        const val DUE_COUNT = 8
        const val WEAK_COUNT = 5
        const val DAILY_MIX_SIZE = NEW_COUNT + DUE_COUNT + WEAK_COUNT

        const val PLACEMENT_TOTAL = 15
        /** 11/15 (73%) skips beginner. Set so guessing (~4/15) never passes. */
        const val PLACEMENT_PASS_SCORE = 11

        /** 60% mastery unlocks dependents; 85% counts as "done" for resume. */
        const val MASTERY_UNLOCK_THRESHOLD = 0.60f
        const val MASTERY_COMPLETE_THRESHOLD = 0.85f

        /**
         * Prerequisite DAG. spineOrder pins the hero chain
         * Ownership -> Lifetimes -> Async -> Axum -> RateLimiter so ties in
         * mastery still recommend the canonical backend path first.
         */
        val TOPIC_GRAPH: List<TopicNode> = listOf(
            TopicNode("toolchain", "rust", 1, emptyList(), spineOrder = 0),
            TopicNode("variables", "rust", 1, listOf("toolchain"), spineOrder = 1),
            TopicNode("ownership", "rust", 1, listOf("variables"), spineOrder = 2),
            TopicNode("borrowing", "rust", 1, listOf("ownership"), spineOrder = 3),
            TopicNode("collections", "rust", 1, listOf("borrowing"), spineOrder = 4),
            TopicNode("error-handling", "rust", 1, listOf("collections"), spineOrder = 5),
            TopicNode("generics-traits", "rust", 2, listOf("error-handling"), spineOrder = 6),
            TopicNode("lifetimes", "rust", 2, listOf("ownership", "generics-traits"), spineOrder = 7),
            TopicNode("smart-pointers", "rust", 2, listOf("lifetimes"), spineOrder = 8),
            TopicNode("async", "rust", 2, listOf("lifetimes", "smart-pointers"), spineOrder = 9),
            TopicNode("axum", "rust", 2, listOf("async"), spineOrder = 10),
            TopicNode("rate-limiter", "system-design", 2, listOf("axum"), spineOrder = 11),
            // System-design branch (needs axum for the Rust-flavored reels).
            TopicNode("http-caching", "system-design", 1, listOf("toolchain"), spineOrder = 60),
            TopicNode("hashing-cap", "system-design", 2, listOf("http-caching"), spineOrder = 61),
            TopicNode("kafka-queues", "system-design", 2, listOf("hashing-cap"), spineOrder = 62),
            TopicNode("raft", "system-design", 3, listOf("kafka-queues"), spineOrder = 63),
            TopicNode("observability", "system-design", 2, listOf("axum"), spineOrder = 64),
        )

        /** The 15 placement reel IDs, spanning beginner topics. Fixed so results are comparable. */
        val PLACEMENT_REEL_IDS: List<String> = listOf(
            "rust-cargo-003",
            "rust-var-007",
            "rust-own-014",
            "rust-own-021",
            "rust-borrow-005",
            "rust-borrow-011",
            "rust-struct-004",
            "rust-enum-006",
            "rust-option-003",
            "rust-vec-002",
            "rust-string-004",
            "rust-result-005",
            "rust-mod-002",
            "rust-test-001",
            "rust-clippy-001",
        )

        /** "Continue Ownership 62%" label for the resume bar. */
        fun resumeLabel(topic: String, masteryPct: Int): String {
            val pretty = topic.split("-").joinToString(" ") { w ->
                w.replaceFirstChar { it.uppercase() }
            }
            return "Continue $pretty $masteryPct%"
        }

        /** Feed shuffle: 70% due + 20% new-in-path + 10% random review. */
        fun feedCounts(queueSize: Int): FeedMix {
            if (queueSize <= 0) return FeedMix(0, 0, 0)
            val due = (queueSize * 0.7).toInt().coerceAtLeast(min(1, queueSize))
            val fresh = (queueSize * 0.2).toInt()
            val random = (queueSize - due - fresh).coerceAtLeast(0)
            return FeedMix(due = due, fresh = fresh, random = random)
        }
    }
}

/** DAG node: one learnable topic with prerequisite topic IDs. */
data class TopicNode(
    val id: String,
    val track: String,
    val level: Int,
    val prerequisites: List<String>,
    /** Lower = earlier on the hero spine; branches start at 50+. */
    val spineOrder: Int,
)

data class DailyMix(
    val newCards: List<ReelEntity>,
    val dueCards: List<ReelEntity>,
    val weakCards: List<ReelEntity>,
    /** Interleaved play order (size <= 20). */
    val ordered: List<ReelEntity>,
)

data class PlacementResult(
    val correct: Int,
    val total: Int,
    val skipBeginner: Boolean,
    val startTopic: String,
    val startLevel: Int,
)

data class ResumeTarget(
    val topic: String,
    val masteryPct: Int,
    val isNew: Boolean,
)

data class FeedMix(
    val due: Int,
    val fresh: Int,
    val random: Int,
)
