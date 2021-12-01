package eu.zimbelstern.tournant

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemRecipeBinding
import kotlin.random.Random

class RecipeListAdapter(private val allRecipes: List<Recipe>) : RecyclerView.Adapter<RecipeListAdapter.RecipeListViewHolder>() {

	private var filteredRecipes = allRecipes

	class RecipeListViewHolder(val binding: RecyclerItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeListViewHolder {
		val binding = RecyclerItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return RecipeListViewHolder(binding)
	}

	override fun onBindViewHolder(holder: RecipeListViewHolder, position: Int) {
		val context = holder.binding.root.context
		val recipe = filteredRecipes[position]
		if (recipe.image != null)
			holder.binding.recipeCardImage.setImageBitmap(BitmapFactory.decodeByteArray(recipe.image, 0, recipe.image.size))
		else
			holder.binding.recipeCardImage.setImageResource(R.drawable.ic_dining)
		holder.binding.recipeCardCategory.apply {
			text = recipe.category
			val colors = resources.obtainTypedArray(R.array.material_colors_700)
			val colorsRipple = resources.obtainTypedArray(R.array.material_colors_900)
			val hashColor = Random(recipe.category.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size)
			chipBackgroundColor = colors.getColorStateList(hashColor)
			rippleColor = colorsRipple.getColorStateList(hashColor)
			colors.recycle()
			colorsRipple.recycle()
			visibility = if (recipe.category != null) View.VISIBLE else View.GONE
			setOnClickListener {
				if (context is MainActivity)
					context.searchForSomething(recipe.category)
			}
		}
		holder.binding.recipeCardCuisine.apply {
			text = recipe.cuisine
			val colors = resources.obtainTypedArray(R.array.material_colors_700)
			val colorsRipple = resources.obtainTypedArray(R.array.material_colors_900)
			val hashColor = Random(recipe.category.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size)
			chipBackgroundColor = colors.getColorStateList(hashColor)
			rippleColor = colorsRipple.getColorStateList(hashColor)
			colors.recycle()
			colorsRipple.recycle()
			visibility = if (recipe.cuisine != null) View.VISIBLE else View.GONE
			setOnClickListener {
				if (context is MainActivity)
					context.searchForSomething(recipe.cuisine)
			}
		}
		holder.binding.recipeCardRating.apply {
			rating = recipe.rating ?: 0f
			visibility = if (recipe.rating != null) View.VISIBLE else View.GONE
		}
		holder.binding.recipeCardTitle.text = recipe.title
		holder.binding.root.setOnClickListener {
			val intent = Intent(holder.binding.root.context, RecipeDetail::class.java).apply {
				putExtra("RECIPE", recipe)
			}
			holder.binding.root.context.startActivity(intent)
		}
	}

	override fun getItemCount(): Int {
		return filteredRecipes.size
	}

	@SuppressLint("NotifyDataSetChanged")
	fun filterRecipes(query: CharSequence?) {
		filteredRecipes = if (query != null) allRecipes.filter {
			(it.title?.contains(query, true) ?: false)
					|| (it.category?.contains(query, true) ?: false)
					|| (it.cuisine?.contains(query, true) ?: false)
		} else allRecipes
		notifyDataSetChanged()
	}

}