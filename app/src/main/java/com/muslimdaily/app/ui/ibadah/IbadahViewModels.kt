package com.muslimdaily.app.ui.ibadah

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.DuaRepository
import com.muslimdaily.app.data.dua.DuaCategory
import com.muslimdaily.app.data.dua.DuaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IbadahUiState(
    val loading: Boolean = true,
    val categories: List<DuaCategory> = emptyList(),
    val favorites: Set<String> = emptySet(),
)

@HiltViewModel
class IbadahViewModel @Inject constructor(repo: DuaRepository) : ViewModel() {
    private val cats = MutableStateFlow<List<DuaCategory>?>(null)

    val ui: StateFlow<IbadahUiState> = combine(cats, repo.favorites) { c, f ->
        IbadahUiState(loading = c == null, categories = c.orEmpty(), favorites = f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IbadahUiState())

    init {
        viewModelScope.launch { cats.value = repo.categories() }
    }
}

data class DuaDetailUiState(
    val loading: Boolean = true,
    val item: DuaItem? = null,
    val favorite: Boolean = false,
)

@HiltViewModel
class DuaDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: DuaRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedState.get<String>("id"))
    private val found = MutableStateFlow<Pair<Boolean, DuaItem?>>(true to null)

    val ui: StateFlow<DuaDetailUiState> = combine(found, repo.favorites) { (loading, item), favs ->
        DuaDetailUiState(loading, item, id in favs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuaDetailUiState())

    init {
        viewModelScope.launch { found.value = false to repo.item(id) }
    }

    fun toggleFavorite() {
        viewModelScope.launch { repo.toggleFavorite(id) }
    }
}
