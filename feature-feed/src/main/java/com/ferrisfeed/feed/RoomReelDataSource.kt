package com.ferrisfeed.feed

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.data.ReelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Room-backed [ReelLocalDataSource].
 *
 * Lives in :feature-feed (which depends on :data) because the [Reel] /
 * [QuizModel] UI models live here while the Room entities live in :data.
 * Saved/liked sets have no Room columns by design (user state, not content),
 * so they persist in DataStore string sets instead.
 */
class RoomReelDataSource(
    private val dao: ReelDao,
    private val dataStore: DataStore<Preferences>,
) : ReelLocalDataSource {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override fun observeReels(): Flow<List<Reel>> = combine(
        dao.observeByTrack(ReelEntity.TRACK_RUST),
        dao.observeByTrack(ReelEntity.TRACK_WASM),
        dao.observeByTrack(ReelEntity.TRACK_SYSTEM_DESIGN),
    ) { rust, wasm, sd -> (rust + wasm + sd).map { it.toFeedReel() } }

    override suspend fun allReels(): List<Reel> = observeReels().first()

    override suspend fun getReel(id: String): Reel? = dao.getById(id)?.toFeedReel()

    override suspend fun upsertImpression(reelId: String, dwellMs: Long, skipped: Boolean) {
        val entity = dao.getById(reelId) ?: return
        dao.update(entity.copy(lastSeenMillis = System.currentTimeMillis()))
    }

    override suspend fun setSaved(reelId: String, saved: Boolean) =
        toggleId(KEY_SAVED_IDS, reelId, saved)

    override suspend fun setLiked(reelId: String, liked: Boolean) =
        toggleId(KEY_LIKED_IDS, reelId, liked)

    override suspend fun savedIds(): Set<String> =
        dataStore.data.first()[KEY_SAVED_IDS].orEmpty()

    override suspend fun likedIds(): Set<String> =
        dataStore.data.first()[KEY_LIKED_IDS].orEmpty()

    /**
     * Minimal honest FSRS-lite step mirroring rust-core: correct answers grow
     * stability, wrong answers shrink it and count a lapse. Full scheduling
     * lives in `ferris-core`; this keeps Room consistent with it.
     */
    override suspend fun recordGrade(reelId: String, correct: Boolean, label: String) {
        val entity = dao.getById(reelId) ?: return
        val now = System.currentTimeMillis()
        val stability = if (correct) {
            (entity.stability * 1.5f + 1f).coerceAtLeast(1f)
        } else {
            (entity.stability * 0.6f).coerceAtLeast(0.3f)
        }
        val difficulty = if (correct) {
            (entity.difficulty - 0.5f).coerceIn(1f, 10f)
        } else {
            (entity.difficulty + 1f).coerceIn(1f, 10f)
        }
        dao.applyReview(
            id = reelId,
            nextDue = now + (stability * 86_400_000L).toLong(),
            stability = stability,
            difficulty = difficulty,
            grade = if (correct) 4 else 1,
            lapseInc = if (correct) 0 else 1,
            seenAt = now,
        )
    }

    private suspend fun toggleId(
        key: Preferences.Key<Set<String>>,
        id: String,
        on: Boolean,
    ) {
        dataStore.edit { prefs ->
            val current = prefs[key].orEmpty()
            prefs[key] = if (on) current + id else current - id
        }
    }

    private fun ReelEntity.toFeedReel(): Reel = Reel(
        id = id,
        track = Track.fromId(track),
        level = level,
        hook = hook,
        bodyMd = bodyMd,
        code = code,
        language = language.ifBlank { "rust" },
        takeaway = takeaway,
        trap = trap,
        trapCompilerMessage = null,
        quiz = parseQuiz(quizJson, code),
        tags = ReelEntity.tagsOf(tagsCsv),
        dueAtEpochMs = nextDueMillis,
        stability = stability,
        isNew = reps == 0,
        pathOrder = orderIndex,
    )

    /**
     * Parses the stored quiz JSON (see content schema: mcq / tap_bug /
     * fill_blank variants with `q`/`question` aliases). Never throws: a bad
     * row falls back to a generic check-in so one corrupt reel cannot break
     * the whole feed.
     */
    private fun parseQuiz(raw: String?, code: String?): QuizModel {
        val fallback = QuizModel(
            type = QuizType.MCQ,
            question = "What was the key takeaway?",
            options = listOf("Got it", "Still fuzzy"),
            answerIndex = 0,
            explanation = "Re-read the takeaway above.",
        )
        if (raw.isNullOrBlank()) return fallback
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            fun text(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { key ->
                    obj[key]?.jsonPrimitive?.contentOrNull()
                }
            fun int(vararg keys: String): Int? =
                keys.firstNotNullOfOrNull { key ->
                    obj[key]?.jsonPrimitive?.intOrNull
                }
            val question = text("question", "q") ?: return@runCatching fallback
            val explanation = text("explanation", "explain").orEmpty()
            val lines = obj["lines"]?.jsonArray?.map { it.jsonPrimitive.content }
                ?: obj["codeLines"]?.jsonArray?.map { it.jsonPrimitive.content }
                .orEmpty()
            val buggy = int("buggyLineIndex", "buggy")
            if (lines.isNotEmpty() && buggy != null) {
                return@runCatching QuizModel(
                    type = QuizType.TAP_BUG,
                    question = question,
                    codeLines = lines,
                    buggyLineIndex = buggy,
                    explanation = explanation,
                )
            }
            val accepted = obj["acceptedAnswers"]?.jsonArray?.map { it.jsonPrimitive.content }
                ?: obj["answers"]?.jsonArray?.map { it.jsonPrimitive.content }
                .orEmpty()
            if (accepted.isNotEmpty()) {
                return@runCatching QuizModel(
                    type = QuizType.FILL_BLANK,
                    question = question,
                    prefix = text("prefix").orEmpty(),
                    suffix = text("suffix").orEmpty(),
                    acceptedAnswers = accepted,
                    explanation = explanation,
                )
            }
            QuizModel(
                type = QuizType.MCQ,
                question = question,
                options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }
                    .orEmpty()
                    .ifEmpty { fallback.options },
                answerIndex = int("answer", "answerIndex") ?: 0,
                explanation = explanation,
            )
        }.getOrDefault(fallback)
    }

    private companion object {
        val KEY_SAVED_IDS = stringSetPreferencesKey("feed_saved_ids")
        val KEY_LIKED_IDS = stringSetPreferencesKey("feed_liked_ids")
    }
}
