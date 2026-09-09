package com.example.esamemobile.data.repository

import android.net.Uri
import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.util.IngredientMatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// CRUD delle ricette su Firestore + upload foto su Storage.
class RecipeRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private fun recipes() = db.collection(RECIPES)

    suspend fun getRecipe(id: String): Recipe? =
        recipes().document(id).get().await().toObject(Recipe::class.java)

    suspend fun getRecipesByIds(ids: List<String>): List<Recipe> =
        ids.mapNotNull { getRecipe(it) }

    suspend fun getMyRecipes(): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", requireUid())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Recipe::class.java)

    // il vincolo isPublic e obbligatorio: le Security Rules vietano di leggere ricette altrui non pubbliche,
    // quindi senza di esso Firestore rifiuterebbe l'intera query. Ordino lato client per non usare un indice.
    suspend fun getPublicRecipesByAuthor(authorId: String): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("isPublic", true)
            .get().await()
            .toObjects(Recipe::class.java)
            .sortedByDescending { it.createdAt }

    suspend fun saveRecipe(recipe: Recipe): String {
        // autore e nomi normalizzati calcolati qui, cosi la UI non deve pensarci
        val withMeta = recipe.copy(
            authorId = requireUid(),
            authorName = auth.currentUser?.displayName.orEmpty(),
            ingredientNames = IngredientMatcher.ingredientNamesFor(recipe),
        )
        // id vuoto = nuova ricetta (lascio generare l'id a Firestore), altrimenti sovrascrivo
        return if (withMeta.id.isEmpty()) {
            val ref = recipes().document()
            ref.set(withMeta).await()
            ref.id
        } else {
            recipes().document(withMeta.id).set(withMeta).await()
            withMeta.id
        }
    }

    suspend fun deleteRecipe(id: String) {
        recipes().document(id).delete().await()
    }

    suspend fun uploadImage(localUri: Uri): String {
        val ref = storage.reference
            .child("recipe_images")
            .child(requireUid())
            .child("${System.currentTimeMillis()}.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    // feed della Home: solo filtro di uguaglianza (niente indice composito), ordinamento lato client
    suspend fun getFeed(authorIds: List<String>? = null): List<Recipe> {
        if (authorIds != null && authorIds.isEmpty()) return emptyList()
        var recipes = recipes()
            .whereEqualTo("isPublic", true)
            .get().await()
            .toObjects(Recipe::class.java)
        if (authorIds != null) {
            val allowed = authorIds.toHashSet()
            recipes = recipes.filter { it.authorId in allowed }
        }
        return recipes.sortedByDescending { it.createdAt }
    }

    suspend fun searchByPantry(
        pantry: List<PantryItem>,
        maxMissing: Int,
        authorIds: List<String>? = null,
    ): List<RecipeMatch> {
        val owned = pantry.map { IngredientMatcher.normalize(it.name) }
        if (owned.isEmpty()) return emptyList()
        if (authorIds != null && authorIds.isEmpty()) return emptyList()

        // Firestore limita arrayContainsAny a 30 valori: prendo i primi 30 ingredienti della dispensa
        val query: Query = recipes()
            .whereEqualTo("isPublic", true)
            .whereArrayContainsAny("ingredientNames", owned.take(30))

        var candidates = query.get().await().toObjects(Recipe::class.java)
        if (authorIds != null) {
            val allowed = authorIds.toHashSet()
            candidates = candidates.filter { it.authorId in allowed }
        }
        return IngredientMatcher.match(pantry, candidates, maxMissing)
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }

    companion object {
        private const val RECIPES = "recipes"
    }
}
