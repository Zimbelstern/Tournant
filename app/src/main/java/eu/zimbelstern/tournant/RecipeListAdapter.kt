package eu.zimbelstern.tournant

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemRecipeBinding
import kotlin.random.Random

class RecipeListAdapter(private val mainActivity: MainActivity, private val allRecipes: List<Recipe>) : RecyclerView.Adapter<RecipeListAdapter.RecipeListViewHolder>() {

	private var filteredRecipes = allRecipes

	class RecipeListViewHolder(val binding: RecyclerItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeListViewHolder {
		val binding = RecyclerItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return RecipeListViewHolder(binding)
	}

	override fun onBindViewHolder(holder: RecipeListViewHolder, position: Int) {
		val recipe = filteredRecipes[position]
		holder.binding.recipeCardImage.apply {
			if (recipe.image != null) {
				setImageBitmap(BitmapFactory.decodeByteArray(recipe.image, 0, recipe.image.size))
				clipToOutline = true
				setPadding(0)
			} else {
				setImageResource(R.drawable.ic_dining)
				setPadding(4 * resources.displayMetrics.density.toInt())
			}
		}
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
				mainActivity.searchForSomething(recipe.category)
			}
		}
		holder.binding.recipeCardCuisine.apply {
			text = recipe.cuisine
			val colors = resources.obtainTypedArray(R.array.material_colors_700)
			val colorsRipple = resources.obtainTypedArray(R.array.material_colors_900)
			val hashColor = Random(recipe.cuisine.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size)
			chipBackgroundColor = colors.getColorStateList(hashColor)
			rippleColor = colorsRipple.getColorStateList(hashColor)
			colors.recycle()
			colorsRipple.recycle()
			visibility = if (recipe.cuisine != null) View.VISIBLE else View.GONE
			setOnClickListener {
				mainActivity.searchForSomething(recipe.cuisine)
			}
		}
		holder.binding.recipeCardRating.apply {
			rating = recipe.rating ?: 0f
			visibility = if (recipe.rating != null) View.VISIBLE else View.GONE
		}
		holder.binding.recipeCardTitle.text = recipe.title
		holder.binding.root.setOnClickListener {
			mainActivity.openRecipeDetail(recipe)
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