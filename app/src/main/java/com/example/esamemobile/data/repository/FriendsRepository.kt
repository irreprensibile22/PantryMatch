package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.Friend
import com.example.esamemobile.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gestisce gli amici dell'utente corrente (sub-collection
 * `users/{uid}/friends`) e la ricerca di altri utenti per email.
 */
class FriendsRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun friendsCollection() =
        db.collection(USERS).document(requireUid()).collection(FRIENDS)

    suspend fun getFriends(): List<Friend> =
        friendsCollection().get().await().toObjects(Friend::class.java)

    /** Solo gli `uid` degli amici: usata per filtrare la ricerca ricette. */
    suspend fun getFriendIds(): List<String> = getFriends().map { it.uid }

    /**
     * Cerca utenti per email esatta (case-insensitive). Le email sono salvate in
     * minuscolo in fase di registrazione, quindi basta una query di uguaglianza.
     * Esclude sé stessi dai risultati.
     */
    suspend fun searchUsersByEmail(query: String): List<UserProfile> {
        val email = query.trim().lowercase()
        if (email.isEmpty()) return emptyList()
        return db.collection(USERS)
            .whereEqualTo("email", email)
            .limit(10)
            .get().await()
            .toObjects(UserProfile::class.java)
            .filter { it.uid != requireUid() }
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
