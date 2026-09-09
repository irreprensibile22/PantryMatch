package com.example.esamemobile.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.esamemobile.R
import com.example.esamemobile.databinding.FragmentLoginBinding
import com.example.esamemobile.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

// Schermata di login: email/password + accesso con Google.
class LoginFragment : Fragment() {

    // pattern ViewBinding: azzero _binding in onDestroyView per non trattenere le view
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            viewModel.loginWithGoogle(credential)
        }.onFailure {
            Snackbar.make(binding.root, getString(R.string.login_google_error), Snackbar.LENGTH_LONG).show()
        }
    }

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
        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        setupGoogleSignIn()

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

    private fun setupGoogleSignIn() {
        // default_web_client_id lo genera il plugin google-services: se manca, nascondo il pulsante Google
        val resId = resources.getIdentifier(
            "default_web_client_id", "string", requireContext().packageName
        )
        if (resId == 0) {
            binding.buttonGoogle.visibility = View.GONE
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(resId))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireContext(), options)
        binding.buttonGoogle.setOnClickListener {
            // signOut prima del launch cosi mostra sempre il selettore account, non riusa l'ultimo
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
