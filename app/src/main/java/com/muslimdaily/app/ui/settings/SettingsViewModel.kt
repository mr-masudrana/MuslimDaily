package com.muslimdaily.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val hijriOffset: StateFlow<Int> =
        store.hijriOffset.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun changeHijriOffset(delta: Int) {
        viewModelScope.launch { store.setHijriOffset((hijriOffset.value + delta).coerceIn(-2, 2)) }
    }
}
