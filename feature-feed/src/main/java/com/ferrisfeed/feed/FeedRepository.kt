package com.ferrisfeed.feed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Track mirrors core-ui Tracks + content schema tracks. */
enum class Track(val id: String) {
    RUST("rust"),
    WASM("wasm"),
    SYSTEM_DESIGN("system-design");

    companion object {
        fun fromId(id: String): Track = entries.firstOrNull { it.id == id } ?: RUST
    }
}

enum class QuizType { MCQ, TAP_BUG, FILL_BLANK }

data class QuizModel(
    val type: QuizType,
    val question: String,
    val options: List<String> = emptyList(),
    val answerIndex: Int = 0,
    // Tap-the-bug
    val codeLines: List<String> = emptyList(),
    val buggyLineIndex: Int = -1,
    // Fill-blank
    val prefix: String = "",
    val suffix: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val explanation: String,
)

data class Reel(
    val id: String,
    val track: Track,
    val level: Int,
    val hook: String,
    val bodyMd: String,
    val code: String?,
    val language: String,
    val takeaway: String,
    val trap: String,
    val trapCompilerMessage: String?,
    val quiz: QuizModel,
    val tags: List<String> = emptyList(),
    // SRS state (mirrors rust-core FSRS-lite fields)
    val dueAtEpochMs: Long = 0L,
    val stability: Float = 0f,
    val isNew: Boolean = true,
    val pathOrder: Int = 0,
)

/** Minimal contract the :data module must fulfill (Room + prepackaged asset impl). */
interface ReelLocalDataSource {
    fun observeReels(): Flow<List<Reel>>
    suspend fun allReels(): List<Reel>
    suspend fun getReel(id: String): Reel?
    suspend fun upsertImpression(reelId: String, dwellMs: Long, skipped: Boolean)
    suspend fun setSaved(reelId: String, saved: Boolean)
    suspend fun setLiked(reelId: String, liked: Boolean)
    suspend fun savedIds(): Set<String>
    suspend fun likedIds(): Set<String>
    suspend fun recordGrade(reelId: String, correct: Boolean, label: String)
}

/** Feed repository: pure ordering + impression plumbing over the local data source. */
interface FeedRepository {
    fun observeQueue(): Flow<List<Reel>>
    suspend fun allReels(): List<Reel>
    suspend fun refreshQueue(shuffled: List<Reel>)
    suspend fun getReel(id: String): Reel?
    suspend fun trackImpression(reelId: String, dwellMs: Long, skipped: Boolean)
    suspend fun setSaved(reelId: String, saved: Boolean)
    suspend fun setLiked(reelId: String, liked: Boolean)
    suspend fun savedIds(): Set<String>
    suspend fun likedIds(): Set<String>
    suspend fun recordGrade(reelId: String, correct: Boolean, label: String)
}

/**
 * In-memory queue + persistent delegate. The queue order is computed by [FeedViewModel]
 * (70/20/10 shuffle) and pushed here via [refreshQueue]; impressions and grades
 * delegate to [local] so Room stays the source of truth for SRS fields.
 */
class DefaultFeedRepository(
    private val local: ReelLocalDataSource,
) : FeedRepository {
    private val queue = MutableStateFlow<List<Reel>>(emptyList())

    override fun observeQueue(): Flow<List<Reel>> = queue.asStateFlow()

    override suspend fun allReels(): List<Reel> = local.allReels()

    /** Also merges live SRS updates so due flags stay fresh without reshuffling. */
    fun observeMergedQueue(): Flow<List<Reel>> = local.observeReels().map { all ->
        val current = queue.value
        if (current.isEmpty()) all else current.map { q -> all.firstOrNull { it.id == q.id } ?: q }
    }

    override suspend fun refreshQueue(shuffled: List<Reel>) {
        queue.value = shuffled
    }

    override suspend fun getReel(id: String): Reel? =
        queue.value.firstOrNull { it.id == id } ?: local.getReel(id)

    override suspend fun trackImpression(reelId: String, dwellMs: Long, skipped: Boolean) =
        local.upsertImpression(reelId, dwellMs, skipped)

    override suspend fun setSaved(reelId: String, saved: Boolean) = local.setSaved(reelId, saved)
    override suspend fun setLiked(reelId: String, liked: Boolean) = local.setLiked(reelId, liked)
    override suspend fun savedIds(): Set<String> = local.savedIds()
    override suspend fun likedIds(): Set<String> = local.likedIds()
    override suspend fun recordGrade(reelId: String, correct: Boolean, label: String) =
        local.recordGrade(reelId, correct, label)
}
