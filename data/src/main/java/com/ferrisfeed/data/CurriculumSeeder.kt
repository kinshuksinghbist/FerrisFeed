package com.ferrisfeed.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * First-launch seeder: populates Room from the validated curriculum JSON
 * bundled in APK assets (synced from repo-root `content/` by the
 * `:app:syncCurriculumAssets` task into generated assets, merged at the APK
 * root as `rust/` and `system-design/` — never hand-copy files there).
 * WASM content stays dormant in the repo and is deliberately NOT seeded.
 *
 * This replaces a prepackaged SQLite asset (`createFromAsset`), which would
 * crash on every launch until a release pipeline generates a Room-valid DB
 * (identity hash and all). Seeding from JSON lets Room create its own schema
 * so there is nothing to keep in sync. Idempotent: skips when rows exist.
 */
class CurriculumSeeder(
    private val context: Context,
    private val dao: ReelDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Inserts all bundled reels. Returns the inserted row count. */
    suspend fun seedIfEmpty(): Int {
        if (dao.countAll() > 0) return 0
        var total = 0
        forEachPack { entities ->
            dao.upsertAll(entities)
            total += entities.size
        }
        markCurrent()
        Log.i(TAG, "Curriculum seed complete: $total reels")
        return total
    }

    /**
     * Content refresh for installs seeded by an older build (redesign v3
     * re-labelled every topic and rewrote quizzes). Runs once per
     * [CONTENT_VERSION] bump: lesson text, topic, level and quiz come from the
     * bundled packs; every SRS column (due date, stability, reps, lapses...)
     * is kept, so nobody loses review history. New reels are inserted.
     */
    suspend fun refreshIfStale(): Int {
        // An empty table (first launch, or a destructive migration) always seeds.
        if (dao.countAll() == 0) return seedIfEmpty()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_CONTENT_VERSION, 0) >= CONTENT_VERSION) return 0
        var total = 0
        forEachPack { fresh ->
            val merged = fresh.map { reel ->
                val old = dao.getById(reel.id) ?: return@map reel
                old.copy(
                    track = reel.track,
                    level = reel.level,
                    topic = reel.topic,
                    hook = reel.hook,
                    bodyMd = reel.bodyMd,
                    code = reel.code,
                    output = reel.output,
                    language = reel.language,
                    takeaway = reel.takeaway,
                    trap = reel.trap,
                    quizJson = reel.quizJson,
                    orderIndex = reel.orderIndex,
                )
            }
            dao.upsertAll(merged)
            total += merged.size
        }
        markCurrent()
        Log.i(TAG, "Curriculum refreshed to v$CONTENT_VERSION: $total reels")
        return total
    }

    private fun markCurrent() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CONTENT_VERSION, CONTENT_VERSION)
            .apply()
    }

    private suspend fun forEachPack(block: suspend (List<ReelEntity>) -> Unit) {
        for ((dir, track) in TRACK_DIRS) {
            val files = runCatching {
                context.assets.list(dir).orEmpty()
            }.getOrDefault(emptyArray())
            for (file in files.filter { it.endsWith(".json") }) {
                val entities = parsePack(dir, track, file)
                if (entities.isNotEmpty()) block(entities)
            }
        }
    }

    private fun parsePack(dir: String, track: String, file: String): List<ReelEntity> {
        val topic = topicForFile(file)
        return runCatching {
            val raw = context.assets
                .open("$dir/$file")
                .bufferedReader()
                .use { it.readText() }
            json.parseToJsonElement(raw).jsonArray.mapIndexedNotNull { index, element ->
                runCatching { element.jsonObject.toReelEntity(track, topic, index) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "CurriculumSeeder"

        /** Bump whenever bundled lesson content changes in a way installs must pick up. */
        const val CONTENT_VERSION = 3
        private const val PREFS = "ferris_content"
        private const val KEY_CONTENT_VERSION = "content_version"

        private val TRACK_DIRS = listOf(
            "rust" to ReelEntity.TRACK_RUST,
            "system-design" to ReelEntity.TRACK_SYSTEM_DESIGN,
        )

        /**
         * `beginner2_ownership.json` -> `ownership`,
         * `rust_mixed_drills.json` -> `drills`. Coarse but stable; the path
         * engine groups by track first and topic second.
         */
        fun topicForFile(file: String): String =
            file.substringBeforeLast(".")
                .substringAfterLast("_", missingDelimiterValue = file.substringBeforeLast("."))
                .replace("-", "_")
                .ifBlank { "general" }
    }
}

/**
 * TODO 21: author-provided topic label wins; the file-derived key stays the
 * fallback for older packs that omit `topic_label`. The topic column is the
 * single grouping id: the path engine, feed filter, search filters, and
 * mastery keys all read this exact string.
 */
internal fun JsonObject.toReelEntity(
    track: String,
    topic: String,
    orderIndex: Int,
): ReelEntity {
    fun text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            // JsonNull CONTENT is the literal "null" string, not a null value,
            // so a JSON `"key": null` would otherwise be stored as "null".
            val element = this[key]
            if (element is JsonNull) null
            else runCatching { element?.jsonPrimitive?.content }.getOrNull()
        }
    // Quiz is stored back as raw JSON so the UI parser and any future
    // Rust grader share one format (see RoomReelDataSource.parseQuiz).
    val quizElement = (this["quiz"] ?: this["Quiz"]).takeUnless { element -> element is JsonNull }
    return ReelEntity(
        id = text("id") ?: error("reel without id"),
        track = text("track") ?: track,
        level = this["level"]?.jsonPrimitive?.intOrNull ?: 1,
        topic = text("topic_label")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: topic,
        hook = text("hook").orEmpty(),
        bodyMd = text("body_md", "bodyMd").orEmpty(),
        code = text("code"),
        output = text("output"),
        language = text("language") ?: "rust",
        takeaway = text("takeaway").orEmpty(),
        trap = text("trap").orEmpty(),
        // JsonObject.toString() is defined to emit valid JSON, which is
        // all the UI parser needs (see RoomReelDataSource.parseQuiz).
        quizJson = quizElement?.toString(),
        orderIndex = orderIndex,
    )
}
