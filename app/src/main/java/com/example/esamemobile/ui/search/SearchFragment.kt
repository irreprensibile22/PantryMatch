package com.example.esamemobile.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.databinding.FragmentSearchBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private val adapter = RecipeMatchAdapter(onClick = { openDetail(it) })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerResults.adapter = adapter

        binding.buttonSearch.setOnClickListener {
            val maxMissing = when (binding.chipGroupMissing.checkedChipId) {
                binding.chipMissing1.id -> 1
                binding.chipMissing2.id -> 2
                else -> 0
            }
            viewModel.search(maxMissing, friendsOnly = binding.chipFriendsOnly.isChecked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { render(it) }
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
        // TODO: navigare al dettaglio passando match.recipe.id via Safe Args
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerResults.adapter = null
        _binding = null
    }
}
