package com.example.esamemobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.util.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val state: StateFlow<Resource<FirebaseUser>?> = _state.asStateFlow()

    val isLoggedIn: Boolean get() = runCatching { repository.isLoggedIn }.getOrDefault(false)

    fun login(email: String, password: String) {
        _state.value = Resource.Loading
        viewModelScope.launch {
            _state.value = runCatching { repository.login(email, password) }
                .fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Login fallito", it) },
                )
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _state.value = Resource.Loading
        viewModelScope.launch {
            _state.value = runCatching { repository.register(email, password, displayName) }
                .fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Registrazione fallita", it) },
                )
        }
    }
}
