package com.ferrisfeed.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Data access for reels, SRS due queue, and full-text search. */
@Dao
interface ReelDao {

    // ---- Basic reads ----

    @Query("SELECT * FROM reels WHERE id = :id")
    suspend fun getById(id: String): ReelEntity?

    @Query("SELECT * FROM reels WHERE id = :id")
    fun observeById(id: String): Flow<ReelEntity?>

    @Query(
        "SELECT * FROM reels WHERE track = :track ORDER BY level ASC, orderIndex ASC",
    )
    fun observeByTrack(track: String): Flow<List<ReelEntity>>

    @Query(
        "SELECT * FROM reels WHERE track = :track AND level = :level " +
            "ORDER BY orderIndex ASC",
    )
    suspend fun getByTrackLevel(track: String, level: Int): List<ReelEntity>

    @Query(
        "SELECT * FROM reels WHERE topic = :topic ORDER BY level ASC, orderIndex ASC",
    )
    suspend fun getByTopic(topic: String): List<ReelEntity>

    @Query("SELECT DISTINCT topic FROM reels WHERE track = :track ORDER BY topic ASC")
    suspend fun topicsForTrack(track: String): List<String>

    @Query("SELECT COUNT(*) FROM reels")
    suspend fun countAll(): Int

    // ---- SRS due queue (drives 70% of feed shuffle) ----

    /**
     * Cards due for review. New cards (reps = 0, nextDue = 0) are excluded;
     * use [getNewCardsInPathOrder] for those.
     */
    @Query(
        "SELECT * FROM reels WHERE reps > 0 AND nextDueMillis <= :now " +
            "ORDER BY nextDueMillis ASC LIMIT :limit",
    )
    suspend fun getDueCards(now: Long, limit: Int): List<ReelEntity>

    @Query(
        "SELECT * FROM reels WHERE reps = 0 AND (:track IS NULL OR track = :track) " +
            "ORDER BY level ASC, orderIndex ASC LIMIT :limit",
    )
    suspend fun getNewCardsInPathOrder(track: String?, limit: Int): List<ReelEntity>

    /** Weak spots: reviewed but low stability or recent lapse. */
    @Query(
        "SELECT * FROM reels WHERE reps > 0 AND (stability < :maxStability OR lapses > 0) " +
            "ORDER BY stability ASC, lapses DESC LIMIT :limit",
    )
    suspend fun getWeakCards(maxStability: Float = 2.5f, limit: Int): List<ReelEntity>

    // ---- Full-text search (FTS4) ----

    /**
     * Raw FTS match. Caller must sanitize [matchQuery] (see [SearchRepository]).
     * Uses the auto-generated content-sync triggers from [ReelFts].
     */
    @Query(
        "SELECT r.* FROM reels AS r JOIN reel_fts AS f ON r.rowid = f.rowid " +
            "WHERE reel_fts MATCH :matchQuery LIMIT :limit",
    )
    suspend fun searchFts(matchQuery: String, limit: Int = 50): List<ReelEntity>

    /**
     * FTS + structured filters in one query so the roadmap / search UI stays
     * a single Room observation without in-memory post-filtering.
     */
    @Query(
        "SELECT r.* FROM reels AS r JOIN reel_fts AS f ON r.rowid = f.rowid " +
            "WHERE reel_fts MATCH :matchQuery " +
            "AND (:track IS NULL OR r.track = :track) " +
            "AND r.level BETWEEN :minLevel AND :maxLevel " +
            "AND (:hasCode IS NULL OR (:hasCode = 1 AND r.code IS NOT NULL AND r.code != '') " +
            "OR (:hasCode = 0)) " +
            "AND (:hasQuiz IS NULL OR (:hasQuiz = 1 AND r.quizJson IS NOT NULL AND r.quizJson != '') " +
            "OR (:hasQuiz = 0)) " +
            "AND (:topic IS NULL OR r.topic = :topic) " +
            "LIMIT :limit",
    )
    suspend fun searchFtsFiltered(
        matchQuery: String,
        track: String?,
        minLevel: Int,
        maxLevel: Int,
        hasCode: Int?,
        hasQuiz: Int?,
        topic: String?,
        limit: Int,
    ): List<ReelEntity>

    /** Non-FTS fallback for empty query: pure filter browse. */
    @Query(
        "SELECT * FROM reels " +
            "WHERE (:track IS NULL OR track = :track) " +
            "AND level BETWEEN :minLevel AND :maxLevel " +
            "AND (:topic IS NULL OR topic = :topic) " +
            "AND (:hasCode IS NULL OR (:hasCode = 1 AND code IS NOT NULL AND code != '')) " +
            "AND (:hasQuiz IS NULL OR (:hasQuiz = 1 AND quizJson IS NOT NULL AND quizJson != '')) " +
            "ORDER BY level ASC, orderIndex ASC LIMIT :limit",
    )
    suspend fun browseFiltered(
        track: String?,
        minLevel: Int,
        maxLevel: Int,
        topic: String?,
        hasCode: Int?,
        hasQuiz: Int?,
        limit: Int,
    ): List<ReelEntity>

    // ---- Writes ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reels: List<ReelEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(reels: List<ReelEntity>): List<Long>

    @Update
    suspend fun update(reel: ReelEntity)

    @Query(
        "UPDATE reels SET nextDueMillis = :nextDue, stability = :stability, " +
            "difficulty = :difficulty, reps = reps + 1, lapses = lapses + :lapseInc, " +
            "lastGrade = :grade, lastSeenMillis = :seenAt WHERE id = :id",
    )
    suspend fun applyReview(
        id: String,
        nextDue: Long,
        stability: Float,
        difficulty: Float,
        grade: Int,
        lapseInc: Int,
        seenAt: Long,
    )

    @Transaction
    suspend fun applyReviews(
        updates: List<SrsUpdate>,
        seenAt: Long,
    ) {
        updates.forEach {
            applyReview(it.id, it.nextDueMillis, it.stability, it.difficulty, it.grade, it.lapseInc, seenAt)
        }
    }
}

/** Value object for batch SRS updates computed by the FSRS scheduler. */
data class SrsUpdate(
    val id: String,
    val nextDueMillis: Long,
    val stability: Float,
    val difficulty: Float,
    /** 1 = Again, 3 = Hard, 4 = Got it. */
    val grade: Int,
    val lapseInc: Int,
)
