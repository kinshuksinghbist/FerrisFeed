package com.ferrisfeed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferrisfeed.data.LearnerProfile
import com.ferrisfeed.data.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-level state: whether first-run onboarding is done. Null until
 * DataStore answers, so the first frame never flashes onboarding at a
 * returning learner.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val progressStore: ProgressStore,
) : ViewModel() {

    val learner: StateFlow<LearnerProfile?> = progressStore.learnerProfile
        .map<LearnerProfile, LearnerProfile?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding(trackFocus: String, experience: Int) {
        viewModelScope.launch { progressStore.saveLearnerProfile(trackFocus, experience) }
    }
}
