package com.ferrisfeed.feed

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

private val KEY_LAST_INDEX = intPreferencesKey("feed_last_index")
private val KEY_LAST_ID = longPreferencesKey("feed_last_id_hash")

data class FeedUiState(
    val reels: List<Reel> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val savedIds: Set<String> = emptySet(),
    val likedIds: Set<String> = emptySet(),
    /** Peek reveals the quiz answer without grading (long-press). */
    val peekReelId: String? = null,
    val deepDiveReelId: String? = null,
)

data class ImpressionEvent(
    val reelId: String,
    val dwellMs: Long,
    val skipped: Boolean,
)

/** Split of [FeedUiState] carried by the first inner combine (4-arity). */
private data class FeedPartialMain(
    val reels: List<Reel>,
    val currentIndex: Int,
    val isLoading: Boolean,
    val savedIds: Set<String>,
)

/** Split of [FeedUiState] carried by the second inner combine (3-arity). */
private data class FeedPartialOverlays(
    val likedIds: Set<String>,
    val peekReelId: String?,
    val deepDiveReelId: String?,
)

/**
 * Feed view model.
 *
 * Shuffle: 70% due SRS + 20% new in path order + 10% random review (see docs/feed-ux.md).
 * Tracking: impression (first paint), dwell (ms on page), skip (dwell < 1500ms and no interaction).
 * Position: persists current index + reel id hash to DataStore, restores on launch.
 */
