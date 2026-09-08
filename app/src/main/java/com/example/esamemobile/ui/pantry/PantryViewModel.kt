package com.example.esamemobile.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.repository.PantryRepository
import com.example.esamemobile.data.repository.ProductLookupRepository
import com.example.esamemobile.util.IngredientMatcher
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PantryViewModel(
    private val repository: PantryRepository = PantryRepository(),
    private val productLookup: ProductLookupRepository = ProductLookupRepository(),
) : ViewModel() {

    /** Esito della risoluzione di un codice a barre in un prodotto. */
    sealed interface ScanResult {
        /**
         * Prodotto riconosciuto.
         *
         * @param name   valore da pre-compilare nel campo nome (la categoria se
         *               disponibile, altrimenti il nome commerciale).
         * @param helper testo di aiuto sotto il campo (di norma il nome
         *               commerciale), oppure null se non c'è nulla da mostrare.
         */
        data class Found(val name: String, val helper: String?) : ScanResult
        /** Codice letto ma non presente nel database Open Food Facts. */
        data class NotFound(val barcode: String) : ScanResult
        /** Errore di rete durante il lookup. */
        data object Error : ScanResult
    }

    private val _items = MutableStateFlow<Resource<List<PantryItem>>>(Resource.Loading)
    val items: StateFlow<Resource<List<PantryItem>>> = _items.asStateFlow()

    /** Stato di caricamento del lookup prodotto (per mostrare un indicatore). */
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _scanResult = MutableSharedFlow<ScanResult>(extraBufferCapacity = 1)
    val scanResult: SharedFlow<ScanResult> = _scanResult.asSharedFlow()

    /** Risolve il codice a barre scansionato nelle info del prodotto. */
    fun lookupBarcode(barcode: String) {
        _scanning.value = true
        viewModelScope.launch {
            val result = runCatching { productLookup.lookup(barcode) }
                .fold(
                    onSuccess = { info -> info.toScanResult(barcode) },
                    onFailure = { ScanResult.Error },
                )
            _scanning.value = false
            _scanResult.emit(result)
        }
    }

    /**
     * Preferisce la **categoria** come nome (più vicina a un ingrediente per il
     * matching) e mostra il nome commerciale come aiuto. Se manca la categoria,
     * usa il nome commerciale nel campo. Se manca tutto, è un NotFound.
     */
    private fun ProductLookupRepository.ProductInfo?.toScanResult(barcode: String): ScanResult {
        if (this == null) return ScanResult.NotFound(barcode)
        return when {
            category != null -> ScanResult.Found(
                name = IngredientMatcher.normalize(category),
                helper = name,
            )
            name != null -> ScanResult.Found(
                name = IngredientMatcher.normalize(name),
                helper = null,
            )
            else -> ScanResult.NotFound(barcode)
        }
    }

    fun load() {
        _items.value = Resource.Loading
        viewModelScope.launch {
            _items.value = runCatching { repository.getPantry() }
                .fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Errore caricamento dispensa", it) },
                )
        }
    }

    fun addItem(name: String, quantity: Double?, unit: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.addItem(PantryItem(name = name, quantity = quantity, unit = unit)) }
                .onSuccess { load() }
                .onFailure { _items.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }

    fun updateItem(item: PantryItem) {
        if (item.name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.updateItem(item) }
                .onSuccess { load() }
                .onFailure { _items.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteItem(itemId) }
                .onSuccess { load() }
                .onFailure { _items.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }
}
