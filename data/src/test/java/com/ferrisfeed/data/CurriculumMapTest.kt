package com.ferrisfeed.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the curriculum map against the shipped content: every seeded reel's
 * `topic_label` must be a known topic of its own track, and every topic must
 * have lessons. A content edit that invents a label fails here, not on a
 * phone as an "Unknown" path row.
 */
class CurriculumMapTest {

    private val contentRoot: File = listOf(File("../content"), File("content"))
        .first { it.isDirectory }

    private fun seededReels(): List<Pair<String, String>> =
        listOf("rust", "system-design").flatMap { dir ->
            File(contentRoot, dir).listFiles { f -> f.extension == "json" }.orEmpty().flatMap { file ->
                Json.parseToJsonElement(file.readText()).jsonArray.map { el ->
                    val obj = el.jsonObject
                    obj.getValue("track").jsonPrimitive.content to obj.getValue("topic_label").jsonPrimitive.content
                }
            }
        }

    @Test
    fun `topic keys are unique across both tracks`() {
        val keys = Curriculum.units.flatMap { u -> u.topics.map { it.key } }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `every seeded reel uses a curriculum topic of its own track`() {
        val reels = seededReels()
        assertTrue("expected the 376 seeded reels, found ${reels.size}", reels.size >= 300)
        reels.forEach { (track, label) ->
            val unit = Curriculum.unitOf(label)
            assertTrue("unknown topic_label '$label'", unit != null)
            assertEquals("topic '$label' is filed under the wrong track", track, unit!!.track)
        }
    }

    @Test
    fun `every curriculum topic has at least one lesson`() {
        val used = seededReels().map { it.second }.toSet()
        Curriculum.units.flatMap { it.topics }.forEach { topic ->
            assertTrue("topic '${topic.key}' has no lessons", topic.key in used)
        }
    }

    @Test
    fun `titles and mastery keys round trip`() {
        assertEquals("Option & Result", Curriculum.titleOf("option-result"))
        assertEquals("Some Future Topic", Curriculum.titleOf("some-future topic"))
        assertEquals("option-result", Curriculum.topicForMasteryKey(Curriculum.sanitize("option-result")))
        assertNull(Curriculum.topicForMasteryKey("not_a_topic"))
    }

    @Test
    fun `curriculum order puts the first unit first and unknown topics last`() {
        assertEquals(0, Curriculum.orderOf("cargo-toolchain"))
        assertTrue(Curriculum.orderOf("ownership") < Curriculum.orderOf("unsafe-ffi"))
        assertEquals(Curriculum.UNKNOWN_ORDER, Curriculum.orderOf("mystery"))
        assertEquals(0, Curriculum.unitIndexInTrack("apis-protocols"))
    }
}
