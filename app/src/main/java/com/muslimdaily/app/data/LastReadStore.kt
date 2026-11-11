package com.muslimdaily.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "muslim_daily_prefs")

@Singleton
class LastReadStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val surahKey = intPreferencesKey("last_surah")
    private val ayahKey = intPreferencesKey("last_ayah")

    /** (surah, ayah) or null if nothing read yet. */
    val lastRead: Flow<Pair<Int, Int>?> = ctx.dataStore.data.map { p ->
        val s = p[surahKey]
        val a = p[ayahKey]
        if (s != null && a != null) s to a else null
    }

    suspend fun save(surah: Int, ayah: Int) {
        ctx.dataStore.edit {
            it[surahKey] = surah
            it[ayahKey] = ayah
        }
    }
}
