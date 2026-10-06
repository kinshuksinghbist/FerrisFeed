package com.ferrisfeed.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Room entity for a single 60-second learning reel.
 *
 * Mirrors the enforced content schema (reel schema spec):
 * id, track, level, hook, body_md (<=70 words), code (<=15 lines),
 * takeaway, trap, quiz JSON. Extra columns support SRS scheduling,
 * feed ordering, and offline search without joining other tables.
 *
 * track: "rust" | "wasm" | "system-design"
 * level: 1 = beginner, 2 = intermediate, 3 = advanced
 */
@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val id: String,
    val track: String,
    val level: Int,
    /** Fine-grained topic key, e.g. "ownership", "lifetimes", "axum", "rate-limiter". */
    val topic: String,
    val hook: String,
    @ColumnInfo(name = "body_md") val bodyMd: String,
    /** Nullable: some theory reels have no code. Max ~15 lines when present. */
    val code: String?,
    /** "rust" | "toml" | "wat" | "typescript" | "" when [code] is null. */
    val language: String = "rust",
    val takeaway: String,
    /** Common compiler error / misconception. */
    val trap: String = "",
    /**
     * Serialized quiz: {"q":"...","options":[...],"answer":0,"explain":"..."}.
     * Stored as raw JSON so the Rust quiz grader and Kotlin UI share one format.
     */
    val quizJson: String? = null,
    /** Comma-separated tags, e.g. "borrowck,move,slice". Queried via LIKE. */
    val tagsCsv: String = "",
    /** Estimated seconds to read (<90). Used for feed UX + analytics buckets. */
    val readTimeSec: Int = 60,
    /** Author-defined order inside (track, level, topic). */
    val orderIndex: Int = 0,
    /** Optional bundled illustration asset path, e.g. "img/own-move.png". */
    val imageAsset: String? = null,

    // ---- SRS scheduling columns (FSRS-lite mirror of rust-core) ----
    /** Epoch millis when this card becomes due. 0 = new / never seen. */
    val nextDueMillis: Long = 0L,
    /** FSRS stability (days). Higher = remembers longer. */
    val stability: Float = 0f,
    /** FSRS difficulty 1..10. */
    val difficulty: Float = 5f,
    /** Total reviews. */
    val reps: Int = 0,
    /** Consecutive "Again" lapses. */
    val lapses: Int = 0,
    /** Last grade: 1=Again, 3=Hard, 4=Got it. 0 = unseen. */
    val lastGrade: Int = 0,
    val lastSeenMillis: Long = 0L,
) {
    companion object {
        const val TRACK_RUST = "rust"
        const val TRACK_WASM = "wasm"
        const val TRACK_SYSTEM_DESIGN = "system-design"

        fun tagsOf(csv: String): List<String> =
            csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val tags: List<String> get() = tagsOf(tagsCsv)
    val hasCode: Boolean get() = !code.isNullOrBlank()
    val hasQuiz: Boolean get() = !quizJson.isNullOrBlank()
    val isNew: Boolean get() = reps == 0
}

/**
 * FTS4 index over the human-readable text columns.
 * Kept in sync automatically via contentEntity (Room generates triggers).
 */
@Fts4(contentEntity = ReelEntity::class)
@Entity(tableName = "reel_fts")
data class ReelFts(
    val hook: String,
    @ColumnInfo(name = "body_md") val bodyMd: String,
    val takeaway: String,
    val trap: String,
    val topic: String,
)
