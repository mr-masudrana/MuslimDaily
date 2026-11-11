package com.muslimdaily.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muslimdaily.app.data.model.NotificationSettings
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.ReminderSetting
import com.muslimdaily.app.data.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "muslim_daily_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val hijriOffsetKey = intPreferencesKey("hijri_offset")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val masterKey = booleanPreferencesKey("notif_master")
    private val leadKey = intPreferencesKey("notif_lead")
    private val latKey = doublePreferencesKey("last_lat")
    private val lngKey = doublePreferencesKey("last_lng")

    private fun prayerKey(p: Prayer) = booleanPreferencesKey("notif_prayer_${p.name}")
    private fun reminderOnKey(t: ReminderType) = booleanPreferencesKey("rem_${t.key}_on")
    private fun reminderMinuteKey(t: ReminderType) = intPreferencesKey("rem_${t.key}_min")

    // ---- First-run onboarding ----
    val onboardingDone: Flow<Boolean> = ctx.settingsStore.data.map { it[onboardingKey] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        ctx.settingsStore.edit { it[onboardingKey] = done }
    }

    // ---- Hijri adjustment ----
    /** Manual Hijri date adjustment in days (-2..+2). */
    val hijriOffset: Flow<Int> = ctx.settingsStore.data.map { it[hijriOffsetKey] ?: 0 }

    suspend fun setHijriOffset(value: Int) {
        ctx.settingsStore.edit { it[hijriOffsetKey] = value.coerceIn(-2, 2) }
    }

    // ---- Last known location (so background jobs can compute prayer times) ----
    val location: Flow<Pair<Double, Double>?> = ctx.settingsStore.data.map { p ->
        val lat = p[latKey]
        val lng = p[lngKey]
        if (lat != null && lng != null) lat to lng else null
    }

    suspend fun saveLocation(lat: Double, lng: Double) {
        ctx.settingsStore.edit {
            it[latKey] = lat
            it[lngKey] = lng
        }
    }

    // ---- Notifications ----
    val notifications: Flow<NotificationSettings> = ctx.settingsStore.data.map { p ->
        NotificationSettings(
            master = p[masterKey] ?: false,
            prayers = Prayer.entries.filter { p[prayerKey(it)] ?: true }.toSet(),
            leadMinutes = p[leadKey] ?: 0,
            reminders = ReminderType.entries.associateWith { t ->
                ReminderSetting(
                    enabled = p[reminderOnKey(t)] ?: false,
                    minuteOfDay = p[reminderMinuteKey(t)] ?: t.defaultMinute,
                )
            },
        )
    }

    suspend fun setNotificationsEnabled(on: Boolean) {
        ctx.settingsStore.edit { it[masterKey] = on }
    }

    suspend fun setPrayerEnabled(prayer: Prayer, on: Boolean) {
        ctx.settingsStore.edit { it[prayerKey(prayer)] = on }
    }

    suspend fun setLeadMinutes(minutes: Int) {
        ctx.settingsStore.edit { it[leadKey] = minutes }
    }

    suspend fun setReminderEnabled(type: ReminderType, on: Boolean) {
        ctx.settingsStore.edit { it[reminderOnKey(type)] = on }
    }

    suspend fun setReminderMinute(type: ReminderType, minuteOfDay: Int) {
        ctx.settingsStore.edit { it[reminderMinuteKey(type)] = minuteOfDay.coerceIn(0, 24 * 60 - 1) }
    }
}
