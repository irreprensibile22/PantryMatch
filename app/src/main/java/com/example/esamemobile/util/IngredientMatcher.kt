package com.example.esamemobile.util

import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeMatch

/**
 * Cuore funzionale dell'app: confronta l'inventario dell'utente con un insieme
 * di ricette e produce i [RecipeMatch] ordinati per numero di ingredienti
 * mancanti.
 *
 * È volutamente Kotlin puro (nessuna dipendenza da Android o Firebase) così da
 * poter essere coperto da test unitari (vedi IngredientMatcherTest).
 */
object IngredientMatcher {

    /** Alias/sinonimi: tutte le forme a sinistra vengono ricondotte a destra. */
    private val SYNONYMS = mapOf(
        "pomodori" to "pomodoro",
        "uova" to "uovo",
        "cipolle" to "cipolla",
        "patate" to "patata",
        "carote" to "carota",
        "zucchine" to "zucchina",
        "peperoni" to "peperone",
    )

    /**
     * Normalizza il nome di un ingrediente per il confronto:
     * minuscolo, trim, spazi compattati e risoluzione dei sinonimi.
     */
    fun normalize(raw: String): String {
        val cleaned = raw.trim().lowercase().replace(Regex("\\s+"), " ")
        return SYNONYMS[cleaned] ?: cleaned
    }

    /**
     * Calcola i match per una lista di ricette dato l'inventario.
     *
     * @param pantry        ingredienti posseduti dall'utente
     * @param recipes       ricette candidate
     * @param maxMissing    numero massimo di ingredienti mancanti ammessi
     *                      (0 = solo preparabili; 2 = fino a 2 mancanti)
     * @return match che rispettano il vincolo, ordinati per mancanti crescenti
     *         e, a parità, per tempo di preparazione.
     */
    fun match(
        pantry: List<PantryItem>,
        recipes: List<Recipe>,
        maxMissing: Int = 2,
    ): List<RecipeMatch> {
        val owned = pantry.map { normalize(it.name) }.toHashSet()

        return recipes
            .map { recipe ->
                val missing = recipe.ingredients
                    .filterNot { it.optional }
                    .map { normalize(it.name) }
                    .distinct()
                    .filterNot { owned.contains(it) }
                RecipeMatch(recipe, missing)
            }
            .filter { it.missingCount <= maxMissing }
            .sortedWith(
                compareBy({ it.missingCount }, { it.recipe.prepTimeMin })
            )
    }

    /**
     * Costruisce la lista denormalizzata di nomi ingredienti (normalizzati e
     * senza duplicati) da salvare nel campo `ingredientNames` della ricetta,
     * usato per le query Firestore.
     */
    fun ingredientNamesFor(recipe: Recipe): List<String> =
        recipe.ingredients.map { normalize(it.name) }.distinct()
}
