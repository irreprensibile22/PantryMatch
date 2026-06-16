package com.example.esamemobile.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.esamemobile.data.model.Friend
import com.example.esamemobile.data.model.UserProfile
import com.example.esamemobile.databinding.FragmentFriendsBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Gestione amici: ricerca utenti per email, aggiunta e rimozione. La lista degli
 * amici viene poi usata dalla ricerca ricette (filtro "Solo amici").
 */
class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendsViewModel by viewModels()
    private val friendsAdapter = FriendsAdapter(onRemove = { viewModel.removeFriend(it.uid) })
    private val searchAdapter = UserSearchAdapter(onAdd = { addFriend(it) })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerFriends.adapter = friendsAdapter
        binding.recyclerSearch.adapter = searchAdapter

        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(binding.editSearch.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.friends.collect { renderFriends(it) } }
                launch { viewModel.searchResults.collect { renderSearch(it) } }
            }
        }
        viewModel.loadFriends()
    }

    private fun renderFriends(state: Resource<List<Friend>>) {
        binding.progress.visibility = if (state is Resource.Loading) View.VISIBLE else View.GONE
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                friendsAdapter.submitList(state.data)
                binding.textEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderSearch(state: Resource<List<UserProfile>>) {
        when (state) {
            is Resource.Success -> searchAdapter.submitList(state.data)
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            is Resource.Loading -> Unit
        }
    }

    private fun addFriend(user: UserProfile) {
        viewModel.addFriend(user)
        binding.editSearch.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerFriends.adapter = null
        binding.recyclerSearch.adapter = null
        _binding = null
    }
}
