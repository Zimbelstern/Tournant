package eu.zimbelstern.tournant

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.data.ColorfulString
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.databinding.RecyclerItemRecipeBinding
import eu.zimbelstern.tournant.ui.MainActivity

class RecipeListAdapter(private val mainActivity: MainActivity)
	: PagingDataAdapter<RecipeDescription, RecipeListAdapter.RecipeListViewHolder>(DIFF_CALLBACK) {

	companion object {
		val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipeDescription>() {
			override fun areItemsTheSame(old: RecipeDescription, new: RecipeDescription): Boolean =
				old.id == new.id

			override fun areContentsTheSame(old: RecipeDescription, new: RecipeDescription): Boolean =
				old == new
		}
	}

	private var ccColors = mapOf<String, ColorfulString>()

	inner class RecipeListViewHolder(val binding: RecyclerItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeListViewHolder {
		val binding = RecyclerItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return RecipeListViewHolder(binding)
	}

	override fun onBindViewHolder(holder: RecipeListViewHolder, position: Int) {
		val recipe = getItem(position) ?: return
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
			chipBackgroundColor = ccColors[text]?.color
			rippleColor = ccColors[text]?.rippleColor
			visibility = if (recipe.category != null) View.VISIBLE else View.GONE
			setOnClickListener {
				mainActivity.searchForSomething(text)
			}
		}
		holder.binding.recipeCardCuisine.apply {
			text = recipe.cuisine
			chipBackgroundColor = ccColors[text]?.color
			rippleColor = ccColors[text]?.rippleColor
			visibility = if (recipe.cuisine != null) View.VISIBLE else View.GONE
			setOnClickListener {
				mainActivity.searchForSomething(text)
			}
		}
		holder.binding.recipeCardRating.apply {
			rating = recipe.rating ?: 0f
			visibility = if (recipe.rating != null) View.VISIBLE else View.GONE
		}
		holder.binding.recipeCardTitle.text = recipe.title
		holder.binding.root.setOnClickListener {
			mainActivity.openRecipeDetail(recipe.id)
		}
	}

	fun updateColors(colorfulStrings: List<ColorfulString>) {
		ccColors = colorfulStrings.associateBy {
			it.string
		}
	}

}