package com.example.pantrymatch.util

import com.example.pantrymatch.data.model.PantryItem
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.data.model.RecipeMatch

// Compares the user's pantry with recipes and counts how many ingredients are missing.
object IngredientMatcher {

    // common plurals/variants mapped to the singular, so "uova" and "uovo" match
    private val SYNONYMS = mapOf(
        "pomodori" to "pomodoro",
        "uova" to "uovo",
        "cipolle" to "cipolla",
        "patate" to "patata",
        "carote" to "carota",
        "zucchine" to "zucchina",
        "peperoni" to "peperone",
    )

    // lowercase, collapsed spaces and synonym applied: canonical form for comparison
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
            // recipes with fewer missing ingredients first; ties broken by shortest time
            .sortedWith(
                compareBy({ it.missingCount }, { it.recipe.prepTimeMin })
            )

    fun annotate(
        pantry: List<PantryItem>,
        recipes: List<Recipe>,
    ): List<RecipeMatch> {
        val owned = pantry.map { normalize(it.name) }.toHashSet()
        return recipes.map { recipe ->
            // optional ingredients do not count as "missing"
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
