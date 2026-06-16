package com.example.esamemobile.ui.search

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

class SearchViewModel(
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val pantryRepository: PantryRepository = PantryRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
) : ViewModel() {

    private val _results = MutableStateFlow<Resource<List<RecipeMatch>>>(Resource.Loading)
    val results: StateFlow<Resource<List<RecipeMatch>>> = _results.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    /** Filtro ingredienti mancanti: null = tutte; 0 = preparabili; 1/2 = mancanti ammessi. */
    var maxMissing: Int? = null
        private set

    /** Filtro categoria piatto (es. "Primo", "Dolce"); null = tutte le categorie. */
    var category: String? = null
        private set

    init {
        load()
        loadFavorites()
    }

    /** Applica i filtri scelti nel dialog e ricarica. */
    fun setFilters(maxMissing: Int?, category: String?) {
        this.maxMissing = maxMissing
        this.category = category
        load()
    }

    /**
     * Carica le ricette della community applicando i filtri attivi: prima la
     * categoria, poi il vincolo sugli ingredienti mancanti (senza vincolo le
     * mostra tutte, annotate sulla dispensa per i badge).
     */
    fun load() {
        _results.value = Resource.Loading
        viewModelScope.launch {
            _results.value = runCatching {
                val pantry = pantryRepository.getPantry()
                var recipes = recipeRepository.getFeed()
                category?.let { cat ->
                    recipes = recipes.filter { it.category.equals(cat, ignoreCase = true) }
                }
                val mm = maxMissing
                if (mm == null) IngredientMatcher.annotate(pantry, recipes)
                else IngredientMatcher.match(pantry, recipes, mm)
            }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Errore nel caricamento", it) },
            )
        }
    }

    /** Aggiorna l'insieme degli id preferiti (cuore pieno/vuoto nel feed). */
    fun loadFavorites() {
        viewModelScope.launch {
            _favoriteIds.value =
                runCatching { favoritesRepository.getFavoriteIds().toSet() }.getOrDefault(emptySet())
        }
    }

    /** Salva/rimuove rapidamente dal feed, con aggiornamento ottimista. */
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
