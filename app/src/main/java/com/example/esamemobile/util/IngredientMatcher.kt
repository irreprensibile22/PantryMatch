package com.example.esamemobile.util

import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeMatch

// Confronta la dispensa dell'utente con le ricette e calcola quanti ingredienti mancano.
object IngredientMatcher {

    // plurali/varianti piu comuni ricondotti al singolare, cosi "uova" e "uovo" combaciano
    private val SYNONYMS = mapOf(
        "pomodori" to "pomodoro",
        "uova" to "uovo",
        "cipolle" to "cipolla",
        "patate" to "patata",
        "carote" to "carota",
        "zucchine" to "zucchina",
        "peperoni" to "peperone",
    )

    // tutto minuscolo, spazi compattati e sinonimo applicato: forma canonica per il confronto
    fun normalize(raw: String): String {
        val cleaned = raw.trim().lowercase().replace(Regex("\\s+"), " ")
        return SYNONYMS[cleaned] ?: cleaned
    }

    fun match(
        pantry: List<PantryItem>,
        recipes: List<Recipe>,
        maxMissing: Int = 2,
    ): List<RecipeMatch> =
        annotate(pantry, recipes)
            .filter { it.missingCount <= maxMissing }
            // prima le ricette con meno ingredienti mancanti, a parita di mancanti le piu veloci
            .sortedWith(
                compareBy({ it.missingCount }, { it.recipe.prepTimeMin })
            )

    fun annotate(
        pantry: List<PantryItem>,
        recipes: List<Recipe>,
    ): List<RecipeMatch> {
        val owned = pantry.map { normalize(it.name) }.toHashSet()
        return recipes.map { recipe ->
            // gli ingredienti opzionali non contano come "mancanti"
            val missing = recipe.ingredients
                .filterNot { it.optional }
                .map { normalize(it.name) }
                .distinct()
                .filterNot { owned.contains(it) }
            RecipeMatch(recipe, missing)
        }
    }

    fun ingredientNamesFor(recipe: Recipe): List<String> =
        recipe.ingredients.map { normalize(it.name) }.distinct()
}
