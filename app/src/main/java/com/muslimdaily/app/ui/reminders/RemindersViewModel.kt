package com.muslimdaily.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.SettingsStore
import com.muslimdaily.app.data.model.NotificationSettings
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.ReminderType
import com.muslimdaily.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val store: SettingsStore,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> =
        store.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    fun canScheduleExact() = scheduler.canScheduleExact()

    fun reschedule() { viewModelScope.launch { scheduler.rescheduleAll() } }

    fun setMaster(on: Boolean) = save { store.setNotificationsEnabled(on) }
    fun setPrayer(prayer: Prayer, on: Boolean) = save { store.setPrayerEnabled(prayer, on) }
    fun setLeadMinutes(minutes: Int) = save { store.setLeadMinutes(minutes) }
    fun setReminderEnabled(type: ReminderType, on: Boolean) = save { store.setReminderEnabled(type, on) }
    fun setReminderMinute(type: ReminderType, minute: Int) = save { store.setReminderMinute(type, minute) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            scheduler.rescheduleAll()
        }
    }
}
