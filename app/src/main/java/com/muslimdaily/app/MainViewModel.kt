package com.muslimdaily.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val settings: SettingsStore) : ViewModel() {

    /** null while loading (the splash screen stays visible), then true/false. */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun finishOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone(true) }
    }
}
