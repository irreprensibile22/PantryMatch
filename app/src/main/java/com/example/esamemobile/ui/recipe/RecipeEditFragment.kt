package com.example.esamemobile.ui.recipe

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.esamemobile.R
import com.example.esamemobile.data.model.Difficulty
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeIngredient
import com.example.esamemobile.databinding.DialogAddIngredientBinding
import com.example.esamemobile.databinding.FragmentRecipeEditBinding
import com.example.esamemobile.util.RecipeFormParser
import com.example.esamemobile.util.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Creazione o modifica di una ricetta. Se riceve [ARG_RECIPE_ID] entra in
 * modalità modifica e pre-compila il form con la ricetta esistente.
 */
class RecipeEditFragment : Fragment() {

    private var _binding: FragmentRecipeEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeEditViewModel by viewModels()

    private val recipeId: String
        get() = arguments?.getString(ARG_RECIPE_ID).orEmpty()

    /** Ricetta esistente in modifica (per conservare id, imageUrl e createdAt). */
    private var editing: Recipe? = null
    private var pickedImage: Uri? = null

    /** Ingredienti aggiunti, gestiti come lista strutturata (chip rimovibili). */
    private val ingredients = mutableListOf<RecipeIngredient>()

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImage = uri
            Glide.with(this).load(uri).into(binding.imageRecipe)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecipeEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonPickImage.setOnClickListener {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.buttonAddIngredient.setOnClickListener { showIngredientDialog() }
        binding.buttonSave.setOnClickListener { save() }
        renderIngredients()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.loaded.collect { it?.let(::onLoaded) } }
                launch { viewModel.saveState.collect { it?.let(::onSaveState) } }
            }
        }
        viewModel.load(recipeId)
    }

    private fun onLoaded(state: Resource<Recipe>) {
        when (state) {
            is Resource.Success -> prefill(state.data)
            is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            is Resource.Loading -> Unit
        }
    }

    private fun prefill(recipe: Recipe) {
        editing = recipe
        binding.editTitle.setText(recipe.title)
        binding.editDescription.setText(recipe.description)
        binding.editCategory.setText(recipe.category, false)
        binding.editPrepTime.setText(if (recipe.prepTimeMin > 0) recipe.prepTimeMin.toString() else "")
        ingredients.clear()
        ingredients.addAll(recipe.ingredients)
        renderIngredients()
        binding.editSteps.setText(RecipeFormParser.stepsToText(recipe.steps))
        binding.switchPublic.isChecked = recipe.isPublic
        when (runCatching { Difficulty.valueOf(recipe.difficulty) }.getOrNull()) {
            Difficulty.MEDIUM -> binding.chipMedium.isChecked = true
            Difficulty.HARD -> binding.chipHard.isChecked = true
            else -> binding.chipEasy.isChecked = true
        }
        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_recipe_placeholder)
            .into(binding.imageRecipe)
    }

    private fun save() {
        val title = binding.editTitle.text?.toString().orEmpty().trim()
        if (title.isEmpty() || ingredients.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.recipe_invalid), Snackbar.LENGTH_LONG).show()
            return
        }
        val difficulty = when (binding.chipGroupDifficulty.checkedChipId) {
            binding.chipMedium.id -> Difficulty.MEDIUM
            binding.chipHard.id -> Difficulty.HARD
            else -> Difficulty.EASY
        }
        val base = editing ?: Recipe()
        val recipe = base.copy(
            title = title,
            description = binding.editDescription.text?.toString().orEmpty().trim(),
            category = binding.editCategory.text?.toString().orEmpty().trim(),
            prepTimeMin = binding.editPrepTime.text?.toString()?.toIntOrNull() ?: 0,
            difficulty = difficulty.name,
            ingredients = ingredients.toList(),
            steps = RecipeFormParser.parseSteps(binding.editSteps.text?.toString().orEmpty()),
            isPublic = binding.switchPublic.isChecked,
        )
        viewModel.save(recipe, pickedImage)
    }

    /** Ridisegna i chip degli ingredienti aggiunti (tap = modifica, X = rimuovi). */
    private fun renderIngredients() {
        binding.chipGroupIngredients.removeAllViews()
        ingredients.forEachIndexed { index, ing ->
            val chip = Chip(requireContext()).apply {
                text = formatIngredient(ing)
                isCloseIconVisible = true
                setOnClickListener { showIngredientDialog(index) }
                setOnCloseIconClickListener {
                    ingredients.removeAt(index)
                    renderIngredients()
                }
            }
            binding.chipGroupIngredients.addView(chip)
        }
        binding.textNoIngredients.visibility =
            if (ingredients.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun formatIngredient(ing: RecipeIngredient): String {
        val qty = ing.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
        val tail = listOfNotNull(qty, ing.unit).joinToString(" ")
        return if (tail.isBlank()) ing.name else "${ing.name} $tail"
    }

    /**
     * Dialog condiviso (stesso della Dispensa) per aggiungere un ingrediente
     * ([editIndex] null) o modificarne uno esistente. Il nome usa il catalogo a
     * tendina (`ingredient_catalog`) per uniformare gli ingredienti.
     */
    private fun showIngredientDialog(editIndex: Int? = null) {
        val dialogBinding = DialogAddIngredientBinding.inflate(layoutInflater)
        val noUnit = getString(R.string.unit_none)
        dialogBinding.editUnit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.pantry_units),
            )
        )
        val existing = editIndex?.let { ingredients[it] }
        dialogBinding.editUnit.setText(existing?.unit?.takeIf { it.isNotBlank() } ?: noUnit, false)
        existing?.let {
            dialogBinding.editName.setText(it.name, false)
            dialogBinding.editQuantity.setText(
                it.quantity?.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else q.toString() }.orEmpty()
            )
        }
        val isEdit = existing != null
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (isEdit) R.string.ingredient_dialog_edit_title
                else R.string.ingredient_dialog_add_title
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                if (isEdit) R.string.ingredient_dialog_save else R.string.ingredient_dialog_add
            ) { _, _ ->
                val name = dialogBinding.editName.text?.toString().orEmpty().trim()
                if (name.isEmpty()) return@setPositiveButton
                val quantity = dialogBinding.editQuantity.text?.toString()
                    ?.replace(',', '.')?.toDoubleOrNull()
                val unit = dialogBinding.editUnit.text?.toString()
                    ?.takeIf { it.isNotBlank() && it != noUnit }
                val ingredient = RecipeIngredient(name = name, quantity = quantity, unit = unit)
                if (editIndex != null) ingredients[editIndex] = ingredient
                else ingredients.add(ingredient)
                renderIngredients()
            }
            .show()
    }

    private fun onSaveState(state: Resource<RecipeEditViewModel.SaveOutcome>) {
        binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
        binding.buttonSave.isEnabled = state !is Resource.Loading
        when (state) {
            is Resource.Success -> {
                val msg = if (state.data.photoSkipped) {
                    getString(R.string.recipe_saved_no_photo)
                } else {
                    getString(R.string.recipe_saved)
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            is Resource.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            is Resource.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_RECIPE_ID = "recipeId"
    }
}
