package com.example.esamemobile.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.esamemobile.R
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.databinding.FragmentRecipesGridBinding
import com.example.esamemobile.ui.recipe.RecipeDetailFragment
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Griglia con tutte le ricette dell'utente + ricerca testuale ("Vedi tutte"). */
class RecipesGridFragment : Fragment() {

    private var _binding: FragmentRecipesGridBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipesGridViewModel by viewModels()
    private val adapter = RecipeGridAdapter(onClick = { openDetail(it) })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecipesGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerGrid.adapter = adapter

        binding.editSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { render(it) }
            }
        }
        viewModel.load()
    }

    private fun render(state: Resource<List<Recipe>>) {
        binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                adapter.submitList(state.data)
                binding.textEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openDetail(recipe: Recipe) {
        findNavController().navigate(
            R.id.action_recipes_grid_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to recipe.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerGrid.adapter = null
        _binding = null
    }
}
