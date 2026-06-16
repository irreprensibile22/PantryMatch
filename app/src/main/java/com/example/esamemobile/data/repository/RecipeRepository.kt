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
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private fun recipes() = db.collection(RECIPES)

    suspend fun getRecipe(id: String): Recipe? =
        recipes().document(id).get().await().toObject(Recipe::class.java)

    /**
     * Carica le ricette con gli [ids] indicati, **preservandone l'ordine** e
     * scartando quelle non più esistenti. Usato per il ricettario dei preferiti.
     */
    suspend fun getRecipesByIds(ids: List<String>): List<Recipe> =
        ids.mapNotNull { getRecipe(it) }

    suspend fun getMyRecipes(): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", requireUid())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Recipe::class.java)

    /**
     * Ricette **pubbliche** di un autore, dalla più recente.
     *
     * Il filtro `isPublic == true` è **server-side** (non solo client): le
     * Security Rules consentono a un altro utente di leggere una ricetta solo se
     * pubblica, perciò una query che non vincola `isPublic` verrebbe rifiutata
     * con *permission denied*. Due filtri di sola uguaglianza non richiedono un
     * indice composito; l'ordinamento per data resta lato client.
     */
    suspend fun getPublicRecipesByAuthor(authorId: String): List<Recipe> =
        recipes()
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("isPublic", true)
            .get().await()
            .toObjects(Recipe::class.java)
            .sortedByDescending { it.createdAt }

    suspend fun saveRecipe(recipe: Recipe): String {
        val withMeta = recipe.copy(
            authorId = requireUid(),
            authorName = auth.currentUser?.displayName.orEmpty(),
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
     * Carica un'immagine di ricetta su Firebase Storage e restituisce l'URL di
     * download pubblico, da salvare nel campo `imageUrl` della ricetta.
     */
    suspend fun uploadImage(localUri: Uri): String {
        val ref = storage.reference
            .child("recipe_images")
            .child(requireUid())
            .child("${System.currentTimeMillis()}.jpg")
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Feed della Home: tutte le ricette pubbliche, ordinate dalla più recente.
     *
     * L'ordinamento per `createdAt` è applicato lato client per non richiedere
     * un ulteriore indice composito `isPublic` + `createdAt` su Firestore.
     *
     * @param authorIds se non null, limita agli autori indicati (es. amici).
     */
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
        if (authorIds != null && authorIds.isEmpty()) return emptyList()

        // Pre-filtro server-side sugli ingredienti (arrayContainsAny, max 30 valori).
        // Il filtro per autore (amici) è applicato lato client: Firestore non
        // permette di combinare arrayContainsAny e whereIn nella stessa query.
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
