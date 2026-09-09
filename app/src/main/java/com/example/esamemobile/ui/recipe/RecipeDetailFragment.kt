package com.example.esamemobile.ui.recipe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
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
import com.example.esamemobile.databinding.FragmentRecipeDetailBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// Dettaglio ricetta con azioni in action bar: preferiti, condivisione ed edita/elimina (solo autore).
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
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipe.collect { state ->
                        binding.progress.visibility =
                            if (state is Resource.Loading) View.VISIBLE else View.GONE
                        if (state is Resource.Success) bind(state.data)
                        if (state is Resource.Error)
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        requireActivity().invalidateOptionsMenu()
                    }
                }
                launch {
                    viewModel.isFavorite.collect { requireActivity().invalidateOptionsMenu() }
                }
                launch {
                    viewModel.deleteState.collect { it?.let(::onDeleteState) }
                }
            }
        }
        viewModel.load(recipeId)
    }

    private fun bind(recipe: Recipe) {
        binding.textTitle.text = recipe.title
        binding.textAuthor.text = recipe.authorName
            .takeIf { it.isNotBlank() }
            ?.let { getString(R.string.by_author, it) }
            .orEmpty()
        binding.textAuthor.visibility =
            if (binding.textAuthor.text.isNullOrBlank()) View.GONE else View.VISIBLE
        bindMeta(recipe)
        binding.textDescription.text = recipe.description
        binding.textDescription.visibility =
            if (recipe.description.isBlank()) View.GONE else View.VISIBLE
        binding.textIngredients.text =
            recipe.ingredients.joinToString("\n") { "• " + formatIngredient(it) }
        binding.textSteps.text = recipe.steps
            .mapIndexed { i, step -> "${i + 1}. $step" }
            .joinToString("\n\n")
        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_recipe_placeholder)
            .into(binding.imageRecipe)
    }

    private fun bindMeta(recipe: Recipe) {
        fun bindPill(view: android.widget.TextView, text: String?) {
            if (text.isNullOrBlank()) {
                view.visibility = View.GONE
            } else {
                view.text = text
                view.visibility = View.VISIBLE
            }
        }
        bindPill(binding.textCategory, recipe.category)
        bindPill(
            binding.textPrepTime,
            recipe.prepTimeMin.takeIf { it > 0 }?.let { getString(R.string.prep_time_format, it) },
        )
        val difficultyRes = when (runCatching { Difficulty.valueOf(recipe.difficulty) }.getOrNull()) {
            Difficulty.MEDIUM -> R.string.difficulty_medium
            Difficulty.HARD -> R.string.difficulty_hard
            Difficulty.EASY -> R.string.difficulty_easy
            null -> null
        }
        bindPill(binding.textDifficulty, difficultyRes?.let(::getString))
        binding.rowMeta.visibility = if (
            binding.textCategory.visibility == View.VISIBLE ||
            binding.textPrepTime.visibility == View.VISIBLE ||
            binding.textDifficulty.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
    }

    private fun formatIngredient(ing: RecipeIngredient): String = listOfNotNull(
        ing.name,
        ing.quantity?.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else q.toString() },
        ing.unit,
    ).joinToString(" ")

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.menu_recipe_detail, menu)
        }

        // rieseguito ad ogni invalidateOptionsMenu(): adatta le voci a stato ricetta/preferito/autore
        override fun onPrepareMenu(menu: Menu) {
            val recipe = (viewModel.recipe.value as? Resource.Success)?.data
            val loaded = recipe != null
            val isAuthor = recipe != null && viewModel.isAuthor(recipe)
            val isFav = viewModel.isFavorite.value

            menu.findItem(R.id.action_favorite)?.apply {
                isVisible = loaded
                setIcon(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                setTitle(if (isFav) R.string.detail_favorite_remove else R.string.detail_favorite_add)
            }
            menu.findItem(R.id.action_share)?.isVisible = loaded
            menu.findItem(R.id.action_edit)?.isVisible = isAuthor
            menu.findItem(R.id.action_delete)?.isVisible = isAuthor
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
            R.id.action_favorite -> {
                viewModel.toggleFavorite()
                val msg = if (viewModel.isFavorite.value) R.string.detail_favorite_added
                else R.string.detail_favorite_removed
                Snackbar.make(binding.root, getString(msg), Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_share -> { shareRecipe(); true }
            R.id.action_edit -> { openEdit(); true }
            R.id.action_delete -> { confirmDelete(); true }
            else -> false
        }
    }

    // condivido la ricetta come testo semplice via intent ACTION_SEND
    private fun shareRecipe() {
        val recipe = (viewModel.recipe.value as? Resource.Success)?.data ?: return
        val text = buildString {
            appendLine(recipe.title)
            if (recipe.description.isNotBlank()) {
                appendLine()
                appendLine(recipe.description)
            }
            appendLine()
            appendLine(getString(R.string.ingredients) + ":")
            recipe.ingredients.forEach { appendLine("• " + formatIngredient(it)) }
            if (recipe.steps.isNotEmpty()) {
                appendLine()
                appendLine(getString(R.string.steps) + ":")
                recipe.steps.forEachIndexed { i, step -> appendLine("${i + 1}. $step") }
            }
        }.trim()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.detail_share)))
    }

    private fun openEdit() {
        findNavController().navigate(
            R.id.action_detail_to_edit,
            bundleOf(RecipeEditFragment.ARG_RECIPE_ID to recipeId),
        )
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_delete_title)
            .setMessage(R.string.detail_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.detail_delete) { _, _ -> viewModel.deleteRecipe() }
            .show()
    }

    private fun onDeleteState(state: Resource<Unit>) {
        binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
        when (state) {
            is Resource.Success -> {
                Snackbar.make(binding.root, getString(R.string.detail_deleted), Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
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
