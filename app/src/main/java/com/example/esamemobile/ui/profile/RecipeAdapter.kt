package com.example.esamemobile.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.esamemobile.R
import com.example.esamemobile.data.model.Recipe
import com.example.esamemobile.databinding.ItemRecipeBinding

/** Lista semplice di ricette (usata per "le mie ricette" nel profilo). */
class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onEdit: (Recipe) -> Unit,
    private val onDelete: (Recipe) -> Unit,
) : ListAdapter<Recipe, RecipeAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            val ctx = binding.root.context
            binding.textTitle.text = recipe.title
            binding.textSubtitle.text = ctx.getString(R.string.prep_time_format, recipe.prepTimeMin)
            binding.textVisibility.text = ctx.getString(
                if (recipe.isPublic) R.string.recipe_visibility_public else R.string.recipe_visibility_private
            )
            Glide.with(ctx)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_recipe_placeholder)
                .into(binding.imageRecipe)
            binding.root.setOnClickListener { onClick(recipe) }
            binding.buttonEdit.setOnClickListener { onEdit(recipe) }
            binding.buttonDelete.setOnClickListener { onDelete(recipe) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
