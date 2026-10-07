package com.ferrisfeed.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Full-text search over Room FTS4 with structured filters.
 *
 * Query sanitization: FTS4 `MATCH` treats `"`, `*`, `(`, `)` etc. as
 * operators. We strip anything that is not a letter, digit, or space, then
 * join tokens with `*` prefix matches (`borrow*`) so partial typing still
 * finds "borrowing". An empty query falls back to pure filter browsing.
 */
class SearchRepository(private val dao: ReelDao) {

    suspend fun search(
        query: String,
        filters: SearchFilters = SearchFilters(),
        limit: Int = 50,
    ): List<ReelEntity> {
        val match = toMatchQuery(query)
        if (match == null) {
            return dao.browseFiltered(
                track = filters.track,
                minLevel = filters.minLevel,
                maxLevel = filters.maxLevel,
                topic = filters.topic,
                hasCode = filters.hasCode?.toInt(),
                hasQuiz = filters.hasQuiz?.toInt(),
                limit = limit,
            )
        }
        // Fast path: no filters -> single-table FTS query.
        if (filters.isEmpty()) return dao.searchFts(match, limit)
        return dao.searchFtsFiltered(
            matchQuery = match,
            track = filters.track,
            minLevel = filters.minLevel,
            maxLevel = filters.maxLevel,
            hasCode = filters.hasCode?.toInt(),
            hasQuiz = filters.hasQuiz?.toInt(),
            topic = filters.topic,
            limit = limit,
        )
    }

    suspend fun topics(track: String): List<String> = dao.topicsForTrack(track)

    fun observeTrack(track: String): Flow<List<ReelEntity>> = dao.observeByTrack(track)

    fun observeDueCount(now: () -> Long): Flow<Int> = observeTrack("rust").map { list ->
        list.count { it.reps > 0 && it.nextDueMillis <= now() }
    }

    /** "borrow checker" -> "borrow* checker*" (prefix match per token). */
    fun toMatchQuery(raw: String): String? {
        val tokens = raw.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(8)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it\"*" }
    }

    private fun Boolean.toInt(): Int = if (this) 1 else 0
}

/** Structured filters shown in the search UI. Null/empty = no constraint. */
data class SearchFilters(
    /** "rust" | "system-design" | null (all). WASM is dormant. */
    val track: String? = null,
    // Levels run 1..4 per the content schema (4 = advanced). The default
    // spans the whole range so unfiltered browse never hides advanced
    // reels; PathViewModel.MIN/MAX_LEVEL mirror these bounds.
    val minLevel: Int = 1,
    val maxLevel: Int = 4,
    val topic: String? = null,
    /** null = either, true = must have code, false = must not. */
    val hasCode: Boolean? = null,
    val hasQuiz: Boolean? = null,
) {
    fun isEmpty(): Boolean =
        track == null && minLevel == 1 && maxLevel == 4 &&
            topic == null && hasCode == null && hasQuiz == null
}
