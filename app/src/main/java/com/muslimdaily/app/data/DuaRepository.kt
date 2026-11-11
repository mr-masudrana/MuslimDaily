package com.muslimdaily.app.data

import android.content.Context
import com.google.gson.Gson
import com.muslimdaily.app.data.dua.DuaCategory
import com.muslimdaily.app.data.dua.DuaFile
import com.muslimdaily.app.data.dua.DuaItem
import com.muslimdaily.app.data.local.DuaDao
import com.muslimdaily.app.data.local.DuaFavoriteEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuaRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: DuaDao,
) {
    /** Reads assets/dua/duas.json once. Invalid or incomplete entries are skipped, never crash. */
    private val data: List<DuaCategory> by lazy {
        try {
            val json = ctx.assets.open("dua/duas.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            Gson().fromJson(json, DuaFile::class.java).categories.orEmpty()
                .filter { !it.id.isNullOrBlank() }
                .map { cat ->
                    cat.copy(items = cat.items.orEmpty().filter { !it.id.isNullOrBlank() && !it.arabic.isNullOrBlank() })
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun categories(): List<DuaCategory> = withContext(Dispatchers.IO) { data }

    suspend fun item(id: String): DuaItem? = withContext(Dispatchers.IO) {
        data.asSequence().flatMap { it.items.orEmpty().asSequence() }.firstOrNull { it.id == id }
    }

    val favorites: Flow<Set<String>> = dao.favorites().map { it.toSet() }

    suspend fun toggleFavorite(id: String) {
        if (dao.count(id) > 0) dao.delete(id) else dao.insert(DuaFavoriteEntity(id))
    }
}
