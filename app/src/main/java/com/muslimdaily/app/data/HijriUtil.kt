package com.muslimdaily.app.data

import com.muslimdaily.app.data.model.HijriDate
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class DayCell(val hijriDay: Int, val date: LocalDate)

/**
 * Offline Hijri calendar (Umm al-Qura, built into Android) with a manual day adjustment,
 * because local moon sighting can differ by a day or two.
 * offset: -2..+2 days; hijri(date) = UmmAlQura(date + offset).
 */
object HijriUtil {
    const val MIN_YEAR = 1350
    const val MAX_YEAR = 1550

    fun of(date: LocalDate, offset: Int): HijriDate {
        val h = HijrahDate.from(date.plusDays(offset.toLong()))
        return HijriDate(
            day = h.get(ChronoField.DAY_OF_MONTH),
            month = h.get(ChronoField.MONTH_OF_YEAR),
            year = h.get(ChronoField.YEAR),
        )
    }

    fun monthCells(year: Int, month: Int, offset: Int): List<DayCell> {
        val length = HijrahDate.of(year, month, 1).lengthOfMonth()
        return (1..length).map { d ->
            DayCell(d, LocalDate.from(HijrahDate.of(year, month, d)).minusDays(offset.toLong()))
        }
    }
}
