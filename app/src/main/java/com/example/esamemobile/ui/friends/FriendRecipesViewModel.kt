package com.example.esamemobile.ui.friends

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

/** Ricette pubbliche di un singolo amico, annotate sulla dispensa dell'utente. */
class FriendRecipesViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val pantryRepository: PantryRepository = PantryRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
) : ViewModel() {

    private val _results = MutableStateFlow<Resource<List<RecipeMatch>>>(Resource.Loading)
    val results: StateFlow<Resource<List<RecipeMatch>>> = _results.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun load(authorId: String) {
        _results.value = Resource.Loading
        viewModelScope.launch {
            _results.value = runCatching {
                val pantry = pantryRepository.getPantry()
                val recipes = recipeRepository.getPublicRecipesByAuthor(authorId)
                IngredientMatcher.annotate(pantry, recipes)
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
