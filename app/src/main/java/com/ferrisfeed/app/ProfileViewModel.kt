package com.ferrisfeed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.Curriculum
import com.ferrisfeed.data.LearnerProfile
import com.ferrisfeed.data.ProgressStore
import com.ferrisfeed.data.QuizTally
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.feed.FeedRepository
import com.ferrisfeed.path.FerrisStage
import com.ferrisfeed.path.ProgressSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TopicMasteryItem(
    val key: String,
    val title: String,
    val track: String,
    val score: Float,
)

data class SavedLesson(
    val id: String,
    val track: String,
    val topicTitle: String,
    val hook: String,
)

data class ProfileUiState(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val todayXp: Int = 0,
    val weekActive: List<Boolean> = List(7) { false },
    val todayIndex: Int = 0,
    val quiz: QuizTally = QuizTally(),
    val lessonsDone: Int = 0,
    val strengths: List<TopicMasteryItem> = emptyList(),
    val practice: List<TopicMasteryItem> = emptyList(),
    val saved: List<SavedLesson> = emptyList(),
    val focus: String = LearnerProfile.FOCUS_BOTH,
    val isLoading: Boolean = true,
) {
    val stage: FerrisStage get() = ProgressSystem.ferrisStage(xp)
    val xpToNext: Int? get() = ProgressSystem.xpToNextStage(xp)
    val stageProgress: Float
        get() {
            val next = FerrisStage.entries.firstOrNull { it.minXp > xp } ?: return 1f
            val floor = stage.minXp
            return (xp - floor).toFloat() / (next.minXp - floor)
        }
    val accuracy: Int? get() = if (quiz.total == 0) null else (quiz.correct * 100) / quiz.total
}

private data class Activity(val xp: Int, val streak: Int, val today: Int, val week: List<Boolean>, val todayIndex: Int)
private data class Library(val saved: List<SavedLesson>, val lessonsDone: Int, val focus: String)

/**
 * The You tab: rank, streak week, honest stats (accuracy from real quiz
 * answers), strongest and weakest topics, saved lessons, learning focus.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val progressStore: ProgressStore,
    private val repository: FeedRepository,
    dao: ReelDao,
) : ViewModel() {

    private val activity = combine(
        progressStore.xp,
        progressStore.streakDays,
        progressStore.todayXp,
        progressStore.heatmapDays,
    ) { xp, streak, today, days ->
        val now = LocalDate.now()
        val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = (0..6).map { offset ->
            monday.plusDays(offset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) in days
        }
        Activity(xp, streak, today, week, now.dayOfWeek.value - 1)
    }

    private val library = combine(
        repository.observeSavedIds(),
        repository.observeAllReels(),
        dao.observeSeenCount(),
        progressStore.learnerProfile,
    ) { savedIds, reels, seen, learner ->
        val saved = reels.filter { it.id in savedIds }.map {
            SavedLesson(it.id, it.track.id, Curriculum.titleOf(it.topic), it.hook)
        }
        Library(saved, seen, learner.trackFocus)
    }

    val state: StateFlow<ProfileUiState> = combine(
        activity,
        progressStore.quizTally,
        progressStore.allStoredMastery,
        library,
    ) { act, tally, mastery, lib ->
        val topics = mastery.mapNotNull { (sanitized, score) ->
            val key = Curriculum.topicForMasteryKey(sanitized) ?: return@mapNotNull null
            val unit = Curriculum.unitOf(key) ?: return@mapNotNull null
            TopicMasteryItem(key, Curriculum.titleOf(key), unit.track, score)
        }
        ProfileUiState(
            xp = act.xp,
            streakDays = act.streak,
            todayXp = act.today,
            weekActive = act.week,
            todayIndex = act.todayIndex,
            quiz = tally,
            lessonsDone = lib.lessonsDone,
            strengths = topics.filter { it.score >= 0.6f }.sortedByDescending { it.score }.take(3),
            practice = topics.filter { it.score > 0f && it.score < 0.4f }.sortedBy { it.score }.take(3),
            saved = lib.saved,
            focus = lib.focus,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun removeSaved(reelId: String) {
        viewModelScope.launch { repository.setSaved(reelId, false) }
    }

    fun setFocus(focus: String) {
        viewModelScope.launch {
            val current = progressStore.learnerProfile.first()
            progressStore.saveLearnerProfile(focus, current.experience)
        }
    }
}
