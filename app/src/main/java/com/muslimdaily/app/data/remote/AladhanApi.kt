package com.muslimdaily.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {
    // method=1: University of Islamic Sciences, Karachi | school=1: Hanafi (Asr)
    @GET("v1/timings/{date}")
    suspend fun timings(
        @Path("date") date: String, // dd-MM-yyyy
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1,
    ): AladhanResponse
}

data class AladhanResponse(val data: AladhanData)
data class AladhanData(val timings: Timings, val date: DateInfo)
data class Timings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String,
)
data class DateInfo(val hijri: Hijri)
data class Hijri(val day: String, val month: HijriMonth, val year: String)
data class HijriMonth(val number: Int)
