package com.muslimdaily.app.data

import com.muslimdaily.app.data.local.PrayerDao
import com.muslimdaily.app.data.local.PrayerDayEntity
import com.muslimdaily.app.data.model.HijriDate
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.PrayerDay
import com.muslimdaily.app.data.remote.AladhanApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class PrayerRepository @Inject constructor(
    private val api: AladhanApi,
    private val dao: PrayerDao,
) {
    suspend fun today(lat: Double, lng: Double): PrayerDay? = forDate(LocalDate.now(), lat, lng)

    /**
     * Prayer times for any date. Network first, saved copy (offline) as fallback.
     * preferCache = true: use the saved copy if there is one, and only go online when missing.
     */
    suspend fun forDate(date: LocalDate, lat: Double, lng: Double, preferCache: Boolean = false): PrayerDay? {
        val key = date.toString()
        if (preferCache) dao.get(key)?.let { return it.toDomain() }
        return try {
            val apiDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH))
            val d = api.timings(apiDate, lat, lng).data
            val entity = PrayerDayEntity(
                date = key,
                fajr = d.timings.Fajr, dhuhr = d.timings.Dhuhr, asr = d.timings.Asr,
                maghrib = d.timings.Maghrib, isha = d.timings.Isha,
                hijriDay = d.date.hijri.day.toInt(),
                hijriMonth = d.date.hijri.month.number,
                hijriYear = d.date.hijri.year.toInt(),
            )
            dao.upsert(entity)
            entity.toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dao.get(key)?.toDomain()
        }
    }

    /** Today's saved times only (no network); null if not loaded yet. */
    suspend fun cachedToday(): PrayerDay? = dao.get(LocalDate.now().toString())?.toDomain()
}

private fun String.toTime(): LocalTime = LocalTime.parse(trim().take(5))

private fun PrayerDayEntity.toDomain() = PrayerDay(
    date = LocalDate.parse(date),
    times = mapOf(
        Prayer.Fajr to fajr.toTime(),
        Prayer.Dhuhr to dhuhr.toTime(),
        Prayer.Asr to asr.toTime(),
        Prayer.Maghrib to maghrib.toTime(),
        Prayer.Isha to isha.toTime(),
    ),
    hijri = HijriDate(hijriDay, hijriMonth, hijriYear),
)
