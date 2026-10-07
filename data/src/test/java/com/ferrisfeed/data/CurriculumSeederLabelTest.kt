package com.ferrisfeed.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM tests for the TODO 21 topic-label rules.
 *
 * `toReelEntity` is a pure JsonObject -> ReelEntity function (no Android
 * types), so the seeder's label behavior is tested here without Robolectric:
 * a per-reel `topic_label` wins over the file-derived key, is trimmed, and
 * omission/blank/JSON-null fall back to the pack key. Also pins the seeder's
 * file-key derivation so renames cannot silently regroup the roadmap.
 */
class CurriculumSeederLabelTest {

    private fun reel(jsonText: String) = Json.parseToJsonElement(jsonText).jsonObject
        .toReelEntity(track = "rust", topic = "ownership", orderIndex = 0)

    private fun reelJson(extra: String) = """
        {
          "id": "rust-b2-001",
          "track": "rust",
          "level": 1,
          "hook": "Rust has no garbage collector yet.",
          "body_md": "Ownership moves values by default.",
          "code": "fn main() {}",
          "language": "rust",
          "takeaway": "Move by default; borrow with ampersand.",
          "trap": "Using a moved value again.",
          $extra
          "quiz": {"type": "mcq", "question": "Q?", "options": ["A", "B"], "answer": 0, "explanation": "Because."},
          "output": "hi"
        }
    """.trimIndent()

    @Test
    fun `topic_label wins over the file-derived key`() {
        val entity = reel(reelJson("\"topic_label\": \"Ownership\","))
        assertEquals("Ownership", entity.topic)
    }

    @Test
    fun `topic_label is trimmed before storage`() {
        val entity = reel(reelJson("\"topic_label\": \"  Ownership  \","))
        assertEquals("Ownership", entity.topic)
    }

    @Test
    fun `missing topic_label falls back to the file-derived key`() {
        val entity = reel(reelJson(""))
        assertEquals("ownership", entity.topic)
    }

    @Test
    fun `JSON-null topic_label falls back instead of storing the string null`() {
        // JsonNull.content is the literal "null" string; the seeder must not
        // store that as a label (regression for the JsonNull trap).
        val entity = reel(reelJson("\"topic_label\": null,"))
        assertEquals("ownership", entity.topic)
    }

    @Test
    fun `blank topic_label falls back to the file-derived key`() {
        val entity = reel(reelJson("\"topic_label\": \"   \","))
        assertEquals("ownership", entity.topic)
    }

    @Test
    fun `JSON-null quiz is stored as null, not the string null`() {
        val raw = """
            {
              "id": "rust-b2-001",
              "track": "rust",
              "level": 1,
              "hook": "Rust has no garbage collector yet.",
              "body_md": "Ownership moves values by default.",
              "takeaway": "Move by default; borrow with ampersand.",
              "trap": "Using a moved value again.",
              "quiz": null,
              "topic_label": "Ownership"
            }
        """.trimIndent()
        assertNull(reel(raw).quizJson)
    }

    @Test
    fun `legacy Quiz wrapper is still read`() {
        val raw = """
            {
              "id": "rust-b2-001",
              "hook": "Rust has no garbage collector yet.",
              "body_md": "Ownership moves values by default.",
              "takeaway": "Move by default; borrow with ampersand.",
              "trap": "Using a moved value again.",
              "Quiz": {"type": "mcq", "question": "Q?", "options": ["A", "B"], "answer": 0, "explanation": "Because."}
            }
        """.trimIndent()
        assertEquals("ownership", reel(raw).topic)
        // The legacy alias must survive into quizJson, not be dropped or nulled.
        assert(reel(raw).quizJson?.contains("mcq") == true)
    }

    @Test
    fun `file-derived keys are stable`() {
        assertEquals("ownership", CurriculumSeeder.topicForFile("beginner2_ownership.json"))
        assertEquals("drills", CurriculumSeeder.topicForFile("rust_mixed_drills.json"))
        assertEquals("unsafe", CurriculumSeeder.topicForFile("advanced1_unsafe.json"))
        assertEquals("cases", CurriculumSeeder.topicForFile("sd8_rust_cases.json"))
        assertEquals("weird", CurriculumSeeder.topicForFile("weird.json"))
        assertEquals("rust_mixed_stuff", CurriculumSeeder.topicForFile("rust-mixed-stuff.json"))
    }
}
