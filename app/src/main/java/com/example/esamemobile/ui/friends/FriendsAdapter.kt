package com.example.esamemobile.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.esamemobile.data.model.Friend
import com.example.esamemobile.databinding.ItemFriendBinding

/** Lista degli amici già aggiunti, con azione di rimozione. */
class FriendsAdapter(
    private val onRemove: (Friend) -> Unit,
) : ListAdapter<Friend, FriendsAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friend: Friend) {
            binding.textName.text = friend.displayName.ifBlank { friend.email }
            binding.textEmail.text = friend.email
            binding.buttonRemove.setOnClickListener { onRemove(friend) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Friend>() {
            override fun areItemsTheSame(old: Friend, new: Friend) = old.uid == new.uid
            override fun areContentsTheSame(old: Friend, new: Friend) = old == new
        }
    }
}
