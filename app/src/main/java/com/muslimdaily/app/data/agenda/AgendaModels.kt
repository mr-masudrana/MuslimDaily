package com.muslimdaily.app.data.agenda

import com.muslimdaily.app.data.dua.LocalizedText
import com.muslimdaily.app.data.model.HijriDate
import java.time.LocalDate
import java.time.LocalTime

// Parsed from assets/agenda/practices.json (nullable: Gson ignores Kotlin defaults)
data class PracticeFile(val practices: List<Practice>? = null)

data class Practice(
    val id: String? = null,
    val title: LocalizedText? = null,
    val time: String? = null,          // "HH:mm", optional
    val weekdays: List<Int>? = null,   // ISO: 1=Monday ... 7=Sunday
    val hijriDays: List<Int>? = null,  // e.g. [13, 14, 15]
    val hijriMonths: List<Int>? = null,// 1..12
    val surah: Int? = null,            // opens this surah when tapped
)

/** A practice applies when every condition that is present matches; no conditions = every day. */
fun Practice.matches(date: LocalDate, hijri: HijriDate): Boolean {
    weekdays?.let { if (date.dayOfWeek.value !in it) return false }
    hijriDays?.let { if (hijri.day !in it) return false }
    hijriMonths?.let { if (hijri.month !in it) return false }
    return true
}

fun Practice.timeOrNull(): LocalTime? = try {
    time?.let { LocalTime.parse(it) }
} catch (e: Exception) {
    null
}
