package com.muslimdaily.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.muslimdaily.app.MainActivity
import com.muslimdaily.app.R
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.ReminderType
import com.muslimdaily.app.ui.common.localizeNumber
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

object NotifConst {
    const val ACTION_ALARM = "com.muslimdaily.app.ALARM"
    const val EXTRA_KIND = "kind"
    const val EXTRA_INDEX = "index"
    const val EXTRA_LEAD = "lead"
    const val KIND_PRAYER = "prayer"
    const val KIND_REMINDER = "reminder"
    const val CHANNEL_PRAYER = "prayer_times"
    const val CHANNEL_REMINDER = "reminders"
}

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun createChannels() {
        val c = localizedContext()
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(NotifConst.CHANNEL_PRAYER, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(c.getString(R.string.channel_prayer))
                .build()
        )
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(NotifConst.CHANNEL_REMINDER, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(c.getString(R.string.channel_reminders))
                .build()
        )
    }

    /** Called by the alarm receiver. Shows the matching notification. */
    fun handleAlarm(intent: Intent) {
        val c = localizedContext()
        val locale = appLocale()
        val index = intent.getIntExtra(NotifConst.EXTRA_INDEX, -1)
        when (intent.getStringExtra(NotifConst.EXTRA_KIND)) {
            NotifConst.KIND_PRAYER -> {
                val prayer = Prayer.entries.getOrNull(index) ?: return
                val lead = intent.getIntExtra(NotifConst.EXTRA_LEAD, 0)
                val name = c.getString(prayer.nameRes)
                val text = if (lead > 0) {
                    c.getString(R.string.notif_prayer_soon, name, localizeNumber(lead, locale))
                } else {
                    c.getString(R.string.notif_prayer_now, name)
                }
                show(100 + prayer.ordinal, NotifConst.CHANNEL_PRAYER, c.getString(R.string.app_name), text)
            }
            NotifConst.KIND_REMINDER -> {
                val type = ReminderType.entries.getOrNull(index) ?: return
                val res = when (type) {
                    ReminderType.Quran -> R.string.notif_quran
                    ReminderType.MorningDhikr -> R.string.notif_morning
                    ReminderType.EveningDhikr -> R.string.notif_evening
                }
                show(200 + type.ordinal, NotifConst.CHANNEL_REMINDER, c.getString(R.string.app_name), c.getString(res))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun show(id: Int, channel: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notification)
    }

    // The app language chosen in Settings applies to screens; for notifications we apply it ourselves.
    private fun appLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return (if (locales.isEmpty()) null else locales.get(0)) ?: Locale.getDefault()
    }

    private fun localizedContext(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        val first = if (locales.isEmpty()) null else locales.get(0)
        if (first == null) return ctx
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocale(first)
        return ctx.createConfigurationContext(cfg)
    }
}
