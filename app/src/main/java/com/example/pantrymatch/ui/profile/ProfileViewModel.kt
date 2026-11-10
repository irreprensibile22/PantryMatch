package com.example.pantrymatch.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.repository.AuthRepository
import com.example.pantrymatch.data.repository.FavoritesRepository
import com.example.pantrymatch.data.repository.FriendsRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Profile: stats and account management (name/email/password). Results are one-shot events.
class ProfileViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepository(),
    private val friendsRepository: FriendsRepository = FriendsRepository(),
) : ViewModel() {

    data class Stats(val recipes: Int = 0, val saved: Int = 0, val friends: Int = 0)

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            val recipes = runCatching { recipeRepository.getMyRecipes().size }.getOrDefault(0)
            val saved = runCatching { favoritesRepository.getFavoriteIds().size }.getOrDefault(0)
            val friends = runCatching { friendsRepository.getFriends().size }.getOrDefault(0)
            _stats.value = Stats(recipes, saved, friends)
        }
    }

    sealed interface Event {
        data class NameUpdated(val name: String) : Event
        data object PasswordUpdated : Event
        data object EmailVerificationSent : Event
        data class PasswordResetSent(val email: String) : Event
        data class EmailReconciled(val email: String) : Event
        data object SessionRevoked : Event
        data class Error(val message: String) : Event
    }

    // one-shot events (dialog/Toast): SharedFlow so they are not replayed after a rotation
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun updateName(name: String) = launchCatching("Errore aggiornamento nome") {
        auth.updateDisplayName(name)
        _events.emit(Event.NameUpdated(name.trim()))
    }

    fun changePassword(currentPassword: String, newPassword: String) =
        launchCatching("Errore cambio password") {
            auth.changePassword(currentPassword, newPassword)
            _events.emit(Event.PasswordUpdated)
        }

    fun changeEmail(currentPassword: String, newEmail: String) =
        launchCatching("Errore cambio email") {
            auth.changeEmail(currentPassword, newEmail)
            _events.emit(Event.EmailVerificationSent)
        }

    fun sendPasswordReset() = launchCatching("Errore invio email") {
        val email = auth.sendPasswordReset()
        _events.emit(Event.PasswordResetSent(email))
    }

    fun reconcileEmail() {
        viewModelScope.launch {
            runCatching { auth.reconcileEmail() }.onSuccess { result ->
                when (result) {
                    is AuthRepository.EmailSync.Updated ->
                        _events.emit(Event.EmailReconciled(result.email))
                    AuthRepository.EmailSync.SessionRevoked ->
                        _events.emit(Event.SessionRevoked)
                    AuthRepository.EmailSync.Unchanged -> Unit
                }
            }
        }
    }

    // helper: runs block and, on error, emits an Event.Error with a fallback message
    private fun launchCatching(fallback: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { _events.emit(Event.Error(it.message ?: fallback)) }
        }
    }
}
