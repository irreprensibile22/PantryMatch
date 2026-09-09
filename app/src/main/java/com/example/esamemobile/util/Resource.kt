package com.example.esamemobile.util

// Stato di un'operazione asincrona esposto alla UI: caricamento, successo o errore.
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}
