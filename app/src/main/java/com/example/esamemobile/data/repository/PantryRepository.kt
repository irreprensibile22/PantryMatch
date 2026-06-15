package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.util.IngredientMatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CRUD sull'inventario (dispensa) dell'utente corrente, salvato nella
 * sub-collection `users/{uid}/pantry`.
 */
class PantryRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun collection() =
        db.collection("users")
            .document(requireUid())
            .collection("pantry")

    suspend fun getPantry(): List<PantryItem> =
        collection().get().await().toObjects(PantryItem::class.java)

    suspend fun addItem(item: PantryItem): String {
        // Normalizza il nome così da renderlo coerente col matcher
        val normalized = item.copy(name = IngredientMatcher.normalize(item.name))
        val ref = collection().document()
        ref.set(normalized).await()
        return ref.id
    }

    suspend fun updateItem(item: PantryItem) {
        require(item.id.isNotEmpty()) { "Item senza id" }
        collection().document(item.id).set(item).await()
    }

    suspend fun deleteItem(itemId: String) {
        collection().document(itemId).delete().await()
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }
}
