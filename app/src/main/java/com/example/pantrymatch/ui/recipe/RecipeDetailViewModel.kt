package com.example.pantrymatch.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.data.repository.AuthRepository
import com.example.pantrymatch.data.repository.FavoritesRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import com.example.pantrymatch.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Recipe detail state: data, favorite flag and delete result.
class RecipeDetailViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
    private val favorites: FavoritesRepository = FavoritesRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _recipe = MutableStateFlow<Resource<Recipe>>(Resource.Loading)
    val recipe: StateFlow<Resource<Recipe>> = _recipe.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState.asStateFlow()

    private var recipeId: String = ""

    fun load(recipeId: String) {
        this.recipeId = recipeId
        _recipe.value = Resource.Loading
        viewModelScope.launch {
            _recipe.value = runCatching { repository.getRecipe(recipeId) }
                .fold(
                    onSuccess = { r ->
                        if (r != null) Resource.Success(r)
                        else Resource.Error("Ricetta non trovata")
                    },
                    onFailure = { Resource.Error(it.message ?: "Errore", it) },
                )
        }
        viewModelScope.launch {
            _isFavorite.value = runCatching { favorites.isFavorite(recipeId) }.getOrDefault(false)
        }
    }

    // only the author sees the edit/delete actions
    fun isAuthor(recipe: Recipe): Boolean {
        val uid = authRepository.currentUid
        return uid != null && recipe.authorId == uid
    }

    fun toggleFavorite() {
        if (recipeId.isEmpty()) return
        // update the icon immediately (optimistic) and restore it if the write fails
        val target = !_isFavorite.value
        _isFavorite.value = target
        viewModelScope.launch {
            runCatching { favorites.setFavorite(recipeId, target) }
                .onFailure { _isFavorite.value = !target }
        }
    }

    fun deleteRecipe() {
        if (recipeId.isEmpty()) return
        _deleteState.value = Resource.Loading
        viewModelScope.launch {
            _deleteState.value = runCatching { repository.deleteRecipe(recipeId) }
                .fold(
                    onSuccess = { Resource.Success(Unit) },
                    onFailure = { Resource.Error(it.message ?: "Errore nell'eliminazione", it) },
                )
        }
    }
}
