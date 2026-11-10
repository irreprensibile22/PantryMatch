package com.example.pantrymatch.util

import com.example.pantrymatch.data.model.RecipeIngredient

// Text <-> list conversions for the recipe editor (one line = one ingredient/step).
object RecipeFormParser {

    // line format: "name : quantity unit" (quantity and unit optional)
    fun parseIngredients(text: String): List<RecipeIngredient> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val parts = line.split(":", limit = 2)
                val name = parts[0].trim()
                if (parts.size < 2 || parts[1].isBlank()) {
                    RecipeIngredient(name = name)
                } else {
                    val tokens = parts[1].trim().split(Regex("\\s+"), limit = 2)
                    // accept both comma and dot as decimal separator
                    val quantity = tokens[0].replace(',', '.').toDoubleOrNull()
                    val unit = tokens.getOrNull(1)?.takeIf { it.isNotBlank() }
                    if (quantity == null) {
                        RecipeIngredient(name = name, unit = parts[1].trim())
                    } else {
                        RecipeIngredient(name = name, quantity = quantity, unit = unit)
                    }
                }
            }
            .toList()

    fun parseSteps(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    fun ingredientsToText(ingredients: List<RecipeIngredient>): String =
        ingredients.joinToString("\n") { ing ->
            // show whole quantities without ".0" (e.g. 2 instead of 2.0)
            val qty = ing.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
            val tail = listOfNotNull(qty, ing.unit).joinToString(" ")
            if (tail.isBlank()) ing.name else "${ing.name} : $tail"
        }

    fun stepsToText(steps: List<String>): String = steps.joinToString("\n")
}
