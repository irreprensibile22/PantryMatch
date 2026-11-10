package com.example.pantrymatch.ui.recipebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.model.Recipe
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

// Recipe Book: two separate lists, "My recipes" and "Saved" (favorites annotated with the pantry match).
class RecipeBookViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
    private val pantryRepository: PantryRepository = PantryRepository(),
) : ViewModel() {

    private val _myRecipes = MutableStateFlow<Resource<List<Recipe>>>(Resource.Loading)
    val myRecipes: StateFlow<Resource<List<Recipe>>> = _myRecipes.asStateFlow()

    private val _favorites = MutableStateFlow<Resource<List<RecipeMatch>>>(Resource.Loading)
    val favorites: StateFlow<Resource<List<RecipeMatch>>> = _favorites.asStateFlow()

    fun loadMyRecipes() {
        _myRecipes.value = Resource.Loading
        viewModelScope.launch {
            _myRecipes.value = runCatching { repository.getMyRecipes() }
                .fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Errore caricamento ricette", it) },
                )
        }
    }

    fun loadFavorites() {
        _favorites.value = Resource.Loading
        viewModelScope.launch {
            _favorites.value = runCatching {
                // fetch the recipes from the favorite ids and annotate them with the pantry match
                val ids = favoritesRepository.getFavoriteIds()
                val recipes = repository.getRecipesByIds(ids)
                val pantry = pantryRepository.getPantry()
                IngredientMatcher.annotate(pantry, recipes)
            }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Errore caricamento preferiti", it) },
            )
        }
    }

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch {
            runCatching { favoritesRepository.setFavorite(recipeId, false) }
                .onSuccess { loadFavorites() }
                .onFailure { _favorites.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteRecipe(recipeId) }
                .onSuccess { loadMyRecipes() }
                .onFailure { _myRecipes.value = Resource.Error(it.message ?: "Errore eliminazione", it) }
        }
    }
}
