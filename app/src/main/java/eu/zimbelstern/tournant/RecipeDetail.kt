package eu.zimbelstern.tournant

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import eu.zimbelstern.tournant.databinding.ActivityRecipeDetailBinding
import kotlin.random.Random

class RecipeDetail : AppCompatActivity() {

	private lateinit var binding: ActivityRecipeDetailBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
		setContentView(binding.root)

		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getBoolean("SCREEN_ON", true))
			window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		intent.extras?.getParcelable<Recipe>("RECIPE")?.let { recipe ->
			title = recipe.title
			binding.recipeDetailTitle.text = recipe.title
			recipe.image?.let {
				binding.recipeDetailImage.visibility = View.VISIBLE
				binding.recipeDetailImageDrawable.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
			}
			recipe.category?.let {
				binding.recipeDetailCategoryAndCuisine.visibility = View.VISIBLE
				binding.recipeDetailCategory.visibility = View.VISIBLE
				binding.recipeDetailCategory.text = it
				val colors = resources.obtainTypedArray(R.array.material_colors_700)
				binding.recipeDetailCategory.chipBackgroundColor = colors.getColorStateList(Random(it.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size))
				colors.recycle()
			}
			recipe.cuisine?.let {
				binding.recipeDetailCategoryAndCuisine.visibility = View.VISIBLE
				binding.recipeDetailCuisine.visibility = View.VISIBLE
				binding.recipeDetailCuisine.text = it
				val colors = resources.obtainTypedArray(R.array.material_colors_700)
				binding.recipeDetailCuisine.chipBackgroundColor = colors.getColorStateList(Random(it.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size))
				colors.recycle()
			}
			recipe.rating?.let {
				binding.recipeDetailRating.visibility = View.VISIBLE
				binding.recipeDetailRating.rating = it
			}
			recipe.cooktime?.let {
				binding.recipeDetailTimes.visibility = View.VISIBLE
				binding.recipeDetailCooktime.visibility = View.VISIBLE
				binding.recipeDetailCooktimeText.text = it
			}
			recipe.preptime?.let {
				binding.recipeDetailTimes.visibility = View.VISIBLE
				binding.recipeDetailPreptime.visibility = View.VISIBLE
				binding.recipeDetailPreptimeText.text = it
			}
			recipe.source?.let {
				binding.recipeDetailSourceAndLink.visibility = View.VISIBLE
				binding.recipeDetailSource.visibility = View.VISIBLE
				binding.recipeDetailSourceText.text = it
			}
			recipe.link?.let {
				binding.recipeDetailSourceAndLink.visibility = View.VISIBLE
				binding.recipeDetailLink.visibility = View.VISIBLE
				binding.recipeDetailLinkText.text = it
			}
			recipe.yields?.let {
				binding.recipeDetailYields.visibility = View.VISIBLE
				binding.recipeDetailYieldsText.text = it
			}
			recipe.ingredientList?.let { list ->
				binding.recipeDetailIngredients.visibility = View.VISIBLE
				binding.recipeDetailIngredientsAmounts.adapter = IngredientListAdapter(list.map { it.amount })
				binding.recipeDetailIngredientsUnits.adapter = IngredientListAdapter(list.map { it.unit })
				binding.recipeDetailIngredientsItems.adapter = IngredientListAdapter(list.map {
					if (it.optional == true)
						getString(R.string.optional, it.item)
					else
						it.item
				})
			}
			recipe.instructions?.let {
				binding.recipeDetailInstructions.visibility = View.VISIBLE
				binding.recipeDetailInstructionsText.text = it.replace("\n", "&lt;br/>").parseAsHtml().toString().parseAsHtml()
			}
			recipe.modifications?.let {
				binding.recipeDetailNotes.visibility = View.VISIBLE
				binding.recipeDetailNotesText.text = it.replace("\n", "&lt;br/>").parseAsHtml().toString().parseAsHtml()
			}
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressed()
		return true
	}

}