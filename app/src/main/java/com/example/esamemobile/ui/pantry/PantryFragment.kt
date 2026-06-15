package com.example.esamemobile.ui.pantry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.esamemobile.databinding.DialogAddIngredientBinding
import com.example.esamemobile.databinding.FragmentPantryBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PantryFragment : Fragment() {

    private var _binding: FragmentPantryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PantryViewModel by viewModels()
    private val adapter = PantryAdapter(onDelete = { viewModel.deleteItem(it.id) })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPantryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerPantry.adapter = adapter
        binding.fabAdd.setOnClickListener { showAddDialog() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { render(it) }
            }
        }
        viewModel.load()
    }

    private fun render(state: Resource<List<com.example.esamemobile.data.model.PantryItem>>) {
        binding.swipeRefresh.isRefreshing = state is Resource.Loading
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

    private fun showAddDialog() {
        val dialogBinding = DialogAddIngredientBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aggiungi ingrediente")
            .setView(dialogBinding.root)
            .setPositiveButton("Aggiungi") { _, _ ->
                viewModel.addItem(
                    name = dialogBinding.editName.text?.toString().orEmpty(),
                    quantity = dialogBinding.editQuantity.text?.toString()?.toDoubleOrNull(),
                    unit = dialogBinding.editUnit.text?.toString()?.takeIf { it.isNotBlank() },
                )
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerPantry.adapter = null
        _binding = null
    }
}
