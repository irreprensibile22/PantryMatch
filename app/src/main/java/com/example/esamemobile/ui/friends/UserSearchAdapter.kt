package com.example.esamemobile.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.esamemobile.data.model.UserProfile
import com.example.esamemobile.databinding.ItemUserSearchBinding

/** Risultati della ricerca utenti per email, con azione "aggiungi amico". */
class UserSearchAdapter(
    private val onAdd: (UserProfile) -> Unit,
) : ListAdapter<UserProfile, UserSearchAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserProfile) {
            binding.textName.text = user.displayName.ifBlank { user.email }
            binding.textEmail.text = user.email
            binding.buttonAdd.setOnClickListener { onAdd(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserProfile>() {
            override fun areItemsTheSame(old: UserProfile, new: UserProfile) = old.uid == new.uid
            override fun areContentsTheSame(old: UserProfile, new: UserProfile) = old == new
        }
    }
}
