package com.example.esamemobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gestisce le ricette preferite dell'utente corrente (sub-collection
 * `users/{uid}/favorites`). Ogni preferito è un documento con id uguale
 * all'id della ricetta, così il toggle è una semplice set/delete.
 */
class FavoritesRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun favoritesCollection() =
        db.collection(USERS).document(requireUid()).collection(FAVORITES)

    suspend fun isFavorite(recipeId: String): Boolean =
        favoritesCollection().document(recipeId).get().await().exists()

    /** Aggiunge ([favorite] = true) o rimuove il preferito per [recipeId]. */
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
