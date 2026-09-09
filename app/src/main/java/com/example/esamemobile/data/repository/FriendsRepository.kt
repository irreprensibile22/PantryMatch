package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.Friend
import com.example.esamemobile.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Amici dell'utente: ricerca per email e gestione della sotto-collezione users/{uid}/friends.
class FriendsRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun friendsCollection() =
        db.collection(USERS).document(requireUid()).collection(FRIENDS)

    suspend fun getFriends(): List<Friend> =
        friendsCollection().get().await().toObjects(Friend::class.java)

    suspend fun getFriendIds(): List<String> = getFriends().map { it.uid }

    suspend fun searchUsersByEmail(query: String): List<UserProfile> {
        val email = query.trim().lowercase()
        if (email.isEmpty()) return emptyList()
        return db.collection(USERS)
            .whereEqualTo("email", email)
            .limit(10)
            .get().await()
            .toObjects(UserProfile::class.java)
            .filter { it.uid != requireUid() } // non mostro me stesso tra i risultati
    }

    suspend fun addFriend(user: UserProfile) {
        val friend = Friend(
            uid = user.uid,
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl,
        )
        friendsCollection().document(user.uid).set(friend).await()
    }

    suspend fun removeFriend(uid: String) {
        friendsCollection().document(uid).delete().await()
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }

    companion object {
        private const val USERS = "users"
        private const val FRIENDS = "friends"
    }
}
