package com.example.esamemobile.ui.pantry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.esamemobile.data.model.PantryItem
import com.example.esamemobile.databinding.ItemPantryBinding

// Lista degli ingredienti in dispensa; ListAdapter usa DIFF per aggiornare solo le righe cambiate.
class PantryAdapter(
    private val onEdit: (PantryItem) -> Unit,
    private val onDelete: (PantryItem) -> Unit,
) : ListAdapter<PantryItem, PantryAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PantryItem) {
            binding.textName.text = item.name.replaceFirstChar { it.uppercase() }
            val qty = listOfNotNull(item.quantity?.let { trimNumber(it) }, item.unit)
                .joinToString(" ")
            binding.textQuantity.text = qty
            binding.buttonEdit.setOnClickListener { onEdit(item) }
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }

        // mostra "2" invece di "2.0" quando la quantita e intera
        private fun trimNumber(value: Double): String =
            if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPantryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PantryItem>() {
            override fun areItemsTheSame(old: PantryItem, new: PantryItem) = old.id == new.id
            override fun areContentsTheSame(old: PantryItem, new: PantryItem) = old == new
        }
    }
}
