package com.ferrisfeed.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.pow

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ferris_progress",
)

/**
 * DataStore-backed progress: XP, per-topic mastery with time decay,
 * streak + heatmap activity, feed scroll position, placement state.
 *
 * Mastery model: each topic stores [TopicMastery] as JSON under key
 * `mastery_<topic>`. Raw score 0..1 decays 2% per inactive day when read
 * back (see [decayedMastery]), so skipping a topic visibly lowers its %.
 * Quiz "Got it" pushes toward 1.0, "Again" pulls down fast.
 */
class ProgressStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // ---- Keys ----

    private val KEY_XP = intPreferencesKey("xp_total")
    private val KEY_STREAK = intPreferencesKey("streak_days")
    private val KEY_LAST_ACTIVE_DAY = stringPreferencesKey("last_active_day")
    private val KEY_ACTIVE_DAYS = stringSetPreferencesKey("active_days")
    private val KEY_DAY_XP_PREFIX = "day_xp_"
    private val KEY_SCROLL_REEL_ID = stringPreferencesKey("scroll_reel_id")
    private val KEY_SCROLL_INDEX = intPreferencesKey("scroll_index")
    private val KEY_PLACEMENT_DONE = booleanPreferencesKey("placement_done")
    private val KEY_PLACEMENT_SKIPPED_BEGINNER = booleanPreferencesKey("placement_skip_beginner")
    private val KEY_ANALYTICS_OPT_OUT = booleanPreferencesKey("analytics_opt_out")
    private val KEY_LAST_SYNC = longPreferencesKey("last_sync_millis")

    // ---- Observable state ----

    val xp: Flow<Int> = context.progressDataStore.data.map { it[KEY_XP] ?: 0 }
    val streakDays: Flow<Int> = context.progressDataStore.data.map { it[KEY_STREAK] ?: 0 }
    val placementDone: Flow<Boolean> =
        context.progressDataStore.data.map { it[KEY_PLACEMENT_DONE] ?: false }
    val analyticsOptOut: Flow<Boolean> =
        context.progressDataStore.data.map { it[KEY_ANALYTICS_OPT_OUT] ?: false }

    val scrollPosition: Flow<ScrollPosition> = context.progressDataStore.data.map {
        ScrollPosition(
            reelId = it[KEY_SCROLL_REEL_ID],
            index = it[KEY_SCROLL_INDEX] ?: 0,
        )
    }

    /** Last 365 active days as ISO yyyy-MM-dd, for the heatmap calendar. */
    val heatmapDays: Flow<Set<String>> =
        context.progressDataStore.data.map { it[KEY_ACTIVE_DAYS] ?: emptySet() }

    // ---- XP ----

    /**
     * Adds XP and records today's activity (streak + heatmap) atomically.
     * Returns the new total.
     */
    suspend fun addXp(amount: Int, now: Long = System.currentTimeMillis()): Int {
        var total = 0
        context.progressDataStore.edit { prefs ->
            total = (prefs[KEY_XP] ?: 0) + max(0, amount)
            prefs[KEY_XP] = total
            recordActivityLocked(prefs, amount, now)
        }
        return total
    }

    suspend fun getXp(): Int = xp.first()

    // ---- Streak / heatmap ----

    /**
     * Records one active day. Streak increments when yesterday was the last
     * active day; same-day repeats keep the streak; gaps reset to 1.
     */
    suspend fun recordActiveDay(now: Long = System.currentTimeMillis()) {
        context.progressDataStore.edit { prefs ->
            recordActivityLocked(prefs, dayXp = 0, now = now)
        }
    }

    suspend fun getHeatmapCounts(days: Int = 120): Map<String, Int> {
        val prefs = context.progressDataStore.data.first()
        val cutoff = LocalDate.now().minusDays(days.toLong())
        return prefs[KEY_ACTIVE_DAYS].orEmpty()
            .filter { runCatching { LocalDate.parse(it) }.getOrNull()?.isAfter(cutoff.minusDays(1)) == true }
            .associateWith { day -> prefs[intPreferencesKey(KEY_DAY_XP_PREFIX + day)] ?: 0 }
    }

    private fun recordActivityLocked(
        prefs: MutablePreferences,
        dayXp: Int,
        now: Long,
    ) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastStr = prefs[KEY_LAST_ACTIVE_DAY]

        val days = prefs[KEY_ACTIVE_DAYS].orEmpty().toMutableSet()
        val isNewDay = lastStr != todayStr
        if (isNewDay) {
            days.add(todayStr)
            // Cap stored set to ~400 entries to bound DataStore size.
            while (days.size > 400) {
                days.remove(days.minOrNull())
            }
            val lastDate = runCatching { LocalDate.parse(lastStr) }.getOrNull()
            val continued = lastDate != null && lastDate.plusDays(1) == today
            prefs[KEY_STREAK] = if (continued) (prefs[KEY_STREAK] ?: 0) + 1 else 1
            prefs[KEY_LAST_ACTIVE_DAY] = todayStr
        }
        if (dayXp > 0) {
            val key = intPreferencesKey(KEY_DAY_XP_PREFIX + todayStr)
            prefs[key] = (prefs[key] ?: 0) + dayXp
        }
        prefs[KEY_ACTIVE_DAYS] = days
    }

    // ---- Mastery with decay ----

    suspend fun recordQuizResult(topic: String, gotIt: Boolean, now: Long = System.currentTimeMillis()) {
        context.progressDataStore.edit { prefs ->
            val key = stringPreferencesKey(masteryKey(topic))
            val current = readMasteryLocked(prefs, topic, now)
            val updated = if (gotIt) {
                // Diminishing push toward 1.0: +0.12 * (1 - m).
                (current.score + 0.12f * (1f - current.score)).coerceIn(0f, 1f)
            } else {
                // Fast pull down on "Again".
                (current.score - 0.25f * (current.score + 0.2f)).coerceIn(0f, 1f)
            }
            prefs[key] = json.encodeToString(
                TopicMastery.serializer(),
                TopicMastery(score = updated, updatedAtMillis = now),
            )
            recordActivityLocked(prefs, dayXp = 0, now = now)
        }
    }

    suspend fun getMastery(topic: String, now: Long = System.currentTimeMillis()): Float {
        val prefs = context.progressDataStore.data.first()
        return readMasteryLocked(prefs, topic, now).score
    }

    suspend fun getAllMastery(
        topics: List<String>,
        now: Long = System.currentTimeMillis(),
    ): Map<String, Float> {
        val prefs = context.progressDataStore.data.first()
        return topics.associateWith { readMasteryLocked(prefs, it, now).score }
    }

    private fun readMasteryLocked(prefs: Preferences, topic: String, now: Long): TopicMastery {
        val raw = prefs[stringPreferencesKey(masteryKey(topic))] ?: return TopicMastery(0f, now)
        val stored = runCatching { json.decodeFromString(TopicMastery.serializer(), raw) }
            .getOrNull() ?: return TopicMastery(0f, now)
        return stored.copy(score = decayedMastery(stored.score, stored.updatedAtMillis, now))
    }

    private fun masteryKey(topic: String) = "mastery_" + topic.lowercase().replace("[^a-z0-9]+".toRegex(), "_")

    // ---- Scroll position (resume-where-left-off) ----

    suspend fun saveScrollPosition(reelId: String?, index: Int) {
        context.progressDataStore.edit { prefs ->
            if (reelId == null) prefs.remove(KEY_SCROLL_REEL_ID) else prefs[KEY_SCROLL_REEL_ID] = reelId
            prefs[KEY_SCROLL_INDEX] = index
        }
    }

    // ---- Placement / prefs ----

    suspend fun setPlacementComplete(skipBeginner: Boolean) {
        context.progressDataStore.edit { prefs ->
            prefs[KEY_PLACEMENT_DONE] = true
            prefs[KEY_PLACEMENT_SKIPPED_BEGINNER] = skipBeginner
        }
    }

    suspend fun didSkipBeginner(): Boolean =
        context.progressDataStore.data.first()[KEY_PLACEMENT_SKIPPED_BEGINNER] ?: false

    suspend fun setAnalyticsOptOut(optOut: Boolean) {
        context.progressDataStore.edit { it[KEY_ANALYTICS_OPT_OUT] = optOut }
    }

    suspend fun setLastSync(millis: Long) {
        context.progressDataStore.edit { it[KEY_LAST_SYNC] = millis }
    }

    suspend fun getLastSync(): Long =
        context.progressDataStore.data.first()[KEY_LAST_SYNC] ?: 0L

    companion object {
        /**
         * 2% multiplicative decay per inactive day: score * 0.98^days.
         * Pure function so rust-core, tests, and UI share identical math.
         */
        fun decayedMastery(raw: Float, updatedAtMillis: Long, nowMillis: Long): Float {
            if (raw <= 0f) return 0f
            val daysInactive = ((nowMillis - updatedAtMillis) / 86_400_000L).coerceAtLeast(0L)
            if (daysInactive == 0L) return raw
            return (raw * 0.98.pow(daysInactive.toDouble())).toFloat().coerceIn(0f, 1f)
        }
    }
}

@Serializable
data class TopicMastery(
    val score: Float,
    val updatedAtMillis: Long,
)

data class ScrollPosition(
    val reelId: String?,
    val index: Int,
)
