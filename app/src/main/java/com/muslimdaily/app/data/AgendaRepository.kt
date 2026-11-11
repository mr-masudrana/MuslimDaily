package com.muslimdaily.app.data

import android.content.Context
import com.google.gson.Gson
import com.muslimdaily.app.data.agenda.Practice
import com.muslimdaily.app.data.agenda.PracticeFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendaRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val data: List<Practice> by lazy {
        try {
            val json = ctx.assets.open("agenda/practices.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            Gson().fromJson(json, PracticeFile::class.java).practices.orEmpty()
                .filter { !it.id.isNullOrBlank() && it.title != null }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun practices(): List<Practice> = withContext(Dispatchers.IO) { data }
}
