package eu.zimbelstern.tournant.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import androidx.core.widget.addTextChangedListener
import eu.zimbelstern.tournant.*
import eu.zimbelstern.tournant.databinding.ActivityRecipeBinding
import eu.zimbelstern.tournant.gourmand.XmlRecipe
import kotlin.random.Random

class RecipeActivity : AppCompatActivity() {

	companion object {
		private const val TAG = "Activity RecipeDetail"
	}

	private lateinit var binding: ActivityRecipeBinding
	private lateinit var recipe: XmlRecipe

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		intent.extras?.getParcelable<XmlRecipe>("RECIPE").let {
			if (it == null) {
				Log.e(TAG, "No recipe provided")
				finish()
				return
			}
			recipe = it
		}

		binding = ActivityRecipeBinding.inflate(layoutInflater)
		setContentView(binding.root)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getBoolean("SCREEN_ON", true))
			window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
		recipe.yields.let {
			binding.recipeDetailYields.visibility = View.VISIBLE
			binding.recipeDetailYieldsValue.apply {
				hint = (recipe.getYieldsValue() ?: 1f).toStringForCooks()
				if (it != null) {
					text = SpannableStringBuilder(hint)
				}
				fillYieldsUnit()
				addTextChangedListener { editable ->
					recipe.ingredientList?.scale(editable.toString().toFloatOrNull()?.div(recipe.getYieldsValue() ?: 1f))?.let { list ->
						binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list)
						fillYieldsUnit()
					}
				}
				setOnFocusChangeListener { _, hasFocus ->
					if (!hasFocus) {
						(getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(binding.root.windowToken, 0)
					}
				}
			}
		}
		recipe.ingredientList?.let { list ->
			binding.recipeDetailIngredients.visibility = View.VISIBLE
			binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list)
		}
		recipe.instructions?.let {
			binding.recipeDetailInstructions.visibility = View.VISIBLE
			binding.recipeDetailInstructionsRecycler.adapter = InstructionsTextAdapter(it)
		}
		recipe.modifications?.let {
			binding.recipeDetailNotes.visibility = View.VISIBLE
			binding.recipeDetailNotesText.text = it.replace("\n", "&lt;br/>").parseAsHtml().toString().parseAsHtml()
		}
		binding.recipeDetailLess.setOnClickListener {
			binding.recipeDetailYieldsValue.text = SpannableStringBuilder(recipe.lessYield(binding.recipeDetailYieldsValue.text.toString().toFloatOrNull() ?: recipe.getYieldsValue() ?: 1f).toStringForCooks())
		}
		binding.recipeDetailMore.setOnClickListener {
			binding.recipeDetailYieldsValue.text = SpannableStringBuilder(recipe.moreYield(binding.recipeDetailYieldsValue.text.toString().toFloatOrNull() ?: recipe.getYieldsValue() ?: 1f).toStringForCooks())
		}
	}

	private fun fillYieldsUnit() {
		binding.recipeDetailYieldsText.text = if (recipe.getYieldsValue() != null) {
			recipe.getYieldsUnit()
				?: resources.getQuantityText(
					R.plurals.servings,
					binding.recipeDetailYieldsValue.text.toString().getQuantityIntForPlurals()
						?: binding.recipeDetailYieldsValue.hint.toString().getQuantityIntForPlurals()
						?: 0
				)
		}
		else {
			resources.getQuantityText(
				R.plurals.lots,
				binding.recipeDetailYieldsValue.text.toString().getQuantityIntForPlurals()
					?: binding.recipeDetailYieldsValue.hint.toString().getQuantityIntForPlurals()
					?: 0
			)
		}
	}

	fun findReferencedRecipe(refId: Int) {
		if (intent.extras?.getInt("FILE_MODE") == MainActivity.FILE_MODE_PREVIEW) {
			Toast.makeText(this, getString(R.string.not_available_in_preview), Toast.LENGTH_LONG).show()
		}
		else {
			startActivity(Intent(this, MainActivity::class.java).apply {
				putExtra("RECIPE_ID", refId)
			})
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == android.R.id.home) {
			finish()
			true
		} else
			super.onOptionsItemSelected(item)
	}

}