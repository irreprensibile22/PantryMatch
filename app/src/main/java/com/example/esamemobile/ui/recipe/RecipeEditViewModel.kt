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

/**
 * Gestisce creazione e modifica di una ricetta. In modifica carica la ricetta
 * esistente per pre-compilare il form; al salvataggio carica prima l'eventuale
 * immagine su Storage e poi scrive il documento.
 */
class RecipeEditViewModel(
    private val repository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    // null finché non si modifica una ricetta esistente
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
            // L'upload dell'immagine è tentato a parte: se lo Storage non è
            // disponibile (es. progetto su piano Spark) si prosegue salvando la
            // ricetta senza foto, segnalandolo all'utente, invece di fallire.
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

    /**
     * Esito di un salvataggio: id della ricetta e se la foto è stata saltata
     * perché l'upload su Storage non è andato a buon fine.
     */
    data class SaveOutcome(val id: String, val photoSkipped: Boolean)
}
