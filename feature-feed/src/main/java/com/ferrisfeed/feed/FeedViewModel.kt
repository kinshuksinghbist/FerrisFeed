package com.ferrisfeed.feed

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.CurriculumSeeder
import com.ferrisfeed.data.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max

private val KEY_LAST_INDEX = intPreferencesKey("feed_last_index")
private val KEY_LAST_ID = longPreferencesKey("feed_last_id_hash")
private val KEY_LAST_REEL_ID = stringPreferencesKey("feed_last_reel_id")
private val KEY_SWIPE_HINT_SEEN = booleanPreferencesKey("swipe_hint_seen")

/** What the feed is currently playing. */
sealed interface FeedScope {
    /** The personal mixed queue (Learn tab). */
    data object Main : FeedScope

    /** One curriculum topic, in lesson order (from Path). */
    data class Topic(val key: String) : FeedScope

    /** A single lesson opened from search or saved; plays its topic from that lesson. */
    data class Lesson(val reelId: String) : FeedScope
}

/** A graded quiz answer kept for this session so swiping back shows it. */
data class AnswerRecord(
    val correct: Boolean,
    val choice: String,
)

data class FeedUiState(
    val reels: List<Reel> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val savedIds: Set<String> = emptySet(),
    val answers: Map<String, AnswerRecord> = emptyMap(),
    val scope: FeedScope = FeedScope.Main,
    /** Bumped whenever the queue is rebuilt so the pager re-seats on [currentIndex]. */
    val queueVersion: Int = 0,
)

data class FeedStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val todayXp: Int = 0,
)

/** Inner 4-arity combine holder (learnings §7: never combine more than 4 flows). */
private data class FeedCore(
    val reels: List<Reel>,
    val index: Int,
    val loading: Boolean,
    val saved: Set<String>,
)

