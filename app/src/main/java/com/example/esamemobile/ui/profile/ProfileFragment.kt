package com.example.esamemobile.ui.profile

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.esamemobile.R
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.DialogChangeEmailBinding
import com.example.esamemobile.databinding.DialogChangePasswordBinding
import com.example.esamemobile.databinding.DialogEditNameBinding
import com.example.esamemobile.databinding.FragmentProfileBinding
import com.example.esamemobile.util.SettingsPrefs
import com.example.esamemobile.work.PantryMatchScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Profilo dell'utente: hero (avatar, nome, email), statistiche (ricette/salvate/
 * amici), gestione account (modifica nome, cambio email/password, reset password),
 * preferenza notifiche e logout. Le ricette pubblicate/preferite sono nel
 * Ricettario (RecipeBookFragment).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindUser()
        setupAccountRows()
        setupNotificationsSwitch()

        binding.buttonLogout.setOnClickListener {
            runCatching { authRepository.logout() }
            findNavController().navigate(R.id.action_profile_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.events.collect { handleEvent(it) } }
                launch { viewModel.stats.collect { renderStats(it) } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Al rientro: riallinea l'email del profilo e aggiorna i conteggi.
        viewModel.reconcileEmail()
        viewModel.loadStats()
    }

    private fun bindUser() {
        val user = runCatching { authRepository.currentUser }.getOrNull()
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_guest)
        binding.textName.text = name
        binding.textEmail.text = user?.email ?: "—"
        binding.textAvatar.text = name.trim().firstOrNull()?.uppercase() ?: "?"
    }

    private fun renderStats(stats: ProfileViewModel.Stats) {
        binding.textStatRecipes.text = stats.recipes.toString()
        binding.textStatSaved.text = stats.saved.toString()
        binding.textStatFriends.text = stats.friends.toString()
    }

    private fun setupAccountRows() {
        binding.rowEditName.imageIcon.setImageResource(R.drawable.ic_edit)
        binding.rowEditName.textLabel.setText(R.string.profile_edit_name)
        binding.rowEditName.root.setOnClickListener { showEditNameDialog() }

        binding.rowChangeEmail.imageIcon.setImageResource(R.drawable.ic_mail)
        binding.rowChangeEmail.textLabel.setText(R.string.profile_change_email)
        binding.rowChangeEmail.root.setOnClickListener { showChangeEmailDialog() }

        binding.rowChangePassword.imageIcon.setImageResource(R.drawable.ic_lock)
        binding.rowChangePassword.textLabel.setText(R.string.profile_change_password)
        binding.rowChangePassword.root.setOnClickListener { showChangePasswordDialog() }

        binding.rowResetPassword.imageIcon.setImageResource(R.drawable.ic_reset)
        binding.rowResetPassword.textLabel.setText(R.string.profile_reset_password)
        binding.rowResetPassword.root.setOnClickListener { showResetPasswordDialog() }
    }

    private fun setupNotificationsSwitch() {
        binding.switchNotifications.isChecked = SettingsPrefs.notificationsEnabled(requireContext())
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val ctx = requireContext().applicationContext
            SettingsPrefs.setNotificationsEnabled(ctx, isChecked)
            if (isChecked) PantryMatchScheduler.schedule(ctx) else PantryMatchScheduler.cancel(ctx)
        }
    }

    private fun handleEvent(event: ProfileViewModel.Event) {
        when (event) {
            is ProfileViewModel.Event.NameUpdated -> {
                bindUser()
                toast(getString(R.string.profile_name_updated))
            }
            is ProfileViewModel.Event.PasswordUpdated ->
                toast(getString(R.string.profile_password_updated))
            is ProfileViewModel.Event.EmailVerificationSent ->
                toast(getString(R.string.profile_email_verification_sent))
            is ProfileViewModel.Event.PasswordResetSent ->
                toast(getString(R.string.profile_reset_password_sent, event.email))
            is ProfileViewModel.Event.EmailReconciled -> {
                bindUser()
                toast(getString(R.string.profile_email_updated))
            }
            is ProfileViewModel.Event.SessionRevoked -> {
                Toast.makeText(
                    requireContext(), R.string.session_revoked_relogin, Toast.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.action_profile_to_login)
            }
            is ProfileViewModel.Event.Error ->
                toast(event.message)
        }
    }

    private fun showEditNameDialog() {
        val b = DialogEditNameBinding.inflate(layoutInflater)
        b.editName.setText(binding.textName.text)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_edit_name)
            .setView(b.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .show()
        dialog.positiveButton().setOnClickListener {
            val name = b.editName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                b.layoutName.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            viewModel.updateName(name)
            dialog.dismiss()
        }
    }

    private fun showChangeEmailDialog() {
        val b = DialogChangeEmailBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_change_email)
            .setView(b.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .show()
        dialog.positiveButton().setOnClickListener {
            val email = b.editNewEmail.text?.toString()?.trim().orEmpty()
            val current = b.editCurrentPassword.text?.toString().orEmpty()
            b.layoutNewEmail.error = null
            b.layoutCurrentPassword.error = null
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                b.layoutNewEmail.error = getString(R.string.register_email_invalid)
                return@setOnClickListener
            }
            if (current.isEmpty()) {
                b.layoutCurrentPassword.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            viewModel.changeEmail(current, email)
            dialog.dismiss()
        }
    }

    private fun showChangePasswordDialog() {
        val b = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_change_password)
            .setView(b.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .show()
        dialog.positiveButton().setOnClickListener {
            val current = b.editCurrentPassword.text?.toString().orEmpty()
            val new = b.editNewPassword.text?.toString().orEmpty()
            val confirm = b.editConfirmPassword.text?.toString().orEmpty()
            b.layoutCurrentPassword.error = null
            b.layoutNewPassword.error = null
            b.layoutConfirmPassword.error = null
            if (current.isEmpty()) {
                b.layoutCurrentPassword.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (new.length < 6) {
                b.layoutNewPassword.error = getString(R.string.password_min_length)
                return@setOnClickListener
            }
            if (new != confirm) {
                b.layoutConfirmPassword.error = getString(R.string.register_password_mismatch)
                return@setOnClickListener
            }
            viewModel.changePassword(current, new)
            dialog.dismiss()
        }
    }

    private fun showResetPasswordDialog() {
        val email = binding.textEmail.text
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_reset_password)
            .setMessage(getString(R.string.profile_reset_password_message, email))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.action_send) { _, _ -> viewModel.sendPasswordReset() }
            .show()
    }

    private fun androidx.appcompat.app.AlertDialog.positiveButton() =
        getButton(android.content.DialogInterface.BUTTON_POSITIVE)

    private fun toast(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
