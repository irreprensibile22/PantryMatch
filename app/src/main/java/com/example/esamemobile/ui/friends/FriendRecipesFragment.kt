package com.example.esamemobile.ui.friends

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
import com.example.esamemobile.R
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.databinding.FragmentFriendRecipesBinding
import com.example.esamemobile.ui.recipe.RecipeDetailFragment
import com.example.esamemobile.ui.search.RecipeMatchAdapter
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// Elenco delle ricette di un amico; riceve authorId/authorName come argomenti di navigazione.
class FriendRecipesFragment : Fragment() {

    private var _binding: FragmentFriendRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendRecipesViewModel by viewModels()
    private val adapter = RecipeMatchAdapter(
        onClick = { openDetail(it) },
        onToggleFavorite = { match, favorite -> viewModel.toggleFavorite(match, favorite) },
    )

    private val authorId: String get() = arguments?.getString(ARG_AUTHOR_ID).orEmpty()
    private val authorName: String get() = arguments?.getString(ARG_AUTHOR_NAME).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textHeaderTitle.text =
            authorName.ifBlank { getString(R.string.nav_friend_recipes) }
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        binding.recyclerResults.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.results.collect { render(it) } }
                launch { viewModel.favoriteIds.collect { adapter.favoriteIds = it } }
            }
        }
        viewModel.load(authorId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
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
            R.id.action_friend_recipes_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to match.recipe.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerResults.adapter = null
        _binding = null
    }

    companion object {
        const val ARG_AUTHOR_ID = "authorId"
        const val ARG_AUTHOR_NAME = "authorName"
    }
}
