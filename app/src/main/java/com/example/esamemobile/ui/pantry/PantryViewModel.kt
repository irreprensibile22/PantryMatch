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

    /** Esito della risoluzione di un codice a barre in un nome prodotto. */
    sealed interface ScanResult {
        /** Prodotto riconosciuto: [name] è pronto (già normalizzato) per il dialog. */
        data class Found(val name: String) : ScanResult
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

    /** Risolve il codice a barre scansionato nel nome del prodotto. */
    fun lookupBarcode(barcode: String) {
        _scanning.value = true
        viewModelScope.launch {
            val result = runCatching { productLookup.lookupName(barcode) }
                .fold(
                    onSuccess = { name ->
                        if (name != null) ScanResult.Found(IngredientMatcher.normalize(name))
                        else ScanResult.NotFound(barcode)
                    },
                    onFailure = { ScanResult.Error },
                )
            _scanning.value = false
            _scanResult.emit(result)
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
