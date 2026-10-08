package com.ferrisfeed.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.ProgressStore
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.data.ReelEntity
import com.ferrisfeed.data.SearchFilters as DataSearchFilters
import com.ferrisfeed.data.SearchRepository
import com.ferrisfeed.data.TopicCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Path tab (Spec v2, S7): the live roadmap plus the search
 * section that now lives at the top of the same screen.
 */
data class PathUiState(
    val nodes: List<PathNode> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val exploringTopic: PathNode? = null,
    val exploringReels: List<SearchResult> = emptyList(),
)

/**
 * Path tab ViewModel.
 *
 * Roadmap nodes are REAL data: one node per topic that actually has reels in
 * Room ([ReelDao.countByTopic] for the reel count + lowest level), with mastery
 * from [ProgressStore.getAllMastery] (which applies the 2%-per-idle-day decay).
 * The old `defaultPathNodes()` demo values are preview-only now — production
 * shows 0% everywhere on a fresh install and fills in as quizzes are answered.
 *
 * Search is the same Room FTS path the old Search tab used; it moved here so
 * the bottom nav only has Feed + Path. TODO 22 makes Browse topic-first:
 * a blank query with a topic filter browses that topic's reels (the same
 * stored topic string the roadmap nodes and Route.TopicFeed key on), so the
 * three entries into a topic — roadmap node, browse chip, tooling feed —
 * share one id and one mental model.
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

    init {
        refresh()
    }

    /** Loads all reels for a specific topic to allow exploring and reviewing previous reels. */
    fun exploreTopic(node: PathNode?) {
        if (node == null) {
            _state.update { it.copy(exploringTopic = null, exploringReels = emptyList()) }
            return
        }
        viewModelScope.launch {
            val reels = dao.getByTopic(node.id).map { it.toSearchResult() }
            _state.update { it.copy(exploringTopic = node, exploringReels = reels) }
        }
    }

    /** Rebuilds the roadmap from Room + persisted mastery. */
    fun refresh() {
        viewModelScope.launch {
            val rust = dao.countByTopic(ReelEntity.TRACK_RUST)
            val systemDesign = dao.countByTopic(ReelEntity.TRACK_SYSTEM_DESIGN)
            val topics = rust.map { it.topic } + systemDesign.map { it.topic }
            val mastery = progressStore.getAllMastery(topics)
            val nodes = buildList {
                rust.forEach { add(it.toPathNode(ReelEntity.TRACK_RUST, mastery[it.topic] ?: 0f)) }
                systemDesign.forEach {
                    add(it.toPathNode(ReelEntity.TRACK_SYSTEM_DESIGN, mastery[it.topic] ?: 0f))
                }
            }
            _state.update { it.copy(nodes = nodes, isLoading = false) }
        }
    }

    /**
     * Runs a Room FTS query for the embedded search section. Empty queries
     * fall back to pure filter browsing (see [SearchRepository]), which the UI
     * only renders once the user has typed something.
     */
    fun onQueryChanged(query: String, filters: SearchFilters) {
        _state.update { it.copy(query = query, filters = filters) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val hits = runCatching {
                searchRepository.search(
                    query = query,
                    filters = filters.toDataFilters(),
                )
            }.getOrDefault(emptyList())
            _state.update {
                it.copy(results = hits.map { entity -> entity.toSearchResult() }, isSearching = false)
            }
        }
    }

    companion object {
        /** Level bounds for the Room filter query (1..4 per content schema). */
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 4
    }
}

/**
 * UI filter -> Room filter. Kept pure and unit-testable: a topic must survive
 * this mapping verbatim (it is an exact-match column value per TODO 21), and
 * an unset level spans the whole 1..4 range so “browse” with no level chip
 * sees every reel of the topic.
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

/**
 * Prerequisite edges come from the canonical DAG so the roadmap keeps locking
 * semantics; topics the DAG does not know about are roots (never locked).
 */
private fun prerequisitesOf(topic: String): List<String> =
    LearningPathEngine.TOPIC_GRAPH.firstOrNull { it.id == topic }?.prerequisites.orEmpty()

private fun TopicCount.toPathNode(track: String, mastery: Float): PathNode = PathNode(
    id = topic,
    title = prettyTopic(topic),
    track = track,
    level = minLevel.coerceIn(1, 4),
    mastery = mastery,
    reelCount = reelCount,
    requires = prerequisitesOf(topic),
)

/**
 * Node title from the stored topic. Since TODO 21 the stored topic is a
 * human label ("ownership", "data systems"); each word is title-cased for
 * display ("Ownership", "Data Systems"), which also keeps legacy file-derived
 * keys (`rust_cases`) readable. The stored value itself is untouched.
 */
private fun prettyTopic(topic: String): String = topic
    .split('_', '-')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    .ifBlank { topic }

private fun ReelEntity.toSearchResult(): SearchResult = SearchResult(
    id = id,
    track = track,
    level = level,
    // TODO 22: every hit names its topic so the card says where it lives in
    // the curriculum; the string is the exact roadmap id (TODO 21).
    topic = topic,
    hook = hook,
    takeaway = takeaway,
    hasCode = hasCode,
    hasQuiz = hasQuiz,
    snippet = bodyMd.take(140),
)
