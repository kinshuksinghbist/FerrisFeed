package com.ferrisfeed.data

/**
 * Weekly card-analytics digest (TODO 27b).
 *
 * Reads EXISTING events only (the in-memory list behind [NoOpBackend], or
 * the exported Firebase events in production tooling) — no new vendor
 * backend, no new collection. Respects the opt-out by construction: when
 * the user opts out, [Analytics] logs nothing, so the digest input is
 * empty and the digest reports "no data" instead of inventing it.
 *
 * Metrics (TODO 27a `metrics-definition`): recognition start/complete,
 * quiz grade rate, taps per topic, plus the dwell/skip signals feed-ux.md
 * already logs. Anything built to compare variants goes through
 * `a-b-test-design` first (27c) — this digest never assigns variants, it
 * only reads.
 */
object CardAnalyticsDigest {

    data class Digest(
        val reelsSeen: Int,
        val quizAnswered: Int,
        val quizAccuracy: Float,
        val recognitionStarted: Int,
        val recognitionHeard: Int,
        val tapsPerTopic: Map<String, Int>,
        val skipRate: Float,
    )

    fun summarize(events: List<AnalyticsEvent>): Digest {
        val dwell = events.filter { it.name == "reel_dwell" }
        val quiz = events.filter { it.name == "quiz_answer" }
        val recStart = events.filter { it.name == "recognition_start" }
        val recDone = events.filter { it.name == "recognition_complete" }
        val taps = events.filter { it.name == "topic_tap" }

        val answered = quiz.size
        val correct = quiz.count { (it.params["correct"] as? Boolean) == true }
        val skips = dwell.count { (it.params["bucket"] as? String) == "skip" }
        val heard = recDone.count { (it.params["heard"] as? Boolean) == true }
        val tapsPerTopic = taps
            .mapNotNull { it.params["topic"] as? String }
            .groupingBy { it }
            .eachCount()

        return Digest(
            reelsSeen = dwell.size,
            quizAnswered = answered,
            quizAccuracy = if (answered == 0) 0f else correct.toFloat() / answered,
            recognitionStarted = recStart.size,
            recognitionHeard = heard,
            tapsPerTopic = tapsPerTopic,
            skipRate = if (dwell.isEmpty()) 0f else skips.toFloat() / dwell.size,
        )
    }

    /** One-line human summary for the weekly log / settings screen. */
    fun Digest.headline(): String =
        "Saw $reelsSeen reels, answered $quizAnswered quizzes " +
            "(${(quizAccuracy * 100).toInt()}% right), " +
            "voice heard $recognitionHeard of $recognitionStarted, " +
            "skip rate ${(skipRate * 100).toInt()}%."
}
