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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.esamemobile.R
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.FragmentProfileBinding
import com.example.esamemobile.ui.recipe.RecipeDetailFragment
import com.example.esamemobile.ui.recipe.RecipeEditFragment
import com.example.esamemobile.ui.search.RecipeMatchAdapter
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Profilo dell'utente: dati base, "Le mie ricette" (scorrimento orizzontale, con
 * ricerca testuale e "Vedi tutte"), il ricettario "Preferiti" (con ricerca) e
 * logout.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()
    private val viewModel: ProfileViewModel by viewModels()
    private val adapter = RecipeAdapter(
        onClick = { openDetail(it.id) },
        onEdit = { openEdit(it) },
        onDelete = { confirmDelete(it) },
        horizontal = true,
    )
    private val favoritesAdapter = RecipeMatchAdapter(
        onClick = { openDetail(it.recipe.id) },
        onToggleFavorite = { match, favorite ->
            if (!favorite) viewModel.removeFavorite(match.recipe.id)
        },
    )

    // Ultimi dati caricati, per applicare la ricerca testuale lato client.
    private var allMyRecipes: List<Recipe> = emptyList()
    private var allFavorites: List<RecipeMatch> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = runCatching { authRepository.currentUser }.getOrNull()
        binding.textName.text = user?.displayName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_guest)
        binding.textEmail.text = user?.email ?: "—"

        binding.recyclerRecipes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerRecipes.adapter = adapter
        binding.recyclerFavorites.adapter = favoritesAdapter

        binding.fabAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit)
        }
        binding.buttonSeeAllMyRecipes.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_recipes_grid)
        }
        binding.buttonSearchMyRecipes.setOnClickListener {
            toggleSearch(binding.layoutSearchMyRecipes, binding.editSearchMyRecipes)
        }
        binding.editSearchMyRecipes.doAfterTextChanged { applyMyRecipesFilter() }
        binding.buttonSearchFavorites.setOnClickListener {
            toggleSearch(binding.layoutSearchFavorites, binding.editSearchFavorites)
        }
        binding.editSearchFavorites.doAfterTextChanged { applyFavoritesFilter() }

        binding.buttonLogout.setOnClickListener {
            runCatching { authRepository.logout() }
            findNavController().navigate(R.id.action_profile_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.myRecipes.collect { renderMyRecipes(it) } }
                launch { viewModel.favorites.collect { renderFavorites(it) } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica al rientro (es. dopo aver creato una ricetta o aggiunto un preferito)
        viewModel.loadMyRecipes()
        viewModel.loadFavorites()
    }

    /** Mostra/nasconde il campo di ricerca; quando si nasconde azzera il filtro. */
    private fun toggleSearch(layout: View, editText: android.widget.EditText) {
        if (layout.visibility == View.VISIBLE) {
            editText.text?.clear()
            layout.visibility = View.GONE
        } else {
            layout.visibility = View.VISIBLE
            editText.requestFocus()
        }
    }

    private fun renderMyRecipes(state: Resource<List<Recipe>>) {
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                allMyRecipes = state.data
                applyMyRecipesFilter()
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun applyMyRecipesFilter() {
        val query = binding.editSearchMyRecipes.text?.toString()?.trim().orEmpty()
        val filtered =
            if (query.isEmpty()) allMyRecipes
            else allMyRecipes.filter { it.title.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
        updateEmpty(binding.textEmpty, filtered.isEmpty(), query, R.string.profile_no_recipes)
    }

    private fun renderFavorites(state: Resource<List<RecipeMatch>>) {
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                allFavorites = state.data
                // Nel ricettario sono tutte preferite: cuori pieni.
                favoritesAdapter.favoriteIds = state.data.map { it.recipe.id }.toSet()
                applyFavoritesFilter()
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun applyFavoritesFilter() {
        val query = binding.editSearchFavorites.text?.toString()?.trim().orEmpty()
        val filtered =
            if (query.isEmpty()) allFavorites
            else allFavorites.filter { it.recipe.title.contains(query, ignoreCase = true) }
        favoritesAdapter.submitList(filtered)
        updateEmpty(binding.textFavoritesEmpty, filtered.isEmpty(), query, R.string.profile_no_favorites)
    }

    private fun updateEmpty(view: android.widget.TextView, isEmpty: Boolean, query: String, emptyRes: Int) {
        view.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            view.text = getString(if (query.isEmpty()) emptyRes else R.string.search_no_results)
        }
    }

    private fun openDetail(recipeId: String) {
        findNavController().navigate(
            R.id.action_profile_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to recipeId),
        )
    }

    private fun openEdit(recipe: Recipe) {
        findNavController().navigate(
            R.id.action_profile_to_edit,
            bundleOf(RecipeEditFragment.ARG_RECIPE_ID to recipe.id),
        )
    }

    private fun confirmDelete(recipe: Recipe) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_delete_title)
            .setMessage(R.string.detail_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.detail_delete) { _, _ -> viewModel.deleteRecipe(recipe.id) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerRecipes.adapter = null
        binding.recyclerFavorites.adapter = null
        _binding = null
    }
}
