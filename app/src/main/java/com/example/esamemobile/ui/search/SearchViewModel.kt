package com.example.esamemobile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.data.repository.FavoritesRepository
import com.example.esamemobile.data.repository.PantryRepository
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.IngredientMatcher
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel della Home: feed community annotato col match sulla dispensa, piu filtri e preferiti.
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
                // senza filtro mostro tutto il feed (annotate); con filtro scarto oltre mm mancanti (match)
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
        // aggiorno subito la UI (ottimista) e, se la scrittura fallisce, ripristino lo stato
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
