package com.example.esamemobile.util

import com.example.esamemobile.data.model.RecipeIngredient

/**
 * Converte il testo libero del form di creazione ricetta in liste strutturate e
 * viceversa, così da poter inserire ingredienti e passaggi con semplici campi
 * multilinea (uno per riga).
 *
 * Formato di un ingrediente:  `nome` oppure `nome : quantità unità`
 * Esempi: "Farina : 200 g", "Uovo : 2", "Sale".
 */
object RecipeFormParser {

    /** Una riga di ingrediente -> [RecipeIngredient]; righe vuote ignorate. */
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
                    val quantity = tokens[0].replace(',', '.').toDoubleOrNull()
                    val unit = tokens.getOrNull(1)?.takeIf { it.isNotBlank() }
                    if (quantity == null) {
                        // nessun numero valido: tratta tutto come unità/nota
                        RecipeIngredient(name = name, unit = parts[1].trim())
                    } else {
                        RecipeIngredient(name = name, quantity = quantity, unit = unit)
                    }
                }
            }
            .toList()

    /** Un passaggio per riga; righe vuote ignorate. */
    fun parseSteps(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    /** Serializza gli ingredienti nel formato testuale del form (per la modifica). */
    fun ingredientsToText(ingredients: List<RecipeIngredient>): String =
        ingredients.joinToString("\n") { ing ->
            val qty = ing.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
            val tail = listOfNotNull(qty, ing.unit).joinToString(" ")
            if (tail.isBlank()) ing.name else "${ing.name} : $tail"
        }

    fun stepsToText(steps: List<String>): String = steps.joinToString("\n")
}
