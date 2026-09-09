package com.example.esamemobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Preferiti dell'utente: un documento per ricetta salvata in users/{uid}/favorites (l'id doc = id ricetta).
class FavoritesRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun favoritesCollection() =
        db.collection(USERS).document(requireUid()).collection(FAVORITES)

    suspend fun isFavorite(recipeId: String): Boolean =
        favoritesCollection().document(recipeId).get().await().exists()

    // dai piu recenti ai piu vecchi (campo addedAt salvato al momento del salvataggio)
    suspend fun getFavoriteIds(): List<String> =
        favoritesCollection()
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .get().await()
            .documents.map { it.id }

    suspend fun setFavorite(recipeId: String, favorite: Boolean) {
        val doc = favoritesCollection().document(recipeId)
        if (favorite) {
            doc.set(
                mapOf("recipeId" to recipeId, "addedAt" to System.currentTimeMillis())
            ).await()
        } else {
            doc.delete().await()
        }
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }

    companion object {
        private const val USERS = "users"
        private const val FAVORITES = "favorites"
    }
}
