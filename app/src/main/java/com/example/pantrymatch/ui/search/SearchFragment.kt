package com.example.pantrymatch.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.pantrymatch.R
import com.example.pantrymatch.data.model.RecipeMatch
import com.example.pantrymatch.data.repository.AuthRepository
import com.example.pantrymatch.databinding.DialogFiltersBinding
import com.example.pantrymatch.databinding.FragmentSearchBinding
import com.example.pantrymatch.ui.recipe.RecipeDetailFragment
import com.example.pantrymatch.util.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// App Home: community feed with a Filters dialog (availability + category).
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private val adapter = RecipeMatchAdapter(
        onClick = { openDetail(it) },
        onToggleFavorite = { match, favorite -> viewModel.toggleFavorite(match, favorite) },
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerResults.adapter = adapter

        val name = runCatching { AuthRepository().currentUser?.displayName }.getOrNull()
        binding.textProfileInitial.text =
            name?.trim()?.firstOrNull()?.uppercase() ?: "?"

        binding.buttonFilters.setOnClickListener { showFilterDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { render(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteIds.collect { adapter.favoriteIds = it }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // reload favorites on return: they may have changed from the detail screen
        viewModel.loadFavorites()
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogFiltersBinding.inflate(layoutInflater)

        dialogBinding.chipGroupAvailability.check(
            when (viewModel.maxMissing) {
                0 -> dialogBinding.chipAvailabilityReady.id
                1 -> dialogBinding.chipAvailabilityMax1.id
                2 -> dialogBinding.chipAvailabilityMax2.id
                else -> dialogBinding.chipAvailabilityAll.id
            }
        )

        // first chip "All" (no tag), then one chip per category; the tag marks filterable chips
        val categories = resources.getStringArray(R.array.recipe_categories)
        addCategoryChip(dialogBinding, getString(R.string.filter_all), isAll = true)
        categories.forEach { addCategoryChip(dialogBinding, it, isAll = false) }
        checkCurrentCategory(dialogBinding)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_title)
            .setView(dialogBinding.root)
            .setNeutralButton(R.string.filter_reset) { _, _ -> viewModel.setFilters(null, null) }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.filter_apply) { _, _ ->
                val maxMissing = when (dialogBinding.chipGroupAvailability.checkedChipId) {
                    dialogBinding.chipAvailabilityReady.id -> 0
                    dialogBinding.chipAvailabilityMax1.id -> 1
                    dialogBinding.chipAvailabilityMax2.id -> 2
                    else -> null
                }
                val checkedCategory = dialogBinding.root
                    .findViewById<Chip>(dialogBinding.chipGroupCategory.checkedChipId)
                val category = checkedCategory
                    ?.takeIf { it.tag == CATEGORY_TAG }
                    ?.text?.toString()
                viewModel.setFilters(maxMissing, category)
            }
            .show()
    }

    private fun addCategoryChip(dialogBinding: DialogFiltersBinding, label: String, isAll: Boolean) {
        val chip = Chip(requireContext()).apply {
            text = label
            isCheckable = true
            if (isAll) isChecked = true else tag = CATEGORY_TAG
        }
        dialogBinding.chipGroupCategory.addView(chip)
    }

    private fun checkCurrentCategory(dialogBinding: DialogFiltersBinding) {
        val current = viewModel.category ?: return
        val group = dialogBinding.chipGroupCategory
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.tag == CATEGORY_TAG && chip.text.toString().equals(current, ignoreCase = true)) {
                chip.isChecked = true
                return
            }
        }
    }

    private fun render(state: Resource<List<RecipeMatch>>) {
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

    private fun openDetail(match: RecipeMatch) {
        findNavController().navigate(
            R.id.action_search_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to match.recipe.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerResults.adapter = null
        _binding = null
    }

    companion object {
        private const val CATEGORY_TAG = "category"
    }
}
