package com.ferrisfeed.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Analytics event logging: dwell time, saves, quiz accuracy per topic.
 *
 * Privacy contract (see docs/analytics.md + docs/privacy-policy.md):
 * - Only reel IDs, track/topic keys, durations, grades, and UI actions.
 * - NEVER body text, code, quiz options, or any PII / advertising ID.
 * - All logging is gated on [isEnabled] (user opt-out in Settings).
 * - A [NoOpBackend] keeps unit tests hermetic; production wires Firebase.
 *
 * A/B: hook wording variants come from RemoteConfig (see [ExperimentFlags]);
 * exposure + outcome are logged together so the test is analyzable.
 */
class Analytics(
    private val backend: AnalyticsBackend,
    private val store: ProgressStore,
) {
    suspend fun isEnabled(): Flow<Boolean> = store.analyticsOptOut.map { !it }

    suspend fun logDwell(reelId: String, track: String, topic: String, dwellMillis: Long) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "reel_dwell",
                params = mapOf(
                    "reel_id" to reelId,
                    "track" to track,
                    "topic" to topic,
                    "dwell_ms" to dwellMillis.coerceIn(0L, 600_000L),
                    "bucket" to dwellBucket(dwellMillis),
                ),
            ),
        )
    }

    suspend fun logImpression(reelId: String, position: Int, source: FeedSource) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "reel_impression",
                params = mapOf(
                    "reel_id" to reelId,
                    "position" to position,
                    "source" to source.key,
                ),
            ),
        )
    }

    suspend fun logSave(reelId: String, saved: Boolean) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = if (saved) "reel_save" else "reel_unsave",
                params = mapOf("reel_id" to reelId),
            ),
        )
    }

    suspend fun logQuizAnswer(
        reelId: String,
        topic: String,
        correct: Boolean,
        grade: Int,
        hookVariant: String? = null,
    ) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "quiz_answer",
                params = buildMap {
                    put("reel_id", reelId)
                    put("topic", topic)
                    put("correct", correct)
                    put("grade", grade)
                    if (hookVariant != null) put("hook_variant", hookVariant)
                },
            ),
        )
    }

    suspend fun logMastery(topic: String, masteryPct: Int) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "topic_mastery",
                params = mapOf("topic" to topic, "mastery_pct" to masteryPct.coerceIn(0, 100)),
            ),
        )
    }

    suspend fun logStreak(streakDays: Int) {
        if (!allowed()) return
        backend.log(AnalyticsEvent("streak_tick", mapOf("streak_days" to streakDays)))
    }

    /**
     * Speaker-opening flow (TODO 24 + 27a `metrics-definition`).
     *
     * Defined BEFORE instrumenting: the card logs recognition start and
     * completion per reel, never the transcript or audio. Completion carries
     * only a boolean heard flag + latency bucket — no speech content leaves
     * the device (privacy policy: no user-typed/spoken content, ever).
     */
    suspend fun logRecognitionStart(reelId: String, topic: String) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "recognition_start",
                params = mapOf("reel_id" to reelId, "topic" to topic),
            ),
        )
    }

    suspend fun logRecognitionComplete(
        reelId: String,
        topic: String,
        heard: Boolean,
        latencyMs: Long,
    ) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "recognition_complete",
                params = mapOf(
                    "reel_id" to reelId,
                    "topic" to topic,
                    "heard" to heard,
                    "latency_bucket" to recognitionBucket(latencyMs),
                ),
            ),
        )
    }

    /** Topic entry taps: which roadmap/directory node led into a topic feed. */
    suspend fun logTopicTap(topic: String, source: String) {
        if (!allowed()) return
        backend.log(
            AnalyticsEvent(
                name = "topic_tap",
                params = mapOf("topic" to topic, "source" to source),
            ),
        )
    }

    private fun recognitionBucket(millis: Long): String = when {
        millis < 3_000 -> "fast"
        millis < 10_000 -> "normal"
        else -> "slow"
    }

    private suspend fun allowed(): Boolean = !store.analyticsOptOut.first()

    private fun dwellBucket(millis: Long): String = when {
        millis < 3_000 -> "skip"
        millis < 15_000 -> "glance"
        millis < 60_000 -> "read"
        else -> "deep"
    }
}

enum class FeedSource(val key: String) {
    DAILY_MIX("daily_mix"),
    SEARCH("search"),
    TOPIC_MAP("topic_map"),
    WIDGET("widget"),
    RESUME("resume"),
}

/** Transport-agnostic event. Backends (Firebase / tests) implement [AnalyticsBackend]. */
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
)

interface AnalyticsBackend {
    fun log(event: AnalyticsEvent)
    fun setUserProperty(name: String, value: String?)
}

/** Default in tests and when Firebase is stripped (e.g. FOSS flavor). */
class NoOpBackend : AnalyticsBackend {
    val events = mutableListOf<AnalyticsEvent>()
    override fun log(event: AnalyticsEvent) {
        events.add(event)
    }
    override fun setUserProperty(name: String, value: String?) = Unit
}

/**
 * RemoteConfig-backed A/B flags. Production implementation reads Firebase
 * RemoteConfig; tests construct this directly.
 *
 * Current experiments:
 * - hook_style: "curiosity_gap" (control) vs "compiler_error" vs "perf_claim"
 * - daily_mix_ratio: "70_20_10" (control) vs "60_30_10"
 */
data class ExperimentFlags(
    val hookStyle: String = "curiosity_gap",
    val dailyMixRatio: String = "70_20_10",
) {
    companion object {
        fun fromRemoteConfig(getString: (String) -> String?): ExperimentFlags {
            return ExperimentFlags(
                hookStyle = getString("hook_style") ?: "curiosity_gap",
                dailyMixRatio = getString("daily_mix_ratio") ?: "70_20_10",
            )
        }
    }
}