class FeedViewModel(
    private val repository: FeedRepository,
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val savedIds = MutableStateFlow<Set<String>>(emptySet())
    private val likedIds = MutableStateFlow<Set<String>>(emptySet())
    private val peekId = MutableStateFlow<String?>(null)
    private val deepDiveId = MutableStateFlow<String?>(null)
    private val index = MutableStateFlow(0)
    private val loading = MutableStateFlow(true)

    private val queue: StateFlow<List<Reel>> = repository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // NB: nested 4+3+2 combines instead of one 7-flow combine. The 7-arity
    // heterogeneous overload does not resolve on our coroutines version, so
    // keep every combine at an arity that has existed forever.
    val uiState: StateFlow<FeedUiState> = combine(
        combine(queue, index, loading, savedIds) { q, i, load, s ->
            FeedPartialMain(
                reels = q,
                currentIndex = i.coerceIn(0, max(0, q.size - 1)),
                isLoading = load,
                savedIds = s,
            )
        },
        combine(likedIds, peekId, deepDiveId) { l, p, d ->
            FeedPartialOverlays(
                likedIds = l,
                peekReelId = p,
                deepDiveReelId = d,
            )
        },
    ) { main, overlays ->
        FeedUiState(
            reels = main.reels,
            currentIndex = main.currentIndex,
            isLoading = main.isLoading,
            savedIds = main.savedIds,
            likedIds = overlays.likedIds,
            peekReelId = overlays.peekReelId,
            deepDiveReelId = overlays.deepDiveReelId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedUiState())

    private var dwellJob: Job? = null
    private var pageStartMs: Long = clock()
    private var interactedWithCurrent = false

    init {
        viewModelScope.launch {
            loading.value = true
            val restored = dataStore.data.first()
            val restoredIndex = restored[KEY_LAST_INDEX] ?: 0
            // Initial queue comes from repository (preloaded by :data prepackaged DB).
            // Re-shuffle once so due cards surface first.
            reshuffle(nowMs = clock())
            index.value = restoredIndex
            loading.value = false
            pageStartMs = clock()
        }
    }

    /** Build the 70/20/10 queue from [all] reels. Pure function for testability. */
    fun buildQueue(all: List<Reel>, nowMs: Long, rng: Random = random): List<Reel> {
        if (all.isEmpty()) return emptyList()
        val due = all.filter { !it.isNew && it.dueAtEpochMs <= nowMs }
            .sortedBy { it.dueAtEpochMs }
        val fresh = all.filter { it.isNew }.sortedBy { it.pathOrder }
        val reviewPool = all.filter { !it.isNew && it.dueAtEpochMs > nowMs }

        // Target sizes; always keep at least 1 fresh when available for forward progress.
        val total = all.size
        val dueCount = (total * 0.7).toInt().coerceAtLeast(1).coerceAtMost(due.size)
        val freshCount = (total * 0.2).toInt().coerceAtMost(fresh.size)
        val reviewCount = (total * 0.1).toInt().coerceAtLeast(
            if (reviewPool.isNotEmpty()) 1 else 0,
        ).coerceAtMost(reviewPool.size)

        val pickedDue = due.take(dueCount)
        val pickedFresh = fresh.take(if (fresh.isNotEmpty() && freshCount == 0) 1 else freshCount)
        val pickedReview = reviewPool.shuffled(rng).take(reviewCount)

        val merged = (pickedDue + pickedFresh + pickedReview).toMutableList()
        // Fill remainder with whatever is left, preserving due-first priority.
        if (merged.size < total) {
            val remaining = (all - merged.toSet()).shuffled(rng)
            merged += remaining.take(total - merged.size)
        }
        // Interleave so two due cards rarely sit back-to-back: due, fresh/review, due...
        return interleave(pickedDue, (pickedFresh + pickedReview).shuffled(rng), merged - (pickedDue + pickedFresh + pickedReview).toSet())
    }

    private fun interleave(due: List<Reel>, other: List<Reel>, tail: List<Reel>): List<Reel> {
        val out = ArrayList<Reel>(due.size + other.size + tail.size)
        val di = due.iterator()
        val oi = other.iterator()
        while (di.hasNext() || oi.hasNext()) {
            if (di.hasNext()) out += di.next()
            if (oi.hasNext()) out += oi.next()
        }
        out += tail
        return out
    }

    fun reshuffle(nowMs: Long = clock(), all: List<Reel>? = null) {
        viewModelScope.launch {
            val source = all ?: queue.value
            if (source.isEmpty()) return@launch
            repository.refreshQueue(buildQueue(source, nowMs))
        }
    }

    fun onPageChanged(newIndex: Int) {
        val state = uiState.value
        val prevId = state.reels.getOrNull(state.currentIndex)?.id
        if (prevId != null) {
            val dwell = clock() - pageStartMs
            val skipped = dwell < 1500L && !interactedWithCurrent
            viewModelScope.launch { repository.trackImpression(prevId, dwell, skipped) }
        }
        index.value = newIndex
        pageStartMs = clock()
        interactedWithCurrent = false
        peekId.value = null
        persistPosition(newIndex, state.reels.getOrNull(newIndex)?.id)
        // Prefetch trigger: repository / data layer warms next 5 (Room is in-memory here;
        // image/code LRU lives in the UI layer via Coil + highlight cache).
        prefetchAhead(newIndex)
    }

    private fun prefetchAhead(fromIndex: Int) {
        viewModelScope.launch {
            val reels = uiState.value.reels
            val end = (fromIndex + 5).coerceAtMost(reels.size - 1)
            for (i in (fromIndex + 1)..end) {
                repository.getReel(reels[i].id)
            }
        }
    }

    fun onLike(reelId: String, liked: Boolean) {
        interactedWithCurrent = true
        likedIds.value = if (liked) likedIds.value + reelId else likedIds.value - reelId
        viewModelScope.launch { repository.setLiked(reelId, liked) }
    }

    fun onSave(reelId: String, saved: Boolean) {
        interactedWithCurrent = true
        savedIds.value = if (saved) savedIds.value + reelId else savedIds.value - reelId
        viewModelScope.launch { repository.setSaved(reelId, saved) }
    }

    fun onToggleSave(reelId: String) {
        onSave(reelId, !savedIds.value.contains(reelId))
    }

    fun onGrade(reelId: String, correct: Boolean, label: String) {
        interactedWithCurrent = true
        viewModelScope.launch { repository.recordGrade(reelId, correct, label) }
    }

    fun onInteract() {
        interactedWithCurrent = true
    }

    fun onPeek(reelId: String?) {
        peekId.value = reelId
    }

    fun onDeepDive(reelId: String?) {
        deepDiveId.value = reelId
    }

    fun restoreIndex(): StateFlow<Int> = index

    private fun persistPosition(index: Int, reelId: String?) {
        dwellJob?.cancel()
        // Debounce writes so fast flings do not hammer DataStore.
        dwellJob = viewModelScope.launch {
            delay(400)
            dataStore.edit { prefs ->
                prefs[KEY_LAST_INDEX] = index
                prefs[KEY_LAST_ID] = (reelId?.hashCode()?.toLong() ?: 0L)
            }
        }
    }
}
