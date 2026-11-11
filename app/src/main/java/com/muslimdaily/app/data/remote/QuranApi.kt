package com.muslimdaily.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApi {
    @GET("v1/surah")
    suspend fun surahs(): SurahListResponse

    // Arabic (Uthmani) + English (Sahih International) + Bengali (Muhiuddin Khan)
    @GET("v1/surah/{number}/editions/quran-uthmani,en.sahih,bn.bengali")
    suspend fun surahEditions(@Path("number") number: Int): SurahEditionsResponse
}

data class SurahListResponse(val data: List<SurahDto>)
data class SurahDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
)

data class SurahEditionsResponse(val data: List<EditionDto>)
data class EditionDto(val edition: EditionInfo, val ayahs: List<AyahDto>)
data class EditionInfo(val identifier: String)
data class AyahDto(val numberInSurah: Int, val text: String)
