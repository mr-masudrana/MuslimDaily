package com.muslimdaily.app

import android.app.Application
import com.muslimdaily.app.notifications.Notifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MuslimDailyApp : Application() {
    @Inject lateinit var notifier: Notifier

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
    }
}
