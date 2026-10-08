package com.ferrisfeed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.coreui.DesignPhilosophy
import com.ferrisfeed.data.ProgressStore
import com.ferrisfeed.feed.FeedRepository
import com.ferrisfeed.feed.Reel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class TopicMasteryItem(
    val id: String,
    val title: String,
    val score: Float,
    val isGoodAt: Boolean,
)

data class ProfileUiState(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val activeDaysThisWeek: List<Boolean> = List(7) { false },
    val designPhilosophy: DesignPhilosophy = DesignPhilosophy.StudioGlass,
    val topicsGoodAt: List<TopicMasteryItem> = emptyList(),
    val topicsToPractice: List<TopicMasteryItem> = emptyList(),
    val savedReels: List<Reel> = emptyList(),
    val completedReelsCount: Int = 0,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val progressStore: ProgressStore,
    private val repository: FeedRepository,
) : ViewModel() {

    private val nowClock: () -> Long = System::currentTimeMillis

    val state: StateFlow<ProfileUiState> = combine(
        combine(progressStore.xp, progressStore.streakDays, progressStore.heatmapDays) { xp, streak, heatmap ->
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekDays = (0..6).map { offset ->
                val dayDate = monday.plusDays(offset.toLong())
                val dayStr = dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                heatmap.contains(dayStr)
            }
            Triple(xp, streak, weekDays)
        },
        progressStore.designPhilosophy,
        progressStore.allStoredMastery,
        combine(repository.observeSavedIds(), repository.observeAllReels()) { savedIds, allReels ->
            allReels.filter { savedIds.contains(it.id) } to allReels.count { !it.isNew && it.stability >= 1.5f }
        },
    ) { (xp, streak, weekDays), philosophyName, masteryMap, (savedReels, completedCount) ->
        val philosophy = if (philosophyName == DesignPhilosophy.NeoBrutalism.name) {
            DesignPhilosophy.NeoBrutalism
        } else {
            DesignPhilosophy.StudioGlass
        }

        val goodList = mutableListOf<TopicMasteryItem>()
        val practiceList = mutableListOf<TopicMasteryItem>()

        masteryMap.forEach { (topic, score) ->
            val pretty = topic.split('_', '-')
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { topic }

            if (score >= 0.70f) {
                goodList.add(TopicMasteryItem(topic, pretty, score, isGoodAt = true))
            } else if (score > 0.01f || score < 0.50f) {
                practiceList.add(TopicMasteryItem(topic, pretty, score, isGoodAt = false))
            }
        }

        ProfileUiState(
            xp = xp,
            streakDays = streak,
            activeDaysThisWeek = weekDays,
            designPhilosophy = philosophy,
            topicsGoodAt = goodList.sortedByDescending { it.score },
            topicsToPractice = practiceList.sortedBy { it.score },
            savedReels = savedReels,
            completedReelsCount = completedCount,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLoading = true),
    )

    fun setDesignPhilosophy(philosophy: DesignPhilosophy) {
        viewModelScope.launch {
            progressStore.setDesignPhilosophy(philosophy.name)
        }
    }

    fun removeSaved(reelId: String) {
        viewModelScope.launch {
            repository.setSaved(reelId, false)
        }
    }
}
