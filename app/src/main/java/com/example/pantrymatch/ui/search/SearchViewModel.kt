package com.example.pantrymatch.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.model.RecipeMatch
import com.example.pantrymatch.data.repository.FavoritesRepository
import com.example.pantrymatch.data.repository.PantryRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import com.example.pantrymatch.util.IngredientMatcher
import com.example.pantrymatch.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Home ViewModel: community feed annotated with the pantry match, plus filters and favorites.
class SearchViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val pantryRepository: PantryRepository = PantryRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
) : ViewModel() {

    private val _results = MutableStateFlow<Resource<List<RecipeMatch>>>(Resource.Loading)
    val results: StateFlow<Resource<List<RecipeMatch>>> = _results.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    var maxMissing: Int? = null
        private set

    var category: String? = null
        private set

    init {
        load()
        loadFavorites()
    }

    fun setFilters(maxMissing: Int?, category: String?) {
        this.maxMissing = maxMissing
        this.category = category
        load()
    }

    fun load() {
        _results.value = Resource.Loading
        viewModelScope.launch {
            _results.value = runCatching {
                val pantry = pantryRepository.getPantry()
                var recipes = recipeRepository.getFeed()
                category?.let { cat ->
                    recipes = recipes.filter { it.category.equals(cat, ignoreCase = true) }
                }
                // no filter: show the whole feed (annotate); with a filter: drop recipes missing more than mm (match)
                val mm = maxMissing
                if (mm == null) IngredientMatcher.annotate(pantry, recipes)
                else IngredientMatcher.match(pantry, recipes, mm)
            }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Errore nel caricamento", it) },
            )
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _favoriteIds.value =
                runCatching { favoritesRepository.getFavoriteIds().toSet() }.getOrDefault(emptySet())
        }
    }

    fun toggleFavorite(match: RecipeMatch, favorite: Boolean) {
        val id = match.recipe.id
        if (id.isEmpty()) return
        // update the UI immediately (optimistic) and restore the state if the write fails
        _favoriteIds.value =
            if (favorite) _favoriteIds.value + id else _favoriteIds.value - id
        viewModelScope.launch {
            runCatching { favoritesRepository.setFavorite(id, favorite) }
                .onFailure {
                    _favoriteIds.value =
                        if (favorite) _favoriteIds.value - id else _favoriteIds.value + id
                }
        }
    }
}
