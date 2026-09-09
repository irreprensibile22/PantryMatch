package com.example.esamemobile.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.esamemobile.R
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.databinding.ItemRecipeMatchBinding

// Schede grandi del feed Home (foto, badge preparabile/mancanti, cuore preferiti).
class RecipeMatchAdapter(
    private val onClick: (RecipeMatch) -> Unit,
    private val onToggleFavorite: (RecipeMatch, Boolean) -> Unit = { _, _ -> },
) : ListAdapter<RecipeMatch, RecipeMatchAdapter.VH>(DIFF) {

    var favoriteIds: Set<String> = emptySet()
        // il cambio dei preferiti tocca solo l'icona cuore: ridisegno tutto senza calcolare un diff
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(val binding: ItemRecipeMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(match: RecipeMatch) {
            val ctx = binding.root.context
            binding.textTitle.text = match.recipe.title
            binding.textAuthor.text = ctx.getString(R.string.by_author, match.recipe.authorName)
            binding.textAvatar.text =
                match.recipe.authorName.trim().firstOrNull()?.uppercase() ?: "?"
            binding.textTime.text = ctx.getString(R.string.prep_time_format, match.recipe.prepTimeMin)

            // badge verde se preparabile subito, altrimenti terracotta con il numero di mancanti
            if (match.isReady) {
                binding.textBadge.text = ctx.getString(R.string.badge_ready)
                binding.textBadge.setBackgroundResource(R.drawable.bg_badge_ready)
            } else {
                binding.textBadge.text = ctx.resources.getQuantityString(
                    R.plurals.badge_missing, match.missingCount, match.missingCount
                )
                binding.textBadge.setBackgroundResource(R.drawable.bg_badge_missing)
            }

            val isFav = favoriteIds.contains(match.recipe.id)
            binding.buttonFavorite.setImageResource(
                if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            binding.buttonFavorite.contentDescription = ctx.getString(
                if (isFav) R.string.detail_favorite_remove else R.string.detail_favorite_add
            )
            binding.buttonFavorite.setOnClickListener { onToggleFavorite(match, !isFav) }

            Glide.with(ctx)
                .load(match.recipe.imageUrl)
                .placeholder(R.drawable.ic_recipe_placeholder)
                .into(binding.imageRecipe)

            binding.root.setOnClickListener { onClick(match) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecipeMatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecipeMatch>() {
            override fun areItemsTheSame(old: RecipeMatch, new: RecipeMatch) =
                old.recipe.id == new.recipe.id
            override fun areContentsTheSame(old: RecipeMatch, new: RecipeMatch) = old == new
        }
    }
}
