package com.muslimdaily.app.data.model

import androidx.annotation.StringRes
import com.muslimdaily.app.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

enum class Prayer(@StringRes val nameRes: Int) {
    Fajr(R.string.prayer_fajr),
    Dhuhr(R.string.prayer_dhuhr),
    Asr(R.string.prayer_asr),
    Maghrib(R.string.prayer_maghrib),
    Isha(R.string.prayer_isha),
}

data class HijriDate(val day: Int, val month: Int, val year: Int)

data class PrayerDay(
    val date: LocalDate,
    val times: Map<Prayer, LocalTime>,
    val hijri: HijriDate,
)

data class NextPrayer(val prayer: Prayer, val time: LocalTime, val remaining: Duration)

fun PrayerDay.next(now: LocalTime): NextPrayer {
    val upcoming = Prayer.entries.firstOrNull { times.getValue(it).isAfter(now) }
    return if (upcoming != null) {
        val t = times.getValue(upcoming)
        NextPrayer(upcoming, t, Duration.between(now, t))
    } else {
        // After Isha: next is tomorrow's Fajr (today's Fajr time used as an approximation)
        val f = times.getValue(Prayer.Fajr)
        NextPrayer(Prayer.Fajr, f, Duration.between(now, f).plusDays(1))
    }
}
