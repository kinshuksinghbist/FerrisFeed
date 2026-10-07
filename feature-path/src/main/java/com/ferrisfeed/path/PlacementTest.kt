package com.ferrisfeed.path

/**
 * Placement-test item bank + scoring (TODO 26, drafts only).
 *
 * No screens ship from this item: 26a is a paper draft reviewed before any
 * UI is built. This file is the pure, unit-tested selection + scoring rule
 * the draft proposes, so the review argues about numbers, not screenshots.
 *
 * Rule (framed with `jobs-to-be-done`): an experienced dev hires the test
 * to skip what they already know, not to prove they are advanced. The
 * smallest flow is 5 questions: 2x L1 (must-pass basics), 2x L2 (the
 * boundary), 1x L3 (the stretch). Scoring:
 * - 4-5 correct -> skip beginner (start at L2).
 * - <= 3 -> start at L1.
 * - L3 correct alone never places alone: it only breaks a 3-3 tie upward.
 *
 * Items come from the seeded reel bank: [selectItems] picks the bank's
 * lowest-order quiz-bearing reel per (level, topic) so the test always
 * mirrors shipped content and never invents questions (`usability-test-plan`
 * + `test-scenario` in docs/placement-test.md).
 */
object PlacementTest {

    /** One candidate drawn from the seeded bank (reel id + level + topic). */
    data class Candidate(
        val reelId: String,
        val level: Int,
        val topic: String,
        val orderIndex: Int,
        val hasQuiz: Boolean,
    )

    data class Outcome(
        val correct: Int,
        val total: Int,
        val skipBeginner: Boolean,
    )

    /**
     * Picks 5 items: 2x L1, 2x L2, 1x L3. Distinct topics preferred so one
     * topic cannot dominate; falls back to lowest orderIndex when the bank
     * is thin. Returns fewer than 5 only when the bank itself is short.
     */
    fun selectItems(bank: List<Candidate>): List<Candidate> {
        val quizBearing = bank.filter { it.hasQuiz }
        val picked = mutableListOf<Candidate>()
        val usedTopics = mutableSetOf<String>()
        fun take(level: Int, n: Int) {
            val pool = quizBearing
                .filter { it.level == level && it !in picked }
                .sortedWith(
                    compareBy(
                        { if (it.topic in usedTopics) 1 else 0 },
                        { it.orderIndex },
                    ),
                )
            pool.take(n).forEach {
                picked += it
                usedTopics += it.topic
            }
        }
        take(1, 2)
        take(2, 2)
        take(3, 1)
        return picked
    }

    /** Scores ordered answers against the selected items (see rule above). */
    fun evaluate(items: List<Candidate>, answers: List<Boolean>): Outcome {
        val total = items.size
        val correct = answers.take(total).count { it }
        val l3Correct = items
            .zip(answers)
            .any { (item, ok) -> ok && item.level >= 3 }
        val skip = correct >= 4 || (correct == 3 && l3Correct)
        return Outcome(correct = correct, total = total, skipBeginner = skip)
    }
}
