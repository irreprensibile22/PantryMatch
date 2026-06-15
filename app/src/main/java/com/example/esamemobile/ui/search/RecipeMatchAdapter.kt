package com.example.esamemobile.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.esamemobile.R
import com.example.esamemobile.data.model.RecipeMatch
import com.example.esamemobile.databinding.ItemRecipeMatchBinding

class RecipeMatchAdapter(
    private val onClick: (RecipeMatch) -> Unit,
) : ListAdapter<RecipeMatch, RecipeMatchAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(match: RecipeMatch) {
            val ctx = binding.root.context
            binding.textTitle.text = match.recipe.title
            binding.textAuthor.text = match.recipe.authorName
            binding.textTime.text = ctx.getString(R.string.prep_time_format, match.recipe.prepTimeMin)

            if (match.isReady) {
                binding.textBadge.text = ctx.getString(R.string.badge_ready)
                binding.textBadge.setBackgroundResource(R.drawable.bg_badge_ready)
            } else {
                binding.textBadge.text = ctx.resources.getQuantityString(
                    R.plurals.badge_missing, match.missingCount, match.missingCount
                )
                binding.textBadge.setBackgroundResource(R.drawable.bg_badge_missing)
            }

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
