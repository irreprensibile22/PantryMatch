package com.example.esamemobile.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    private val _recipe = MutableStateFlow<Resource<Recipe>>(Resource.Loading)
    val recipe: StateFlow<Resource<Recipe>> = _recipe.asStateFlow()

    fun load(recipeId: String) {
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
    }
}
