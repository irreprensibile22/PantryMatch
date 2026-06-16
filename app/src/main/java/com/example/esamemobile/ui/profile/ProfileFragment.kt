package com.example.esamemobile.ui.profile

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
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.FragmentProfileBinding
import com.example.esamemobile.ui.recipe.RecipeDetailFragment
import com.example.esamemobile.ui.recipe.RecipeEditFragment
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Profilo dell'utente: dati base, elenco delle proprie ricette (con accesso alla
 * creazione) e logout.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()
    private val viewModel: ProfileViewModel by viewModels()
    private val adapter = RecipeAdapter(
        onClick = { openDetail(it) },
        onEdit = { openEdit(it) },
        onDelete = { confirmDelete(it) },
    )

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

        binding.recyclerRecipes.adapter = adapter

        binding.fabAddRecipe.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit)
        }

        binding.buttonLogout.setOnClickListener {
            runCatching { authRepository.logout() }
            findNavController().navigate(R.id.action_profile_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myRecipes.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica al rientro (es. dopo aver creato una ricetta)
        viewModel.loadMyRecipes()
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
            R.id.action_profile_to_detail,
            bundleOf(RecipeDetailFragment.ARG_RECIPE_ID to recipe.id),
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
        _binding = null
    }
}
