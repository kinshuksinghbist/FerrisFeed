package com.ferrisfeed.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.Curriculum
import com.ferrisfeed.data.CurriculumTopic
import com.ferrisfeed.data.CurriculumUnit
import com.ferrisfeed.data.ProgressStore
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.data.ReelEntity
import com.ferrisfeed.data.SearchFilters as DataSearchFilters
import com.ferrisfeed.data.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One topic row on the Path. */
data class TopicUi(
    val key: String,
    val title: String,
    val blurb: String,
    val track: String,
    val reelCount: Int,
    val seenCount: Int,
    val mastery: Float,
) {
    val status: TopicStatus
        get() = when {
            mastery >= MASTERED || (seenCount >= reelCount && reelCount > 0 && mastery >= 0.5f) -> TopicStatus.Mastered
            seenCount > 0 -> TopicStatus.InProgress
            else -> TopicStatus.New
        }

    companion object {
        const val MASTERED = 0.85f
    }
}

enum class TopicStatus { New, InProgress, Mastered }

/** One unit section on the Path. */
data class UnitUi(
    val id: String,
    val number: Int,
    val title: String,
    val blurb: String,
    val topics: List<TopicUi>,
) {
    val reelCount: Int get() = topics.sumOf { it.reelCount }
    val seenCount: Int get() = topics.sumOf { it.seenCount }
    val progress: Float get() = if (reelCount == 0) 0f else seenCount.toFloat() / reelCount
}

data class PathUiState(
    val track: String = Curriculum.RUST,
    val units: List<UnitUi> = emptyList(),
    val upNext: TopicUi? = null,
    val upNextUnit: UnitUi? = null,
    val isLoading: Boolean = true,
    val searchOpen: Boolean = false,
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
) {
    val topicCount: Int get() = units.sumOf { it.topics.size }
    val startedCount: Int get() = units.sumOf { u -> u.topics.count { it.status != TopicStatus.New } }
    val masteredCount: Int get() = units.sumOf { u -> u.topics.count { it.status == TopicStatus.Mastered } }
}

/**
 * Path tab (v3): the curriculum as units → topics for one track at a time,
 * a single "Up next" recommendation, and lesson search behind a search
 * button. Counts come from Room (query-only projections), mastery from
 * [ProgressStore] with its time decay. [refresh] runs every time the tab is
 * shown so progress made in the feed is visible immediately.
 */
@HiltViewModel
class PathViewModel @Inject constructor(
    private val dao: ReelDao,
    private val progressStore: ProgressStore,
) : ViewModel() {

    private val searchRepository = SearchRepository(dao)
    private val _state = MutableStateFlow(PathUiState())
    val state: StateFlow<PathUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var trackChosen = false

    fun refresh() {
        viewModelScope.launch {
            if (!trackChosen) {
                val focus = progressStore.learnerProfile.first().trackFocus
                _state.update { it.copy(track = trackForFocus(focus)) }
                trackChosen = true
            }
            load(_state.value.track)
        }
    }

    fun selectTrack(track: String) {
        if (track == _state.value.track) return
        trackChosen = true
        _state.update { it.copy(track = track) }
        viewModelScope.launch { load(track) }
    }

    private suspend fun load(track: String) {
        val counts = dao.countByTopic(track).associateBy { it.topic }
        val seen = dao.countSeenByTopic(track).associate { it.topic to it.reelCount }
        val mastery = progressStore.getAllMastery(counts.keys.toList())

        fun ui(topic: CurriculumTopic) = TopicUi(
            key = topic.key,
            title = topic.title,
            blurb = topic.blurb,
            track = track,
            reelCount = counts[topic.key]?.reelCount ?: 0,
            seenCount = seen[topic.key] ?: 0,
            mastery = mastery[topic.key] ?: 0f,
        )

        val units = Curriculum.unitsFor(track).map { unit: CurriculumUnit ->
            UnitUi(
                id = unit.id,
                number = unit.number,
                title = unit.title,
                blurb = unit.blurb,
                topics = unit.topics.map(::ui).filter { it.reelCount > 0 },
            )
        }.filter { it.topics.isNotEmpty() }.toMutableList()

        // Topics a future content pack adds before the map knows them.
        val known = Curriculum.unitsFor(track).flatMap { u -> u.topics.map { it.key } }.toSet()
        val extras = counts.keys.filter { it !in known }.sorted().map { key ->
            ui(CurriculumTopic(key, Curriculum.titleOf(key), "New lessons"))
        }
        if (extras.isNotEmpty()) {
            units += UnitUi("$track-extra", units.size + 1, "More lessons", "Fresh topics from content updates.", extras)
        }

        val flat = units.flatMap { u -> u.topics.map { u to it } }
        val next = flat.firstOrNull { (_, t) -> t.status == TopicStatus.InProgress }
            ?: flat.firstOrNull { (_, t) -> t.status == TopicStatus.New }
        _state.update {
            it.copy(
                units = units,
                upNext = next?.second,
                upNextUnit = next?.first,
                isLoading = false,
            )
        }
    }

    fun setSearchOpen(open: Boolean) {
        _state.update { it.copy(searchOpen = open) }
        if (!open) {
            searchJob?.cancel()
            _state.update { it.copy(query = "", filters = SearchFilters(), results = emptyList(), isSearching = false) }
        }
    }

    /** Room FTS search; a blank query with any filter browses instead. */
    fun onQueryChanged(query: String, filters: SearchFilters) {
        _state.update { it.copy(query = query, filters = filters) }
        searchJob?.cancel()
        if (!hasActiveBrowse(query, filters)) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(150) // debounce keystrokes
            val hits = runCatching {
                searchRepository.search(query = query, filters = filters.toDataFilters())
            }.getOrDefault(emptyList())
            _state.update { s ->
                s.copy(results = hits.map { entity -> entity.toSearchResult() }, isSearching = false)
            }
        }
    }

    companion object {
        /** Level bounds for the Room filter query (1..4 per content schema). */
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 4

        /** Track for a learner focus value; "both" shows Rust first. */
        fun trackForFocus(focus: String): String =
            if (focus == Curriculum.SYSTEM_DESIGN) Curriculum.SYSTEM_DESIGN else Curriculum.RUST
    }
}

/**
 * UI filter -> Room filter. Pure and unit-tested: a topic survives verbatim
 * (exact-match column value) and an unset level spans the whole 1..4 range.
 */
internal fun SearchFilters.toDataFilters(): DataSearchFilters = DataSearchFilters(
    track = track,
    minLevel = level ?: PathViewModel.MIN_LEVEL,
    maxLevel = level ?: PathViewModel.MAX_LEVEL,
    hasCode = hasCode,
    hasQuiz = hasQuiz,
    topic = topic,
)

/** Anything the user narrowed (query text or any chip) counts as an active browse. */
internal fun hasActiveBrowse(query: String, filters: SearchFilters): Boolean =
    query.isNotBlank() || filters.isScoped()

private fun ReelEntity.toSearchResult(): SearchResult = SearchResult(
    id = id,
    track = track,
    level = level,
    topic = topic,
    hook = hook,
    takeaway = takeaway,
    hasCode = hasCode,
    hasQuiz = hasQuiz,
    snippet = bodyMd.take(140),
)
