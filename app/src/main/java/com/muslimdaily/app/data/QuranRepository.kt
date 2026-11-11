package com.muslimdaily.app.data

import com.muslimdaily.app.data.local.AyahEntity
import com.muslimdaily.app.data.local.BookmarkEntity
import com.muslimdaily.app.data.local.QuranDao
import com.muslimdaily.app.data.local.SurahEntity
import com.muslimdaily.app.data.remote.QuranApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class QuranRepository @Inject constructor(
    private val api: QuranApi,
    private val dao: QuranDao,
) {
    fun surahs() = dao.surahs()
    fun surah(n: Int) = dao.surah(n)
    fun ayahs(n: Int) = dao.ayahs(n)
    fun bookmarks() = dao.bookmarks()
    fun bookmarkedAyahs(n: Int) = dao.bookmarkedAyahs(n)

    /** Downloads the surah list once, then it is available offline. */
    suspend fun ensureSurahs(): Boolean {
        if (dao.surahCount() >= 114) return true
        return try {
            dao.insertSurahs(
                api.surahs().data.map {
                    SurahEntity(it.number, it.name, it.englishName, it.englishNameTranslation, it.numberOfAyahs)
                }
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** Downloads one surah (Arabic + English + Bengali) the first time it is opened. */
    suspend fun ensureAyahs(n: Int): Boolean {
        if (dao.ayahCount(n) > 0) return true
        return try {
            val eds = api.surahEditions(n).data
            val ar = eds.first { it.edition.identifier == "quran-uthmani" }.ayahs
            val en = eds.first { it.edition.identifier == "en.sahih" }.ayahs
            val bn = eds.first { it.edition.identifier == "bn.bengali" }.ayahs
            dao.insertAyahs(
                ar.indices.map { i ->
                    AyahEntity(n, ar[i].numberInSurah, ar[i].text, en[i].text, bn[i].text)
                }
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleBookmark(surah: Int, ayah: Int) {
        if (dao.bookmarkCount(surah, ayah) > 0) dao.deleteBookmark(surah, ayah)
        else dao.insertBookmark(BookmarkEntity(surah = surah, ayah = ayah))
    }
}
