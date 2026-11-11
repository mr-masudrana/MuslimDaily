package com.muslimdaily.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun scheduler(): NotificationScheduler
    fun notifier(): Notifier
}

private fun entryPoint(context: Context): NotificationEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, NotificationEntryPoint::class.java)

/** Fires when a prayer time or a daily reminder is due: shows the notification, then schedules the next ones. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ep = entryPoint(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ep.notifier().handleAlarm(intent)
                withTimeoutOrNull(8_000) { ep.scheduler().rescheduleAll() }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Restores alarms after reboot, app update, or a time / time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ep = entryPoint(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ep.notifier().createChannels()
                withTimeoutOrNull(8_000) { ep.scheduler().rescheduleAll() }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Runs about every 12 hours (with internet) to download fresh prayer times and re-schedule alarms. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        entryPoint(applicationContext).scheduler().rescheduleAll(refresh = true)
        return Result.success()
    }
}
