package com.example.pantrymatch.ui.friends

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

// A friend's public recipes, annotated with the pantry match (same cards as Home).
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
        // optimistic update with rollback on error
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
