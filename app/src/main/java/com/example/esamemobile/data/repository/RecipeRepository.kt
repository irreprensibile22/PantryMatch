package com.example.esamemobile.data.repository

import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.util.IngredientMatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * CRUD sulle ricette (collezione `recipes`) e ricerca per ingredienti.
 *
 * La ricerca pre-filtra lato server con `whereArrayContainsAny` sul campo
 * denormalizzato `ingredientNames` (max 30 valori), poi rifinisce il match
 * lato client con [IngredientMatcher] per applicare il vincolo sui mancanti.
 */
class RecipeRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun recipes() = db.collection(RECIPES)

    suspend fun getRecipe(id: String): Recipe? =
        recipes().document(id).get().await().toObject(Recipe::class.java)

    suspend fun getMyRecipes(): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", requireUid())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Recipe::class.java)

    suspend fun saveRecipe(recipe: Recipe): String {
        val withMeta = recipe.copy(
            authorId = requireUid(),
            ingredientNames = IngredientMatcher.ingredientNamesFor(recipe),
        )
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

    /**
     * Cerca le ricette pubbliche realizzabili con l'inventario [pantry],
     * ammettendo fino a [maxMissing] ingredienti mancanti.
     *
     * @param authorIds se non null, limita la ricerca a questi autori
     *                  (es. la lista degli amici).
     */
    suspend fun searchByPantry(
        pantry: List<PantryItem>,
        maxMissing: Int,
        authorIds: List<String>? = null,
    ): List<RecipeMatch> {
        val owned = pantry.map { IngredientMatcher.normalize(it.name) }
        if (owned.isEmpty()) return emptyList()

        // Firestore limita arrayContainsAny a 30 valori
        var query: Query = recipes()
            .whereEqualTo("isPublic", true)
            .whereArrayContainsAny("ingredientNames", owned.take(30))

        if (authorIds != null) {
            if (authorIds.isEmpty()) return emptyList()
            query = query.whereIn("authorId", authorIds.take(30))
        }

        val candidates = query.get().await().toObjects(Recipe::class.java)
        return IngredientMatcher.match(pantry, candidates, maxMissing)
    }

    private fun requireUid(): String =
        requireNotNull(auth.currentUser?.uid) { "Utente non autenticato" }

    companion object {
        private const val RECIPES = "recipes"
    }
}
