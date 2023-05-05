package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.parseAsHtml
import androidx.core.widget.addTextChangedListener
import eu.zimbelstern.tournant.Constants.Companion.PREF_SCREEN_ON
import eu.zimbelstern.tournant.IngredientTableAdapter
import eu.zimbelstern.tournant.InstructionsTextAdapter
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeUtils
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityRecipeBinding
import eu.zimbelstern.tournant.getQuantityIntForPlurals
import eu.zimbelstern.tournant.scale
import eu.zimbelstern.tournant.toStringForCooks
import kotlin.random.Random

class RecipeActivity : AppCompatActivity() {

	companion object {
		private const val TAG = "Activity RecipeDetail"
	}

	private lateinit var binding: ActivityRecipeBinding
	private val viewModel: RecipeViewModel by viewModels {
		RecipeViewModelFactory(
			(application as TournantApplication).database.recipeDao()
		)
	}

	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		intent.getLongExtra("RECIPE_ID", 0).let {
			if (it > 0)
				viewModel.pullRecipe(it)
			else {
				Log.e(TAG, "No recipe provided")
				finish()
				return
			}
		}

		binding = ActivityRecipeBinding.inflate(layoutInflater)
		setContentView(binding.root)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getBoolean(PREF_SCREEN_ON, true))
			window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		viewModel.recipe.observe(this) { recipeWithIngredients ->

			if (recipeWithIngredients == null) {
				Toast.makeText(applicationContext, getString(R.string.recipe_not_found), Toast.LENGTH_LONG).show()
				finish()
				return@observe
			}

			recipeWithIngredients.recipe.let { recipe ->
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
					binding.recipeDetailCooktimeText.text = "$it'"
				}
				recipe.preptime?.let {
					binding.recipeDetailTimes.visibility = View.VISIBLE
					binding.recipeDetailPreptime.visibility = View.VISIBLE
					binding.recipeDetailPreptimeText.text = "$it'"
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
				recipe.yieldValue.let {
					binding.recipeDetailYields.visibility = View.VISIBLE
					binding.recipeDetailYieldsValue.apply {
						hint = (it ?: 1f).toStringForCooks()
						if (it != null) {
							text = SpannableStringBuilder(hint)
						}
						fillYieldsUnit()
						addTextChangedListener { editable ->
							val scaleFactor = editable.toString().toFloatOrNull()?.div(recipe.yieldValue ?: 1f)
							recipeWithIngredients.ingredients.scale(scaleFactor).let { list ->
								binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list, scaleFactor)
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
				recipe.instructions?.let {
					binding.recipeDetailInstructions.visibility = View.VISIBLE
					binding.recipeDetailInstructionsRecycler.adapter = InstructionsTextAdapter(it)
				}
				recipe.notes?.let {
					binding.recipeDetailNotes.visibility = View.VISIBLE
					binding.recipeDetailNotesText.text = it.parseAsHtml()
				}
			}
			recipeWithIngredients.ingredients.let { list ->
				binding.recipeDetailIngredients.visibility = View.VISIBLE
				binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list)
			}
			binding.recipeDetailLess.setOnClickListener {
				binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
					RecipeUtils.lessYield(
						binding.recipeDetailYieldsValue.text.toString().toFloatOrNull()
							?: viewModel.recipe.value?.recipe?.yieldValue ?: 1f
					).toStringForCooks()
				)
			}
			binding.recipeDetailMore.setOnClickListener {
				binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
					RecipeUtils.moreYield(
						binding.recipeDetailYieldsValue.text.toString().toFloatOrNull()
							?: viewModel.recipe.value?.recipe?.yieldValue ?: 1f
					).toStringForCooks())
			}
		}
	}

	private fun fillYieldsUnit() {
		viewModel.recipe.value?.recipe?.let { recipe ->
			binding.recipeDetailYieldsText.text = if (recipe.yieldValue != null) {
				recipe.yieldUnit
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
	}

	fun findReferencedRecipe(refId: Long) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
		})
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == android.R.id.home) {
			finish()
			true
		} else
			super.onOptionsItemSelected(item)
	}

}