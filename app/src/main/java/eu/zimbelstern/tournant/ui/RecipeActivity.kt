package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.text.parseAsHtml
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import eu.zimbelstern.tournant.BuildConfig
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_SCREEN_ON
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeUtils
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityRecipeBinding
import eu.zimbelstern.tournant.getQuantityIntForPlurals
import eu.zimbelstern.tournant.scale
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.ui.adapter.IngredientTableAdapter
import eu.zimbelstern.tournant.ui.adapter.InstructionsTextAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormatSymbols
import kotlin.random.Random

class RecipeActivity : AppCompatActivity() {

	companion object {
		private const val TAG = "RecipeActivity"
	}

	private lateinit var binding: ActivityRecipeBinding
	private val viewModel: RecipeViewModel by viewModels {
		RecipeViewModelFactory(
			application as TournantApplication,
			intent.getLongExtra("RECIPE_ID", 0L)
		)
	}

	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (!intent.hasExtra("RECIPE_ID")) {
			Log.e(TAG, "No recipe provided")
			finish()
			return
		}

		binding = ActivityRecipeBinding.inflate(layoutInflater)
		setContentView(binding.root)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getBoolean(PREF_SCREEN_ON, true))
			window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		binding.recipeDetailPreptime.updateLayoutParams<LinearLayout.LayoutParams> {
			weight = getString(R.string.preptime).length.toFloat()
		}

		binding.recipeDetailCooktime.updateLayoutParams<LinearLayout.LayoutParams> {
			weight = getString(R.string.cooktime).length.toFloat()
		}

		lifecycleScope.launch {
			viewModel.recipe.collectLatest { recipeWithIngredients ->
				recipeWithIngredients.recipe.let { recipe ->
					binding.recipe = recipe
					title = recipe.title
					binding.recipeDetailImage.visibility = recipe.image.let { image ->
						val imageFile = File(File(application.filesDir, "images"), "${recipe.id}.jpg")
						if (imageFile.exists()) {
							Glide.with(this@RecipeActivity)
								.load(File(File(application.filesDir, "images"), "${recipe.id}.jpg"))
								.signature(ObjectKey(imageFile.lastModified()))
								.into(binding.recipeDetailImageDrawable)
							View.VISIBLE
						}
						else if (image != null) {
							binding.recipeDetailImageDrawable.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.size))
							View.VISIBLE
						}
						else {
							View.GONE
						}
					}
					recipe.category?.let {
						val colors = resources.obtainTypedArray(R.array.material_colors_700)
						binding.recipeDetailCategory.chipBackgroundColor = colors.getColorStateList(Random(it.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size))
						colors.recycle()
					}
					recipe.cuisine?.let {
						val colors = resources.obtainTypedArray(R.array.material_colors_700)
						binding.recipeDetailCuisine.chipBackgroundColor = colors.getColorStateList(Random(it.hashCode()).nextInt(resources.getStringArray(R.array.material_colors_700).size))
						colors.recycle()
					}
					recipe.yieldValue.let {
						binding.recipeDetailYields.visibility = View.VISIBLE
						binding.recipeDetailYieldsValue.apply {
							keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
							hint = (it ?: 1f).toStringForCooks()
							if (it != null) {
								text = SpannableStringBuilder(hint)
							}
							fillYieldsUnit(it, recipe.yieldUnit)
							addTextChangedListener { editable ->
								val scaleFactor = editable.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, ".".single()).toFloatOrNull()?.div(recipe.yieldValue ?: 1f)
								recipeWithIngredients.ingredients.scale(scaleFactor).let { list ->
									binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list, scaleFactor)
									fillYieldsUnit(recipe.yieldValue, recipe.yieldUnit)
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
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, ".".single()).toFloatOrNull()
								?: recipeWithIngredients.recipe.yieldValue ?: 1f
						).toStringForCooks()
					)
				}
				binding.recipeDetailMore.setOnClickListener {
					binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
						RecipeUtils.moreYield(
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, ".".single()).toFloatOrNull()
								?: recipeWithIngredients.recipe.yieldValue ?: 1f
						).toStringForCooks())
				}
			}
		}
	}

	private fun fillYieldsUnit(value: Float?, unit: String?) {
		binding.recipeDetailYieldsText.text = if (value != null) {
			unit
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

	fun findReferencedRecipe(refId: Long) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
		})
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options_recipe, menu)
		if (application.getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getInt(PREF_MODE, 0) == MODE_SYNCED)
			menu.removeItem(R.id.edit)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.share -> {
				lifecycleScope.launch {
					withContext(Dispatchers.IO) {
						val filename = binding.recipeDetailTitle.text.toString()
							.ifBlank { getString(R.string.recipe) }
						viewModel.writeRecipesToExportDir(filename)
						val uri = FileProvider.getUriForFile(
							application,
							BuildConfig.APPLICATION_ID + ".fileprovider",
							File(File(filesDir, "export"), "$filename.xml")
						)
						ShareCompat.IntentBuilder(this@RecipeActivity)
							.setStream(uri)
							.setType("application/xml")
							.startChooser()
					}
				}
				true
			}
			R.id.edit -> {
				startActivity(Intent(this, RecipeEditingActivity::class.java).apply {
					putExtra("RECIPE_ID", intent.getLongExtra("RECIPE_ID", 0L))
				})
				true
			}
			android.R.id.home -> {
				finish()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

}