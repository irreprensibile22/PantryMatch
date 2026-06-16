package com.example.esamemobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    private val _myRecipes = MutableStateFlow<Resource<List<Recipe>>>(Resource.Loading)
    val myRecipes: StateFlow<Resource<List<Recipe>>> = _myRecipes.asStateFlow()

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

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteRecipe(recipeId) }
                .onSuccess { loadMyRecipes() }
                .onFailure { _myRecipes.value = Resource.Error(it.message ?: "Errore eliminazione", it) }
        }
    }
}
