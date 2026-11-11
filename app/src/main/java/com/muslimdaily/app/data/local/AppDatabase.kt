package com.muslimdaily.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks", indices = [Index(value = ["surah", "ayah"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "prayer_days")
data class PrayerDayEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
)

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val translationEnglish: String,
    val ayahCount: Int,
)

@Entity(tableName = "ayahs", primaryKeys = ["surah", "ayah"])
data class AyahEntity(
    val surah: Int,
    val ayah: Int,
    val arabic: String,
    val english: String,
    val bengali: String,
)

@Entity(tableName = "dua_favorites")
data class DuaFavoriteEntity(@PrimaryKey val itemId: String)

@Dao
interface DuaDao {
    @Query("SELECT itemId FROM dua_favorites")
    fun favorites(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM dua_favorites WHERE itemId = :id")
    suspend fun count(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(f: DuaFavoriteEntity)

    @Query("DELETE FROM dua_favorites WHERE itemId = :id")
    suspend fun delete(id: String)
}

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_days WHERE date = :date")
    suspend fun get(date: String): PrayerDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrayerDayEntity)
}

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY number")
    fun surahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :n")
    fun surah(n: Int): Flow<SurahEntity?>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun surahCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(list: List<SurahEntity>)

    @Query("SELECT * FROM ayahs WHERE surah = :n ORDER BY ayah")
    fun ayahs(n: Int): Flow<List<AyahEntity>>

    @Query("SELECT COUNT(*) FROM ayahs WHERE surah = :n")
    suspend fun ayahCount(n: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(list: List<AyahEntity>)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun bookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT ayah FROM bookmarks WHERE surah = :n")
    fun bookmarkedAyahs(n: Int): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE surah = :s AND ayah = :a")
    suspend fun bookmarkCount(s: Int, a: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(b: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surah = :s AND ayah = :a")
    suspend fun deleteBookmark(s: Int, a: Int)
}

@Database(
    entities = [BookmarkEntity::class, PrayerDayEntity::class, SurahEntity::class, AyahEntity::class, DuaFavoriteEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun quranDao(): QuranDao
    abstract fun duaDao(): DuaDao
}
