package com.example.esamemobile.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.FragmentProfileBinding

/**
 * Profilo dell'utente: mostra i dati base e consente il logout.
 * (La lista delle proprie ricette verrà aggiunta collegando RecipeRepository.)
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = runCatching { authRepository.currentUser }.getOrNull()
        binding.textName.text = user?.displayName?.takeIf { it.isNotBlank() } ?: "Ospite"
        binding.textEmail.text = user?.email ?: "—"

        binding.buttonLogout.setOnClickListener {
            runCatching { authRepository.logout() }
            // TODO: navigare alla schermata di login dopo il logout
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
