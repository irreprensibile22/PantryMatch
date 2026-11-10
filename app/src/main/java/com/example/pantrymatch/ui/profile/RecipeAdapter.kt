package com.example.pantrymatch.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pantrymatch.R
import com.example.pantrymatch.data.model.Recipe
import com.example.pantrymatch.databinding.ItemRecipeBinding

// "My recipes" in the Recipe Book: full-width cards with Public/Private badge and actions.
class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onEdit: (Recipe) -> Unit,
    private val onDelete: (Recipe) -> Unit,
) : ListAdapter<Recipe, RecipeAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            val ctx = binding.root.context
            binding.textTitle.text = recipe.title
            binding.textTime.text = ctx.getString(R.string.prep_time_format, recipe.prepTimeMin)
            val count = recipe.ingredients.size
            binding.textIngredients.text =
                "· " + ctx.resources.getQuantityString(R.plurals.ingredients_count, count, count)

            if (recipe.isPublic) {
                binding.textBadge.setText(R.string.recipe_visibility_public)
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_public)
                binding.textBadge.setTextColor(Color.parseColor("#5C6E3F"))
            } else {
                binding.textBadge.setText(R.string.recipe_visibility_private)
                binding.textBadge.setBackgroundResource(R.drawable.bg_status_private)
                binding.textBadge.setTextColor(Color.parseColor("#8A7766"))
            }

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
