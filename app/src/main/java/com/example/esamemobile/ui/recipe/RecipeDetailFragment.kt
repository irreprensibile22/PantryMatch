package com.example.esamemobile.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.esamemobile.R
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.databinding.FragmentRecipeDetailBinding
import com.example.esamemobile.util.Resource
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val recipeId: String
        get() = arguments?.getString(ARG_RECIPE_ID).orEmpty()
    private val viewModel: RecipeDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipe.collect { state ->
                    binding.progress.visibility =
                        if (state is Resource.Loading) View.VISIBLE else View.GONE
                    if (state is Resource.Success) bind(state.data)
                }
            }
        }
        viewModel.load(recipeId)
    }

    private fun bind(recipe: Recipe) {
        binding.textTitle.text = recipe.title
        binding.textDescription.text = recipe.description
        binding.textIngredients.text = recipe.ingredients.joinToString("\n") { ing ->
            "• " + listOfNotNull(
                ing.name,
                ing.quantity?.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else q.toString() },
                ing.unit,
            ).joinToString(" ")
        }
        binding.textSteps.text = recipe.steps
            .mapIndexed { i, step -> "${i + 1}. $step" }
            .joinToString("\n\n")
        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_recipe_placeholder)
            .into(binding.imageRecipe)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Chiave del bundle per l'id ricetta (vedi <argument> in nav_graph). */
        const val ARG_RECIPE_ID = "recipeId"
    }
}
