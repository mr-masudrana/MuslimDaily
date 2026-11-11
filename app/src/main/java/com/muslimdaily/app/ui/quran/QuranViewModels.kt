package com.muslimdaily.app.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.LastReadStore
import com.muslimdaily.app.data.QuranRepository
import com.muslimdaily.app.data.local.AyahEntity
import com.muslimdaily.app.data.local.BookmarkEntity
import com.muslimdaily.app.data.local.SurahEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoadState { Loading, Done, Error }

// ---------------- Surah list ----------------

data class QuranUiState(
    val load: LoadState = LoadState.Loading,
    val surahs: List<SurahEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val lastRead: Pair<Int, Int>? = null,
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val repo: QuranRepository,
    lastReadStore: LastReadStore,
) : ViewModel() {

    private val load = MutableStateFlow(LoadState.Loading)

    val ui: StateFlow<QuranUiState> = combine(
        repo.surahs(), repo.bookmarks(), lastReadStore.lastRead, load
    ) { surahs, bookmarks, lastRead, l ->
        QuranUiState(
            load = if (surahs.isNotEmpty()) LoadState.Done else l,
            surahs = surahs,
            bookmarks = bookmarks,
            lastRead = lastRead,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            load.value = LoadState.Loading
            load.value = if (repo.ensureSurahs()) LoadState.Done else LoadState.Error
        }
    }
}

// ---------------- Reader ----------------

data class ReaderUiState(
    val load: LoadState = LoadState.Loading,
    val surah: SurahEntity? = null,
    val ayahs: List<AyahEntity> = emptyList(),
    val bookmarked: Set<Int> = emptySet(),
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: QuranRepository,
    private val lastRead: LastReadStore,
) : ViewModel() {

    val surahNumber: Int = checkNotNull(savedState.get<Int>("surah"))
    val initialAyah: Int = savedState.get<Int>("ayah") ?: 1

    private val load = MutableStateFlow(LoadState.Loading)

    val ui: StateFlow<ReaderUiState> = combine(
        load,
        repo.surah(surahNumber),
        repo.ayahs(surahNumber),
        repo.bookmarkedAyahs(surahNumber),
    ) { l, surah, ayahs, bookmarked ->
        ReaderUiState(
            load = if (ayahs.isNotEmpty()) LoadState.Done else l,
            surah = surah,
            ayahs = ayahs,
            bookmarked = bookmarked.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init { retry() }

    fun retry() {
        viewModelScope.launch {
            load.value = LoadState.Loading
            repo.ensureSurahs()
            load.value = if (repo.ensureAyahs(surahNumber)) LoadState.Done else LoadState.Error
        }
    }

    fun toggleBookmark(ayah: Int) {
        viewModelScope.launch { repo.toggleBookmark(surahNumber, ayah) }
    }

    fun saveLastRead(ayah: Int) {
        viewModelScope.launch { lastRead.save(surahNumber, ayah) }
    }
}
