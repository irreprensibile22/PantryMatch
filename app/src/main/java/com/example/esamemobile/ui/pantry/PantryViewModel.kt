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

    // esito della scansione barcode, consumato una sola volta dalla UI (vedi _scanResult)
    sealed interface ScanResult {
        // name = testo da pre-compilare nel campo, helper = nome commerciale mostrato sotto
        data class Found(val name: String, val helper: String?) : ScanResult
        data class NotFound(val barcode: String) : ScanResult
        data object Error : ScanResult
    }

    private val _items = MutableStateFlow<Resource<List<PantryItem>>>(Resource.Loading)
    val items: StateFlow<Resource<List<PantryItem>>> = _items.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    // evento one-shot: SharedFlow invece di StateFlow per non riemettere l'esito dopo una rotazione
    private val _scanResult = MutableSharedFlow<ScanResult>(extraBufferCapacity = 1)
    val scanResult: SharedFlow<ScanResult> = _scanResult.asSharedFlow()

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

    private fun ProductLookupRepository.ProductInfo?.toScanResult(barcode: String): ScanResult {
        if (this == null) return ScanResult.NotFound(barcode)
        // preferisco la categoria: e piu vicina a un ingrediente utile al matching che non il nome commerciale
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