/**
 * Feed view model (v3).
 *
 * One instance can be shared by every feed route (Navigation3 does not give
 * each entry its own ViewModelStore here, learnings §22), so the active
 * [FeedScope] is explicit: Learn, a topic, or a single lesson. Leaving a topic
 * and returning to Learn restores the Learn position from the persisted reel
 * id instead of resetting to the top.
 *
 * Quiz answers are the only grading signal (SRS, mastery, XP, streak).
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val dataStore: DataStore<Preferences>,
    private val progressStore: ProgressStore,
) : ViewModel() {

    /** Overridable clock for tests. */
    var clock: () -> Long = System::currentTimeMillis

    private val savedIds = MutableStateFlow<Set<String>>(emptySet())
    private val answers = MutableStateFlow<Map<String, AnswerRecord>>(emptyMap())
    private val index = MutableStateFlow(0)
    private val loading = MutableStateFlow(true)
    private val scope = MutableStateFlow<FeedScope>(FeedScope.Main)
    private val queueVersion = MutableStateFlow(0)

    private val swipeHintSeenInternal = MutableStateFlow(true)
    val swipeHintSeen: StateFlow<Boolean> = swipeHintSeenInternal

    private var allReels: List<Reel> = emptyList()
    private var mainReelId: String? = null
    private var appliedScope: FeedScope? = null

    private val queue: StateFlow<List<Reel>> = repository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<FeedUiState> = combine(
        combine(queue, index, loading, savedIds) { q, i, l, s ->
            FeedCore(q, i.coerceIn(0, max(0, q.size - 1)), l, s)
        },
        answers,
        scope,
        queueVersion,
    ) { core, a, sc, v ->
        FeedUiState(
            reels = core.reels,
            currentIndex = core.index,
            isLoading = core.loading,
            savedIds = core.saved,
            answers = a,
            scope = sc,
            queueVersion = v,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedUiState())

    val stats: StateFlow<FeedStats> = combine(
        progressStore.xp,
        progressStore.streakDays,
        progressStore.todayXp,
    ) { xp, streak, today -> FeedStats(xp, streak, today) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeedStats())

    private var dwellJob: Job? = null
    private var scopeJob: Job? = null
    private var pageStartMs: Long = clock()
    private var interactedWithCurrent = false

    init {
        viewModelScope.launch {
            savedIds.value = repository.savedIds()
            val prefs = dataStore.data.first()
            swipeHintSeenInternal.value = prefs[KEY_SWIPE_HINT_SEEN] ?: false
            mainReelId = prefs[KEY_LAST_REEL_ID]
        }
    }

    /**
     * Points the feed at [target]. Idempotent: routes call it on every entry.
     * Returns the rebuild job so callers can await it.
     */
    fun setScope(target: FeedScope): Job? {
        if (appliedScope == target && queue.value.isNotEmpty()) return null
        appliedScope = target
        scopeJob?.cancel()
        scopeJob = viewModelScope.launch {
            loading.value = true
            scope.value = target
            val source = loadAll()
            if (source.isEmpty()) {
                repository.refreshQueue(emptyList())
                loading.value = false
                return@launch
            }
            val (built, start) = when (target) {
                FeedScope.Main -> {
                    val profile = progressStore.learnerProfile.first()
                    val q = QueueBuilder.build(source, clock(), profile)
                    val restoreId = mainReelId ?: dataStore.data.first()[KEY_LAST_REEL_ID]
                    q to q.indexOfFirst { it.id == restoreId }.coerceAtLeast(0)
                }
                is FeedScope.Topic -> {
                    val q = QueueBuilder.topicOrder(source.filter { it.topic == target.key })
                    q to q.indexOfFirst { it.isNew }.coerceAtLeast(0)
                }
                is FeedScope.Lesson -> {
                    val topic = source.firstOrNull { it.id == target.reelId }?.topic
                    val q = if (topic == null) {
                        source.filter { it.id == target.reelId }
                    } else {
                        QueueBuilder.topicOrder(source.filter { it.topic == topic })
                    }
                    q to q.indexOfFirst { it.id == target.reelId }.coerceAtLeast(0)
                }
            }
            repository.refreshQueue(built)
            index.value = start
            queueVersion.update { it + 1 }
            loading.value = false
            pageStartMs = clock()
            interactedWithCurrent = false
        }
        return scopeJob
    }

    private suspend fun loadAll(): List<Reel> {
        if (allReels.isNotEmpty()) {
            // Refresh SRS fields (due dates, isNew) without waiting on a seed.
            allReels = repository.allReels().ifEmpty { allReels }
            return allReels
        }
        // First run: Room is still seeding from bundled JSON, so wait for the
        // first non-empty snapshot. 30s cap, then the empty state shows retry.
        allReels = withTimeoutOrNull(30_000) {
            repository.observeAllReels().first { it.isNotEmpty() }
        }.orEmpty()
        // The first query opened the database, which starts the seed/refresh
        // pass; wait for it so the queue is built from complete, current
        // content (packs land one by one, and old installs get new labels).
        if (withTimeoutOrNull(15_000) { CurriculumSeeder.ready.first { it } } != null) {
            allReels = repository.allReels().ifEmpty { allReels }
        }
        return allReels
    }

    /** Re-attempt a load after an empty feed. */
    fun retryLoad() {
        val target = scope.value
        appliedScope = null
        allReels = emptyList()
        setScope(target)
    }

    /** Called with the reel index under the current page. */
    fun onPageChanged(newIndex: Int) {
        val state = uiState.value
        if (newIndex == state.currentIndex && state.reels.isNotEmpty()) return
        val prevId = state.reels.getOrNull(state.currentIndex)?.id
        if (prevId != null) {
            val dwell = clock() - pageStartMs
            val skipped = dwell < 1500L && !interactedWithCurrent
            viewModelScope.launch { repository.trackImpression(prevId, dwell, skipped) }
        }
        index.value = newIndex
        if (newIndex > 0) dismissSwipeHint()
        pageStartMs = clock()
        interactedWithCurrent = false
        if (state.scope == FeedScope.Main) {
            val id = state.reels.getOrNull(newIndex)?.id
            mainReelId = id
            persistPosition(newIndex, id)
        }
    }

    fun onToggleSave(reelId: String): Boolean {
        interactedWithCurrent = true
        val nowSaved = !savedIds.value.contains(reelId)
        savedIds.value = if (nowSaved) savedIds.value + reelId else savedIds.value - reelId
        viewModelScope.launch { repository.setSaved(reelId, nowSaved) }
        return nowSaved
    }

    /**
     * Quiz answers are the SOLE grading signal: SRS step, topic mastery, XP
     * (15 right / 5 for trying) and today's activity for the streak.
     */
    fun onGrade(reelId: String, correct: Boolean, choice: String) {
        if (answers.value.containsKey(reelId)) return
        interactedWithCurrent = true
        answers.update { it + (reelId to AnswerRecord(correct, choice)) }
        viewModelScope.launch {
            repository.recordGrade(reelId, correct, choice)
            val topic = repository.getReel(reelId)?.topic?.ifBlank { null }
            if (topic != null) progressStore.recordQuizResult(topic, correct)
            progressStore.addXp(if (correct) XP_CORRECT else XP_WRONG)
            progressStore.recordDailyActivity()
        }
    }

    fun onInteract() {
        interactedWithCurrent = true
    }

    fun dismissSwipeHint() {
        if (!swipeHintSeenInternal.value) {
            swipeHintSeenInternal.value = true
            viewModelScope.launch { dataStore.edit { it[KEY_SWIPE_HINT_SEEN] = true } }
        }
    }

    private fun persistPosition(index: Int, reelId: String?) {
        dwellJob?.cancel()
        // Debounce writes so fast flings do not hammer DataStore.
        dwellJob = viewModelScope.launch {
            delay(400)
            dataStore.edit { prefs ->
                prefs[KEY_LAST_INDEX] = index
                if (reelId != null) {
                    prefs[KEY_LAST_REEL_ID] = reelId
                    prefs[KEY_LAST_ID] = reelId.hashCode().toLong()
                }
            }
            progressStore.saveScrollPosition(reelId, index)
        }
    }

    companion object {
        const val XP_CORRECT = 15
        const val XP_WRONG = 5
        const val DAILY_GOAL_XP = 50
    }
}
