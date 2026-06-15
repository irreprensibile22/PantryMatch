package com.example.esamemobile.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.esamemobile.R
import com.example.esamemobile.databinding.FragmentLoginBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                binding.editEmail.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
            )
        }
        // TODO: pulsante Google Sign-In + navigazione alla registrazione

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.visibility =
                        if (state is Resource.Loading) View.VISIBLE else View.GONE
                    when (state) {
                        is Resource.Success ->
                            findNavController().navigate(R.id.action_login_to_pantry)
                        is Resource.Error ->
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
