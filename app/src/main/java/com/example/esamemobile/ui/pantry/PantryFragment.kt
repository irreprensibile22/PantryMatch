package com.example.esamemobile.ui.pantry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.esamemobile.R
import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.databinding.DialogAddIngredientBinding
import com.example.esamemobile.databinding.FragmentPantryBinding
import com.example.esamemobile.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

class PantryFragment : Fragment() {

    private var _binding: FragmentPantryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PantryViewModel by viewModels()
    private val adapter = PantryAdapter(
        onEdit = { showItemDialog(it) },
        onDelete = { viewModel.deleteItem(it.id) },
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPantryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerPantry.adapter = adapter
        binding.fabAdd.setOnClickListener { showItemDialog() }
        binding.fabScan.setOnClickListener { startScan() }

        val name = runCatching { AuthRepository().currentUser?.displayName }.getOrNull()
        binding.textProfileInitial.text = name?.trim()?.firstOrNull()?.uppercase() ?: "?"
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { render(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanning.collect { binding.fabScan.isEnabled = !it }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanResult.collect { onScanResult(it) }
            }
        }
        viewModel.load()
    }

    /** Avvia la scansione tramite il Google code scanner (nessun permesso CAMERA). */
    private fun startScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE,
            )
            .build()
        GmsBarcodeScanning.getClient(requireContext(), options).startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.takeIf { it.isNotBlank() }?.let { viewModel.lookupBarcode(it) }
            }
            .addOnCanceledListener { /* scansione annullata dall'utente */ }
            .addOnFailureListener {
                Snackbar.make(binding.root, R.string.scan_error, Snackbar.LENGTH_LONG).show()
            }
    }

    /** Risultato del lookup del prodotto: apre il dialog pre-compilato o avvisa. */
    private fun onScanResult(result: PantryViewModel.ScanResult) {
        when (result) {
            is PantryViewModel.ScanResult.Found ->
                showItemDialog(prefillName = result.name.replaceFirstChar { it.uppercase() })
            is PantryViewModel.ScanResult.NotFound -> {
                Snackbar.make(
                    binding.root,
                    getString(R.string.scan_not_found, result.barcode),
                    Snackbar.LENGTH_LONG,
                ).show()
                showItemDialog()
            }
            PantryViewModel.ScanResult.Error ->
                Snackbar.make(binding.root, R.string.scan_error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun render(state: Resource<List<PantryItem>>) {
        binding.swipeRefresh.isRefreshing = state is Resource.Loading
        when (state) {
            is Resource.Loading -> Unit
            is Resource.Success -> {
                adapter.submitList(state.data)
                binding.textEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                binding.textPantryCount.text = resources.getQuantityString(
                    R.plurals.pantry_count, state.data.size, state.data.size
                )
            }
            is Resource.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Dialog condiviso per aggiungere un nuovo ingrediente ([existing] null) o
     * modificarne uno esistente (campi pre-compilati).
     */
    private fun showItemDialog(existing: PantryItem? = null, prefillName: String? = null) {
        val dialogBinding = DialogAddIngredientBinding.inflate(layoutInflater)

        // Unità: menu a tendina con valori predefiniti (non digitabile).
        val noUnit = getString(R.string.unit_none)
        dialogBinding.editUnit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.pantry_units),
            )
        )
        // Selezione iniziale: unità esistente oppure "(nessuna)".
        dialogBinding.editUnit.setText(existing?.unit?.takeIf { it.isNotBlank() } ?: noUnit, false)

        existing?.let {
            // setText(.., false) per non far comparire il dropdown di suggerimenti
            // durante il pre-riempimento in modifica.
            dialogBinding.editName.setText(it.name.replaceFirstChar { c -> c.uppercase() }, false)
            dialogBinding.editQuantity.setText(it.quantity?.let(::trimNumber).orEmpty())
        }
        // Nome pre-compilato dal codice a barre (solo in aggiunta, non in modifica).
        if (existing == null && !prefillName.isNullOrBlank()) {
            dialogBinding.editName.setText(prefillName, false)
        }
        val isEdit = existing != null
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) "Modifica ingrediente" else "Aggiungi ingrediente")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Salva" else "Aggiungi") { _, _ ->
                val name = dialogBinding.editName.text?.toString().orEmpty()
                val quantity = dialogBinding.editQuantity.text?.toString()?.toDoubleOrNull()
                val unit = dialogBinding.editUnit.text?.toString()
                    ?.takeIf { it.isNotBlank() && it != noUnit }
                if (existing != null) {
                    viewModel.updateItem(existing.copy(name = name, quantity = quantity, unit = unit))
                } else {
                    viewModel.addItem(name, quantity, unit)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerPantry.adapter = null
        _binding = null
    }
}
