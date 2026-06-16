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

/** Tutte le ricette dell'utente in griglia, con ricerca testuale per titolo. */
class RecipesGridViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    private val _recipes = MutableStateFlow<Resource<List<Recipe>>>(Resource.Loading)
    val recipes: StateFlow<Resource<List<Recipe>>> = _recipes.asStateFlow()

    private var all: List<Recipe> = emptyList()
    private var query: String = ""

    fun load() {
        _recipes.value = Resource.Loading
        viewModelScope.launch {
            runCatching { repository.getMyRecipes() }
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
