package com.example.pantrymatch.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.data.repository.FavoritesRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import com.example.pantrymatch.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel for the "See all" grid: shows own recipes or saved ones, with search.
class RecipesGridViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
) : ViewModel() {

    // grid source, chosen by the caller
    enum class Mode { MINE, SAVED }

    private val _recipes = MutableStateFlow<Resource<List<Recipe>>>(Resource.Loading)
    val recipes: StateFlow<Resource<List<Recipe>>> = _recipes.asStateFlow()

    // keep the full list and filter in memory, so searching does not re-run the query
    private var all: List<Recipe> = emptyList()
    private var query: String = ""

    fun load(mode: Mode) {
        _recipes.value = Resource.Loading
        viewModelScope.launch {
            runCatching {
                when (mode) {
                    Mode.MINE -> repository.getMyRecipes()
                    Mode.SAVED -> repository.getRecipesByIds(favoritesRepository.getFavoriteIds())
                }
            }
                .onSuccess {
                    all = it
                    emitFiltered()
                }
                .onFailure { _recipes.value = Resource.Error(it.message ?: "Errore caricamento", it) }
        }
    }

    fun search(query: String) {
        this.query = query.trim()
        emitFiltered()
    }

    private fun emitFiltered() {
        val filtered =
            if (query.isEmpty()) all
            else all.filter { it.title.contains(query, ignoreCase = true) }
        _recipes.value = Resource.Success(filtered)
    }
}
