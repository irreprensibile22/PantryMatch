package com.example.pantrymatch.data.repository

import com.example.pantrymatch.data.model.PantryItem
import com.example.pantrymatch.util.IngredientMatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// The user's pantry in Firestore (users/{uid}/pantry).
class PantryRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun collection() =
        db.collection("users")
            .document(requireUid())
            .collection("pantry")

    suspend fun getPantry(): List<PantryItem> =
        collection().get().await().toObjects(PantryItem::class.java)

    // store the name already normalized, so matching stays consistent without re-normalizing each time
    suspend fun addItem(item: PantryItem): String {
        val normalized = item.copy(name = IngredientMatcher.normalize(item.name))
        val ref = collection().document()
        ref.set(normalized).await()
        return ref.id
    }

    suspend fun updateItem(item: PantryItem) {
        require(item.id.isNotEmpty()) { "Item senza id" }
        val normalized = item.copy(name = IngredientMatcher.normalize(item.name))
        collection().document(item.id).set(normalized).await()
    }

    suspend fun deleteItem(itemId: String) {
        collection().document(itemId).delete().await()
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }
}
