package com.example.esamemobile.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.repository.PantryRepository
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PantryViewModel(
    private val repository: PantryRepository = PantryRepository(),
) : ViewModel() {

    private val _items = MutableStateFlow<Resource<List<PantryItem>>>(Resource.Loading)
    val items: StateFlow<Resource<List<PantryItem>>> = _items.asStateFlow()

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
