package com.example.pantrymatch.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pantrymatch.R
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.databinding.ItemRecipeGridBinding

// Compact cards for the "See all" grid (2 columns).
class RecipeGridAdapter(
    private val onClick: (Recipe) -> Unit,
) : ListAdapter<Recipe, RecipeGridAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            val ctx = binding.root.context
            binding.textTitle.text = recipe.title
            binding.textSubtitle.text = ctx.getString(R.string.prep_time_format, recipe.prepTimeMin)
            Glide.with(ctx)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_recipe_placeholder)
                .into(binding.imageRecipe)
            binding.root.setOnClickListener { onClick(recipe) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecipeGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Recipe>() {
            override fun areItemsTheSame(old: Recipe, new: Recipe) = old.id == new.id
            override fun areContentsTheSame(old: Recipe, new: Recipe) = old == new
        }
    }
}
