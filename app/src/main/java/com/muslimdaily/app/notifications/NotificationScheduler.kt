package com.muslimdaily.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.muslimdaily.app.data.PrayerRepository
import com.muslimdaily.app.data.SettingsStore
import com.muslimdaily.app.data.model.NotificationSettings
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules alarms for today's and tomorrow's prayers plus the daily reminders.
 * Every fired alarm (and app open, boot, time-zone change, and a 12-hourly background job)
 * calls rescheduleAll() again, so the window keeps rolling forward.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val settings: SettingsStore,
    private val prayers: PrayerRepository,
) {
    private val alarmManager: AlarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val mutex = Mutex()

    private companion object {
        val DEFAULT_LOCATION = 23.8103 to 90.4125 // Dhaka
        const val REMINDER_BASE_CODE = 1_000_001
        const val REFRESH_WORK = "prayer_refresh"
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** refresh = true: re-download times instead of trusting the saved copy (used by the background job). */
    suspend fun rescheduleAll(refresh: Boolean = false) {
        mutex.withLock {
            val s = settings.notifications.first()
            cancelAll()
            val systemAllows = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            if (!s.master || !systemAllows) {
                WorkManager.getInstance(ctx).cancelUniqueWork(REFRESH_WORK)
                return
            }
            ensurePeriodicRefresh()
            schedulePrayers(s, refresh)
            scheduleReminders(s)
        }
    }

    private suspend fun schedulePrayers(s: NotificationSettings, refresh: Boolean) {
        if (s.prayers.isEmpty()) return
        val (lat, lng) = settings.location.first() ?: DEFAULT_LOCATION
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val todayDay = prayers.forDate(today, lat, lng, preferCache = !refresh)
        // If tomorrow cannot be loaded (offline), use today's times as a close estimate until the next refresh.
        val tomorrowDay = prayers.forDate(tomorrow, lat, lng, preferCache = !refresh) ?: todayDay?.copy(date = tomorrow)
        val now = System.currentTimeMillis()

        for (day in listOfNotNull(todayDay, tomorrowDay)) {
            for (prayer in s.prayers) {
                val time = day.times[prayer] ?: continue
                val at = LocalDateTime.of(day.date, time)
                    .minusMinutes(s.leadMinutes.toLong())
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (at > now + 1_000) {
                    prayerPending(prayer, day.date, s.leadMinutes, PendingIntent.FLAG_UPDATE_CURRENT)
                        ?.let { setAlarm(at, it) }
                }
            }
        }
    }

    private fun scheduleReminders(s: NotificationSettings) {
        for (type in ReminderType.entries) {
            val r = s.reminders[type] ?: continue
            if (!r.enabled) continue
            val now = LocalDateTime.now()
            var at = LocalDateTime.of(now.toLocalDate(), LocalTime.of(r.minuteOfDay / 60, r.minuteOfDay % 60))
            if (!at.isAfter(now)) at = at.plusDays(1)
            val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            reminderPending(type, PendingIntent.FLAG_UPDATE_CURRENT)?.let { setAlarm(millis, it) }
        }
    }

    private fun setAlarm(at: Long, pi: PendingIntent) {
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) // may be a few minutes late
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAll() {
        val today = LocalDate.now()
        for (offset in -1L..2L) {
            val date = today.plusDays(offset)
            Prayer.entries.forEach { cancel(prayerPending(it, date, 0, PendingIntent.FLAG_NO_CREATE)) }
        }
        ReminderType.entries.forEach { cancel(reminderPending(it, PendingIntent.FLAG_NO_CREATE)) }
    }

    private fun cancel(pi: PendingIntent?) {
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun prayerPending(prayer: Prayer, date: LocalDate, lead: Int, flag: Int): PendingIntent? {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(NotifConst.ACTION_ALARM)
            .putExtra(NotifConst.EXTRA_KIND, NotifConst.KIND_PRAYER)
            .putExtra(NotifConst.EXTRA_INDEX, prayer.ordinal)
            .putExtra(NotifConst.EXTRA_LEAD, lead)
        val code = (date.toEpochDay() % 100_000).toInt() * 10 + prayer.ordinal
        return PendingIntent.getBroadcast(ctx, code, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun reminderPending(type: ReminderType, flag: Int): PendingIntent? {
        val intent = Intent(ctx, AlarmReceiver::class.java)
            .setAction(NotifConst.ACTION_ALARM)
            .putExtra(NotifConst.EXTRA_KIND, NotifConst.KIND_REMINDER)
            .putExtra(NotifConst.EXTRA_INDEX, type.ordinal)
        return PendingIntent.getBroadcast(ctx, REMINDER_BASE_CODE + type.ordinal, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun ensurePeriodicRefresh() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
