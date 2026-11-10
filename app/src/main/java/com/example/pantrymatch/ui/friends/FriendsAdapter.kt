package com.example.pantrymatch.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pantrymatch.R
import com.example.pantrymatch.data.model.Friend
import com.example.pantrymatch.databinding.ItemFriendBinding

// Friends list: name, email, recipe count and remove button.
class FriendsAdapter(
    private val onClick: (Friend) -> Unit,
    private val onRemove: (Friend) -> Unit,
) : ListAdapter<FriendItem, FriendsAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendItem) {
            val friend = item.friend
            // fall back to the email if the name is missing
            val name = friend.displayName.ifBlank { friend.email }
            binding.textName.text = name
            binding.textAvatar.text = name.trim().firstOrNull()?.uppercase() ?: "?"
            binding.textEmail.text = friend.email
            binding.textRecipeCount.text = binding.root.resources.getQuantityString(
                R.plurals.friend_recipe_count, item.recipeCount, item.recipeCount
            )
            binding.root.setOnClickListener { onClick(friend) }
            binding.buttonRemove.setOnClickListener { onRemove(friend) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FriendItem>() {
            override fun areItemsTheSame(old: FriendItem, new: FriendItem) =
                old.friend.uid == new.friend.uid
            override fun areContentsTheSame(old: FriendItem, new: FriendItem) = old == new
        }
    }
}
