package com.muslimdaily.app.data.model

enum class ReminderType(val key: String, val defaultMinute: Int) {
    Quran("quran", 20 * 60),
    MorningDhikr("morning", 5 * 60),
    EveningDhikr("evening", 17 * 60 + 30),
}

data class ReminderSetting(val enabled: Boolean = false, val minuteOfDay: Int = 0)

data class NotificationSettings(
    val master: Boolean = false,
    val prayers: Set<Prayer> = Prayer.entries.toSet(),
    val leadMinutes: Int = 0, // 0 = at prayer time
    val reminders: Map<ReminderType, ReminderSetting> =
        ReminderType.entries.associateWith { ReminderSetting(false, it.defaultMinute) },
)
