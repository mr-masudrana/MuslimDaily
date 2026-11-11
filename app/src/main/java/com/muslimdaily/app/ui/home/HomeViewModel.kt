package com.muslimdaily.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.PrayerRepository
import com.muslimdaily.app.data.SettingsStore
import com.muslimdaily.app.data.location.LocationProvider
import com.muslimdaily.app.data.model.PrayerDay
import com.muslimdaily.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val day: PrayerDay? = null,
    val usingDefaultLocation: Boolean = false,
    val now: LocalTime = LocalTime.now(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val location: LocationProvider,
    private val settings: SettingsStore,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val hijriOffset: StateFlow<Int> =
        settings.hijriOffset.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private companion object {
        val DHAKA = 23.8103 to 90.4125
    }

    init {
        load()
        startClock()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val loc = location.current()
            if (loc != null) settings.saveLocation(loc.first, loc.second)
            val (lat, lng) = loc ?: DHAKA
            val day = repo.today(lat, lng)
            _state.update { it.copy(loading = false, day = day, usingDefaultLocation = loc == null) }
            scheduler.rescheduleAll() // keeps prayer notifications up to date
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(now = LocalTime.now()) }
                val day = _state.value.day
                if (day != null && day.date != LocalDate.now() && !_state.value.loading) load() // new day
                delay(10_000)
            }
        }
    }
}
