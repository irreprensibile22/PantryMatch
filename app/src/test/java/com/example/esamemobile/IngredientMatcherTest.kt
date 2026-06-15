package com.example.esamemobile

import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeIngredient
import com.example.esamemobile.util.IngredientMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientMatcherTest {

    private fun pantry(vararg names: String) =
        names.map { PantryItem(name = it) }

    private fun recipe(
        title: String,
        prepTimeMin: Int = 10,
        ingredients: List<RecipeIngredient>,
    ) = Recipe(title = title, prepTimeMin = prepTimeMin, ingredients = ingredients)

    private fun ing(name: String, optional: Boolean = false) =
        RecipeIngredient(name = name, optional = optional)

    @Test
    fun `normalize gestisce maiuscole spazi e sinonimi`() {
        assertEquals("pomodoro", IngredientMatcher.normalize("  Pomodori "))
        assertEquals("uovo", IngredientMatcher.normalize("UOVA"))
        assertEquals("farina", IngredientMatcher.normalize("Farina"))
    }

    @Test
    fun `ricetta con tutti gli ingredienti e preparabile`() {
        val pancake = recipe(
            "Pancake",
            ingredients = listOf(ing("Uova"), ing("Farina"), ing("Latte")),
        )
        val result = IngredientMatcher.match(
            pantry = pantry("uovo", "farina", "latte"),
            recipes = listOf(pancake),
            maxMissing = 2,
        )
        assertEquals(1, result.size)
        assertTrue(result[0].isReady)
        assertEquals(0, result[0].missingCount)
    }

    @Test
    fun `ingredienti opzionali non contano come mancanti`() {
        val r = recipe(
            "Insalata",
            ingredients = listOf(ing("Lattuga"), ing("Olio", optional = true)),
        )
        val result = IngredientMatcher.match(pantry("lattuga"), listOf(r), maxMissing = 0)
        assertEquals(1, result.size)
        assertTrue(result[0].isReady)
    }

    @Test
    fun `il filtro maxMissing esclude le ricette con troppi mancanti`() {
        val r = recipe(
            "Torta",
            ingredients = listOf(ing("Farina"), ing("Zucchero"), ing("Uova"), ing("Burro")),
        )
        // In dispensa solo farina -> mancano 3 ingredienti
        val conLimite2 = IngredientMatcher.match(pantry("farina"), listOf(r), maxMissing = 2)
        assertTrue(conLimite2.isEmpty())

        val conLimite3 = IngredientMatcher.match(pantry("farina"), listOf(r), maxMissing = 3)
        assertEquals(1, conLimite3.size)
        assertEquals(3, conLimite3[0].missingCount)
    }

    @Test
    fun `i risultati sono ordinati per mancanti crescenti poi per tempo`() {
        val pronta = recipe("Pronta", prepTimeMin = 30, ingredients = listOf(ing("Pane")))
        val unoMancante = recipe("UnoMancante", prepTimeMin = 5, ingredients = listOf(ing("Pane"), ing("Burro")))
        val veloce = recipe("Veloce", prepTimeMin = 5, ingredients = listOf(ing("Pane")))

        val result = IngredientMatcher.match(
            pantry = pantry("pane"),
            recipes = listOf(pronta, unoMancante, veloce),
            maxMissing = 2,
        )
        // Prima le preparabili (0 mancanti), tra queste prima la più veloce
        assertEquals("Veloce", result[0].recipe.title)
        assertEquals("Pronta", result[1].recipe.title)
        assertEquals("UnoMancante", result[2].recipe.title)
        assertEquals(listOf("burro"), result[2].missingIngredients)
    }

    @Test
    fun `ingredienti duplicati nella ricetta contano una volta sola`() {
        val r = recipe(
            "Doppio",
            ingredients = listOf(ing("Pomodori"), ing("pomodoro"), ing("Basilico")),
        )
        val result = IngredientMatcher.match(pantry("pomodoro"), listOf(r), maxMissing = 2)
        assertEquals(1, result.size)
        assertEquals(listOf("basilico"), result[0].missingIngredients)
        assertFalse(result[0].isReady)
    }
}
