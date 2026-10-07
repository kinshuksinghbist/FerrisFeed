package com.ferrisfeed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Aggregated user stats for the top stat bar (P6 32d).
 */
data class UserStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    progressStore: ProgressStore,
) : ViewModel() {

    val stats: StateFlow<UserStats> = combine(
        progressStore.xp,
        progressStore.streakDays,
    ) { xp, streak ->
        UserStats(xp = xp, streakDays = streak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserStats(),
    )
}
