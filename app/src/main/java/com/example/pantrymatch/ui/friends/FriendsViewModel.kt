package com.example.pantrymatch.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrymatch.data.model.UserProfile
import com.example.pantrymatch.data.repository.FriendsRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import com.example.pantrymatch.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Friends list (with public recipe count) and user search by email.
class FriendsViewModel(
    private val repository: FriendsRepository = FriendsRepository(),
    private val recipeRepository: RecipeRepository = RecipeRepository(),
) : ViewModel() {

    private val _friends = MutableStateFlow<Resource<List<FriendItem>>>(Resource.Loading)
    val friends: StateFlow<Resource<List<FriendItem>>> = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<List<UserProfile>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<UserProfile>>> = _searchResults.asStateFlow()

    fun loadFriends() {
        _friends.value = Resource.Loading
        viewModelScope.launch {
            _friends.value = runCatching {
                repository.getFriends().map { friend ->
                    // count each friend's public recipes; on failure show 0 without breaking the list
                    val count = runCatching {
                        recipeRepository.getPublicRecipesByAuthor(friend.uid).size
                    }.getOrDefault(0)
                    FriendItem(friend, count)
                }
            }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Errore caricamento amici", it) },
            )
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }
        _searchResults.value = Resource.Loading
        viewModelScope.launch {
            _searchResults.value = runCatching { repository.searchUsersByEmail(query) }
                .fold(
                    onSuccess = { Resource.Success(it) },
                    onFailure = { Resource.Error(it.message ?: "Errore nella ricerca", it) },
                )
        }
    }

    fun addFriend(user: UserProfile) {
        viewModelScope.launch {
            runCatching { repository.addFriend(user) }
                .onSuccess {
                    _searchResults.value = Resource.Success(emptyList())
                    loadFriends()
                }
                .onFailure { _friends.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            runCatching { repository.removeFriend(uid) }
                .onSuccess { loadFriends() }
                .onFailure { _friends.value = Resource.Error(it.message ?: "Errore", it) }
        }
    }
}
