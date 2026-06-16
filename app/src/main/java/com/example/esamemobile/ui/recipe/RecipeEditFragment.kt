package com.example.esamemobile.ui.recipe

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.esamemobile.databinding.FragmentRecipeEditBinding
import com.example.esamemobile.util.RecipeFormParser
import com.example.esamemobile.util.Resource
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
        binding.buttonSave.setOnClickListener { save() }

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
        binding.editCategory.setText(recipe.category)
        binding.editPrepTime.setText(if (recipe.prepTimeMin > 0) recipe.prepTimeMin.toString() else "")
        binding.editIngredients.setText(RecipeFormParser.ingredientsToText(recipe.ingredients))
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
        val ingredients = RecipeFormParser.parseIngredients(binding.editIngredients.text?.toString().orEmpty())
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
            ingredients = ingredients,
            steps = RecipeFormParser.parseSteps(binding.editSteps.text?.toString().orEmpty()),
            isPublic = binding.switchPublic.isChecked,
        )
        viewModel.save(recipe, pickedImage)
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
