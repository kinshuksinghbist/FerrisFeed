package com.ferrisfeed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.coreui.DesignPhilosophy
import com.ferrisfeed.data.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregated user stats for the top stat bar and app shell design philosophy.
 */
data class UserStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val progressStore: ProgressStore,
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

    val designPhilosophy: StateFlow<DesignPhilosophy> = progressStore.designPhilosophy
        .map { if (it == DesignPhilosophy.NeoBrutalism.name) DesignPhilosophy.NeoBrutalism else DesignPhilosophy.StudioGlass }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DesignPhilosophy.StudioGlass,
        )

    fun setDesignPhilosophy(philosophy: DesignPhilosophy) {
        viewModelScope.launch {
            progressStore.setDesignPhilosophy(philosophy.name)
        }
    }
}
