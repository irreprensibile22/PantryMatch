package com.example.esamemobile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.data.repository.FriendsRepository
import com.example.esamemobile.data.repository.PantryRepository
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val pantryRepository: PantryRepository = PantryRepository(),
    private val friendsRepository: FriendsRepository = FriendsRepository(),
) : ViewModel() {

    private val _results = MutableStateFlow<Resource<List<RecipeMatch>>>(Resource.Success(emptyList()))
    val results: StateFlow<Resource<List<RecipeMatch>>> = _results.asStateFlow()

    /**
     * @param maxMissing  0 = solo preparabili; 1 o 2 = ammetti mancanti
     * @param friendsOnly se true cerca solo tra le ricette degli amici
     */
    fun search(maxMissing: Int, friendsOnly: Boolean) {
        _results.value = Resource.Loading
        viewModelScope.launch {
            _results.value = runCatching {
                val pantry = pantryRepository.getPantry()
                val authorIds = if (friendsOnly) friendsRepository.getFriendIds() else null
                recipeRepository.searchByPantry(pantry, maxMissing, authorIds)
            }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Errore nella ricerca", it) },
            )
        }
    }
}
