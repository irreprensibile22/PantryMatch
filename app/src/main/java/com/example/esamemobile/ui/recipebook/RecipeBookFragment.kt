package com.example.esamemobile.ui.recipebook

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
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.FragmentRecipebookBinding
import com.example.esamemobile.ui.profile.RecipeAdapter
import com.example.esamemobile.ui.profile.RecipesGridFragment
import com.example.esamemobile.ui.recipe.RecipeDetailFragment
import com.example.esamemobile.ui.recipe.RecipeEditFragment
import com.example.esamemobile.ui.search.RecipeMatchAdapter
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Ricettario dell'utente con due tab: "Le mie ricette" (card con azioni
 * modifica/elimina + FAB di creazione) e "Salvati" (i preferiti dalla community).
 * Separato dalla schermata Profilo.
 */
class RecipeBookFragment : Fragment() {

    private var _binding: FragmentRecipebookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeBookViewModel by viewModels()
    private val adapter = RecipeAdapter(
        onClick = { openDetail(it.id) },
        onEdit = { openEdit(it) },
        onDelete = { confirmDelete(it) },
    )
    private val favoritesAdapter = RecipeMatchAdapter(
        onClick = { openDetail(it.recipe.id) },
        onToggleFavorite = { match, favorite ->
            if (!favorite) viewModel.removeFavorite(match.recipe.id)
        },
    )

    // Dati completi: la ricerca testuale filtra lato client il tab attivo.
    private var allMyRecipes: List<Recipe> = emptyList()
    private var allFavorites: List<RecipeMatch> = emptyList()
    private var myCount = 0
    private var savedCount = 0
    private var hasQuery = false
    private val isMineTab get() = binding.tabRecipeBook.selectedTabPosition == 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecipebookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerRecipes.adapter = adapter
        binding.recyclerFavorites.adapter = favoritesAdapter

        val name = runCatching { AuthRepository().currentUser?.displayName }.getOrNull()
        binding.textProfileInitial.text = name?.trim()?.firstOrNull()?.uppercase() ?: "?"

        binding.fabAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_recipebook_to_edit)
        }
        binding.buttonSeeAll.setOnClickListener {
            val mode = if (isMineTab) "mine" else RecipesGridFragment.MODE_SAVED
            findNavController().navigate(
                R.id.action_recipebook_to_recipes_grid,
                bundleOf(RecipesGridFragment.ARG_MODE to mode),
            )
        }
        binding.editSearch.doAfterTextChanged { applyFilter() }

        binding.tabRecipeBook.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = updateTab()
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.myRecipes.collect { renderMyRecipes(it) } }
                launch { viewModel.favorites.collect { renderFavorites(it) } }
            }
        }
        updateTab()
    }

    override fun onResume() {
        super.onResume()
        // Ricarica al rientro (es. dopo aver creato una ricetta o aggiunto un preferito)
        viewModel.loadMyRecipes()
        viewModel.loadFavorites()
    }

    /** Applica la ricerca testuale (per titolo) a entrambe le liste. */
    private fun applyFilter() {
        val q = binding.editSearch.text?.toString()?.trim().orEmpty()
        hasQuery = q.isNotEmpty()
        val mine = if (q.isEmpty()) allMyRecipes
        else allMyRecipes.filter { it.title.contains(q, ignoreCase = true) }
        val saved = if (q.isEmpty()) allFavorites
        else allFavorites.filter { it.recipe.title.contains(q, ignoreCase = true) }
        myCount = mine.size
        savedCount = saved.size
        adapter.submitList(mine)
        favoritesAdapter.submitList(saved)
        updateTab()
    }

    /** Mostra la lista del tab attivo, aggiorna sottotitolo, FAB e stati vuoti. */
    private fun updateTab() {
        val mine = isMineTab
        binding.textSubtitle.setText(
            if (mine) R.string.recipe_book_subtitle_mine else R.string.recipe_book_subtitle_saved
        )
        binding.fabAddRecipe.visibility = if (mine) View.VISIBLE else View.GONE

        binding.recyclerRecipes.visibility = if (mine && myCount > 0) View.VISIBLE else View.GONE
        binding.recyclerFavorites.visibility =
            if (!mine && savedCount > 0) View.VISIBLE else View.GONE

        val emptyRes = if (hasQuery) R.string.search_no_results
        else if (mine) R.string.profile_no_recipes else R.string.profile_no_favorites
        binding.textEmpty.visibility = if (mine && myCount == 0) View.VISIBLE else View.GONE
        binding.textFavoritesEmpty.visibility =
            if (!mine && savedCount == 0) View.VISIBLE else View.GONE
        if (mine) binding.textEmpty.setText(emptyRes) else binding.textFavoritesEmpty.setText(emptyRes)
    }

    private fun renderMyRecipes(state: Resource<List<Recipe>>) {
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                allMyRecipes = state.data
                applyFilter()
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderFavorites(state: Resource<List<RecipeMatch>>) {
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                allFavorites = state.data
                // Nel ricettario sono tutte preferite: cuori pieni.
                favoritesAdapter.favoriteIds = state.data.map { it.recipe.id }.toSet()
                applyFilter()
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openDetail(recipeId: String) {
        findNavController().navigate(
            R.id.action_recipebook_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to recipeId),
        )
    }

    private fun openEdit(recipe: Recipe) {
        findNavController().navigate(
            R.id.action_recipebook_to_edit,
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
