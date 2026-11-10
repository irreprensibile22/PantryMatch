package com.example.pantrymatch.data.repository

import android.net.Uri
import com.example.pantrymatch.data.model.PantryItem
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.data.model.RecipeMatch
import com.example.pantrymatch.util.IngredientMatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// Recipe CRUD on Firestore + photo upload to Storage.
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

    // the isPublic constraint is required: the Security Rules forbid reading other users' private recipes,
    // so without it Firestore would reject the whole query. Sorted client-side to avoid needing an index.
    suspend fun getPublicRecipesByAuthor(authorId: String): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("isPublic", true)
            .get().await()
            .toObjects(Recipe::class.java)
            .sortedByDescending { it.createdAt }

    suspend fun saveRecipe(recipe: Recipe): String {
        // author and normalized names are computed here, so the UI does not have to
        val withMeta = recipe.copy(
            authorId = requireUid(),
            authorName = auth.currentUser?.displayName.orEmpty(),
            ingredientNames = IngredientMatcher.ingredientNamesFor(recipe),
        )
        // empty id = new recipe (let Firestore generate the id), otherwise overwrite
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

    // Home feed: equality filter only (no composite index), sorted client-side
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

        // Firestore caps arrayContainsAny at 30 values: take the first 30 pantry ingredients
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
