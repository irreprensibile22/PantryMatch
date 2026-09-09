package com.example.esamemobile.ui.recipe

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Crea/modifica ricetta: carica l'esistente (se in modifica) e gestisce salvataggio + upload foto.
class RecipeEditViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    private val _loaded = MutableStateFlow<Resource<Recipe>?>(null)
    val loaded: StateFlow<Resource<Recipe>?> = _loaded.asStateFlow()

    private val _saveState = MutableStateFlow<Resource<SaveOutcome>?>(null)
    val saveState: StateFlow<Resource<SaveOutcome>?> = _saveState.asStateFlow()

    fun load(recipeId: String) {
        if (recipeId.isEmpty()) return
        _loaded.value = Resource.Loading
        viewModelScope.launch {
            _loaded.value = runCatching { repository.getRecipe(recipeId) }
                .fold(
                    onSuccess = { r ->
                        if (r != null) Resource.Success(r) else Resource.Error("Ricetta non trovata")
                    },
                    onFailure = { Resource.Error(it.message ?: "Errore", it) },
                )
        }
    }

    fun save(recipe: Recipe, imageUri: Uri?) {
        _saveState.value = Resource.Loading
        viewModelScope.launch {
            // se Storage non e attivo (piano Spark) l'upload fallisce: salvo comunque la ricetta senza foto
            var photoSkipped = false
            val toSave = if (imageUri != null) {
                runCatching { repository.uploadImage(imageUri) }.fold(
                    onSuccess = { url -> recipe.copy(imageUrl = url) },
                    onFailure = { photoSkipped = true; recipe },
                )
            } else {
                recipe
            }

            _saveState.value = runCatching { repository.saveRecipe(toSave) }.fold(
                onSuccess = { Resource.Success(SaveOutcome(it, photoSkipped)) },
                onFailure = { Resource.Error(it.message ?: "Errore nel salvataggio", it) },
            )
        }
    }

    data class SaveOutcome(val id: String, val photoSkipped: Boolean)
}
