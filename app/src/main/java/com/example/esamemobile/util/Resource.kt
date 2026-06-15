package com.example.esamemobile.util

/**
 * Wrapper generico per esporre dallo strato UI lo stato di un'operazione
 * asincrona (loading / success / error). Usato dai ViewModel via StateFlow.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}
