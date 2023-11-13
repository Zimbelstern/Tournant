package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.AlarmClock
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
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
import eu.zimbelstern.tournant.parseLocalFormattedFloat
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
import java.util.Calendar
import kotlin.random.Random

class RecipeActivity : AppCompatActivity(), IngredientTableAdapter.IngredientTableInterface, InstructionsTextAdapter.InstructionsTextInterface {

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

		if (resources.displayMetrics.run { widthPixels / density } > 600) {
			binding.recipeDetailImageDrawable.apply {
				layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.CENTER_CROP
			}
		}

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
					recipe.instructions?.let {
						binding.recipeDetailInstructions.visibility = View.VISIBLE
						binding.recipeDetailInstructionsRecycler.adapter = InstructionsTextAdapter(this@RecipeActivity, it)
					}
					recipe.yieldValue.let {
						binding.recipeDetailYields.visibility = View.VISIBLE
						binding.recipeDetailYieldsValue.apply {
							keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
							hint = (it ?: 1f).toStringForCooks(thousands = false)
							if (it != null) {
								text = SpannableStringBuilder(hint)
							}
							fillYieldsUnit(it, recipe.yieldUnit)
							addTextChangedListener { editable ->
								val scaleFactor = editable.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toFloatOrNull()?.div(recipe.yieldValue ?: 1f)
								recipeWithIngredients.ingredients.scale(scaleFactor).let { list ->
									binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list, scaleFactor)
									binding.recipeDetailInstructionsRecycler.adapter = recipe.instructions?.let {
										InstructionsTextAdapter(this@RecipeActivity, it, list, scaleFactor)
									}
									fillYieldsUnit(recipe.yieldValue, recipe.yieldUnit)
								}
							}
							setOnFocusChangeListener { _, hasFocus ->
								if (!hasFocus) {
									(getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(binding.root.windowToken, 0)
								}
							}
							savedInstanceState?.getString("YIELD_VALUE")?.let {
								if (it.isNotEmpty()) {
									setText(it)
								}
							}
						}
					}
					recipe.notes?.let {
						binding.recipeDetailNotes.visibility = View.VISIBLE
						binding.recipeDetailNotesText.text = it.parseAsHtml()
					}
				}
				recipeWithIngredients.ingredients.let { list ->
					binding.recipeDetailIngredients.visibility = View.VISIBLE
					if (savedInstanceState?.getString("YIELD_VALUE").isNullOrEmpty()) {
						binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list)
					} else {
						savedInstanceState?.putString("YIELD_VALUE", null)
					}
				}
				if (intent.hasExtra("RECIPE_YIELD_AMOUNT")) {
					val requestedYieldAmount = intent.getFloatExtra("RECIPE_YIELD_AMOUNT", 0f)
					val requestedYieldUnit = intent.getStringExtra("RECIPE_YIELD_UNIT")
					if (requestedYieldUnit.isNullOrEmpty() && recipeWithIngredients.recipe.yieldUnit != null) {
						scale(requestedYieldAmount)
					}
					else if (requestedYieldUnit == recipeWithIngredients.recipe.yieldUnit) {
						binding.recipeDetailYieldsValue.setText(requestedYieldAmount.toStringForCooks(thousands = false))
					}
					intent.removeExtra("RECIPE_YIELD_AMOUNT")
				}
				binding.recipeDetailLess.setOnClickListener {
					binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
						RecipeUtils.lessYield(
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toFloatOrNull()
								?: recipeWithIngredients.recipe.yieldValue ?: 1f
						).toStringForCooks(thousands = false)
					)
				}
				binding.recipeDetailMore.setOnClickListener {
					binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
						RecipeUtils.moreYield(
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toFloatOrNull()
								?: recipeWithIngredients.recipe.yieldValue ?: 1f
						).toStringForCooks(thousands = false))
				}
				binding.recipeDetailReset.setOnClickListener {
					binding.recipeDetailYieldsValue.setText("")
				}
				binding.recipeDetailCopy.apply {
					if (recipeWithIngredients.ingredients.isEmpty())
						visibility = View.GONE
					else {
						visibility = View.VISIBLE
						setOnClickListener {
							val textToCopy = (binding.recipeDetailIngredientsRecycler.adapter as IngredientTableAdapter).ingredientsToString()
							(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(getString(R.string.ingredients), textToCopy))
							Toast.makeText(this@RecipeActivity, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.dependentRecipes.collectLatest { recipeTitleIdList ->
				if (recipeTitleIdList.isNotEmpty()) {
					binding.recipeDetailDependentRecipes.visibility = View.VISIBLE
					binding.recipeDetailDependentRecipesText.movementMethod = LinkMovementMethod.getInstance()
					binding.recipeDetailDependentRecipesText.text =
						recipeTitleIdList.joinTo(SpannableStringBuilder(), "\n") {
							SpannableString(it.title).apply {
								setSpan(
									object : ClickableSpan() {
										override fun onClick(widget: View) {
											startActivity(Intent(this@RecipeActivity, RecipeActivity::class.java).apply {
												putExtra("RECIPE_ID", it.id)
											})
										}
									},
									0,
									it.title.length,
									Spanned.SPAN_INCLUSIVE_EXCLUSIVE
								)
							}
					}
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

	override fun openRecipe(refId: Long, yieldAmount: Float?, yieldUnit: String?) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
			if (yieldAmount != null) {
				putExtra("RECIPE_YIELD_AMOUNT", yieldAmount)
				putExtra("RECIPE_YIELD_UNIT", yieldUnit)
			}
		})
	}

	override fun scale(scaleFactor: Float) {
		binding.root.clearFocus()
		val oldYieldValue = binding.recipeDetailYieldsValue.hint.toString().parseLocalFormattedFloat() ?: 1f
		binding.recipeDetailYieldsValue.setText((oldYieldValue * scaleFactor).toStringForCooks(thousands = false))
	}

	override fun setAlarm(minutes: Int) {
		try {
			val calendar = Calendar.getInstance().apply {
				add(Calendar.MINUTE, minutes)
			}
			startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
				putExtra(AlarmClock.EXTRA_MESSAGE, binding.recipe?.title)
				putExtra(AlarmClock.EXTRA_HOUR, calendar[Calendar.HOUR_OF_DAY])
				putExtra(AlarmClock.EXTRA_MINUTES, calendar[Calendar.MINUTE])
			})
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_suitable_application, Toast.LENGTH_LONG).show()
		}
	}

	override fun startTimer(seconds: Int) {
		try {
			startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
				putExtra(AlarmClock.EXTRA_MESSAGE, binding.recipe?.title)
				putExtra(AlarmClock.EXTRA_LENGTH, seconds)
				putExtra(AlarmClock.EXTRA_SKIP_UI, true)
			})
			val timeString = if (seconds >= 60)
				"%02d".format(seconds / 60) + ":" + "%02d".format(seconds % 60) + " min"
			else
				"$seconds s"
			Toast.makeText(this, getString(R.string.timer_set, timeString), Toast.LENGTH_SHORT).show()
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_suitable_application, Toast.LENGTH_LONG).show()
		}
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

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putString("YIELD_VALUE", binding.recipeDetailYieldsValue.text.toString())
		super.onSaveInstanceState(outState)
	}

}