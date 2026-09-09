package com.example.esamemobile.ui.auth

import android.os.Bundle
import android.util.Patterns
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
import com.example.esamemobile.databinding.FragmentRegisterBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// Schermata di registrazione con email/password e validazione dei campi lato client.
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonRegister.setOnClickListener {
            val name = binding.editName.text?.toString().orEmpty().trim()
            val email = binding.editEmail.text?.toString().orEmpty().trim()
            val password = binding.editPassword.text?.toString().orEmpty()
            val confirm = binding.editPasswordConfirm.text?.toString().orEmpty()

            // valido prima di chiamare Firebase (password minima 6 caratteri, come richiede Firebase)
            val error = when {
                name.isEmpty() || email.isEmpty() || password.length < 6 ->
                    getString(R.string.register_invalid)
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    getString(R.string.register_email_invalid)
                password != confirm ->
                    getString(R.string.register_password_mismatch)
                else -> null
            }
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.register(email, password, name)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.visibility =
                        if (state is Resource.Loading) View.VISIBLE else View.GONE
                    when (state) {
                        is Resource.Success ->
                            findNavController().navigate(R.id.action_register_to_pantry)
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
