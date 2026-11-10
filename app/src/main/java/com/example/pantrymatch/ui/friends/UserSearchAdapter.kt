package com.example.pantrymatch.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pantrymatch.data.model.UserProfile
import com.example.pantrymatch.databinding.ItemUserSearchBinding

// User search results by email, with a "+" button to add them as friends.
class UserSearchAdapter(
    private val onAdd: (UserProfile) -> Unit,
) : ListAdapter<UserProfile, UserSearchAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserProfile) {
            val name = user.displayName.ifBlank { user.email }
            binding.textName.text = name
            binding.textAvatar.text = name.trim().firstOrNull()?.uppercase() ?: "?"
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
