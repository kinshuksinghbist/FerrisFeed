package com.ferrisfeed.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
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
        for ((dir, track) in TRACK_DIRS) {
            val files = runCatching {
                context.assets.list(dir).orEmpty()
            }.getOrDefault(emptyArray())
            for (file in files.filter { it.endsWith(".json") }) {
                total += seedFile(dir, track, file)
            }
        }
        Log.i(TAG, "Curriculum seed complete: $total reels")
        return total
    }

    private suspend fun seedFile(dir: String, track: String, file: String): Int {
        val topic = topicForFile(file)
        return runCatching {
            val raw = context.assets
                .open("$dir/$file")
                .bufferedReader()
                .use { it.readText() }
            val rows = json.parseToJsonElement(raw).jsonArray
            val entities = rows.mapIndexedNotNull { index, element ->
                runCatching { element.jsonObject.toEntity(track, topic, index) }
                    .getOrNull()
            }
            if (entities.isNotEmpty()) dao.upsertAll(entities)
            entities.size
        }.getOrDefault(0)
    }

    private fun kotlinx.serialization.json.JsonObject.toEntity(
        track: String,
        topic: String,
        orderIndex: Int,
    ): ReelEntity {
        fun text(vararg keys: String): String? =
            keys.firstNotNullOfOrNull { key ->
                runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()
            }
        // Quiz is stored back as raw JSON so the UI parser and any future
        // Rust grader share one format (see RoomReelDataSource.parseQuiz).
        val quizElement = this["quiz"] ?: this["Quiz"]
        return ReelEntity(
            id = text("id") ?: error("reel without id"),
            track = text("track") ?: track,
            level = this["level"]?.jsonPrimitive?.intOrNull ?: 1,
            topic = topic,
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

    companion object {
        private const val TAG = "CurriculumSeeder"

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
