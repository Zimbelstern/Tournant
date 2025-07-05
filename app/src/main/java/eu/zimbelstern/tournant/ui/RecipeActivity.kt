package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.text.InputFilter
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.method.DigitsKeyListener
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.parseAsHtml
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.addLastModifiedToFileCacheKey
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import eu.zimbelstern.tournant.BuildConfig
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MARKDOWN
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_SCREEN_ON
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeUtils
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityRecipeBinding
import eu.zimbelstern.tournant.databinding.InputFieldTimeBinding
import eu.zimbelstern.tournant.databinding.RecyclerPreparationsBinding
import eu.zimbelstern.tournant.getAppOrSystemLocale
import eu.zimbelstern.tournant.getQuantityIntForPlurals
import eu.zimbelstern.tournant.parseLocalFormattedDouble
import eu.zimbelstern.tournant.safeInsets
import eu.zimbelstern.tournant.scale
import eu.zimbelstern.tournant.splitLines
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.ui.adapter.IngredientTableAdapter
import eu.zimbelstern.tournant.ui.adapter.InstructionsTextAdapter
import eu.zimbelstern.tournant.ui.adapter.PreparationsAdapter
import eu.zimbelstern.tournant.utils.RecipeMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecipeActivity : AppCompatActivity(), IngredientTableAdapter.IngredientTableInterface, InstructionsTextAdapter.InstructionsTextInterface, PreparationsAdapter.PreparationsInterface {

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

	private val markwon: Markwon? by lazy {
		if (getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getBoolean(PREF_MARKDOWN, true)) {
			Markwon.builder(this)
				.usePlugin(HtmlPlugin.create())
				.usePlugin(RecipeMarkwonPlugin(this))
				.usePlugin(SoftBreakAddsNewLinePlugin.create())
				.build()
		}
		else null
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

		enableEdgeToEdge()
		ViewGroupCompat.installCompatInsetsDispatch(window.decorView.rootView)

		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
			Log.d(TAG, "setOnApplyWindowInsetsListener(content)")
			view.updateLayoutParams<MarginLayoutParams> {
				topMargin = windowInsets.safeInsets().top
				bottomMargin = windowInsets.safeInsets().bottom
			}
			view.updatePadding(
				left = windowInsets.safeInsets().left,
				right = windowInsets.safeInsets().right,
			)
			WindowInsetsCompat.CONSUMED
		}

		@Suppress("DEPRECATION")
		if (Build.VERSION.SDK_INT < 35) {
			window.navigationBarColor = ContextCompat.getColor(this, R.color.bar_color)
		}

		setContentView(binding.root)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		if (getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getBoolean(PREF_SCREEN_ON, true))
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

		@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
		binding.recipeDetailKeywords.setContent {
			CompositionLocalProvider(LocalRippleConfiguration provides null) {
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					viewModel.recipe
						.map { it.keywords }
						.collectAsState(emptyList())
						.value.forEach {
							Chip(
								modifier = Modifier.height(24.dp),
								onClick = {},
								colors = ChipDefaults.chipColors(backgroundColor = materialColors100.getRandom(it)),
								border = BorderStroke(2.dp, materialColors200.getRandom(it)),
								shape = RoundedCornerShape(4.dp)
							) {
								Text(text = it, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 24.dp))
							}
						}
				}
			}
		}

		binding.recipeDetailSeason.setContent {
			viewModel.recipe.map { it.season?.getIncludedMonths() }.collectAsState(null).value?.let { months ->
				TournantTheme {
					Surface {
						Column {
							Text(
								stringResource(R.string.season),
								color = colorResource(R.color.heading_color),
								fontFamily = FontFamily(Font(R.font.quicksand_bold)),
								fontSize = 18.sp
							)

							val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

							val monthNamesThreeLetters = Calendar.getInstance().run {
								(0..11).map {
									set(Calendar.MONTH, it)
									getDisplayName(
										Calendar.MONTH,
										Calendar.SHORT,
										getAppOrSystemLocale()
									)
									?.take(3) ?: ""
								}
							}

							val maxSpaceThreeLetters = TextMeasurer(
								LocalFontFamilyResolver.current,
								LocalDensity.current,
								LocalLayoutDirection.current
							).run {
								monthNamesThreeLetters.maxOf {
									measure(it, LocalTextStyle.current.copy(fontSize = 12.sp)).size.width
								} + 2 * resources.displayMetrics.density
							}

							val availableSpace = remember { mutableIntStateOf(0) }

							val useThreeLetters = remember { derivedStateOf { maxSpaceThreeLetters * 12 <= availableSpace.intValue } }

							Row(
								Modifier
									.fillMaxWidth()
									.onGloballyPositioned {
										availableSpace.intValue = it.size.width
									}
							) {
								monthNamesThreeLetters.forEachIndexed { i, monthName ->
									Column(
										Modifier.weight(1f),
										horizontalAlignment = Alignment.CenterHorizontally
									) {
										Box(
											Modifier
												.padding(bottom = 2.dp)
												.alpha(if (i in months) 1f else .3f)
										) {
											Text(
												text = when {
													useThreeLetters.value -> monthName
													monthName.first().isDigit() -> monthName.takeWhile { it.isDigit() }
													else -> monthName.take(1)
												},
												fontSize = 12.sp
											)
											if (i == currentMonth)
												Box(
													Modifier
														.size(4.dp)
														.align(Alignment.BottomCenter)
														.clip(CircleShape)
														.background(if (i in months) MaterialTheme.colors.primary else Color.Gray.copy(.3f))
												)
										}
										if (i in months) {
											Box(
												Modifier
													.height(4.dp)
													.fillMaxWidth()
													.clip(
														RoundedCornerShape(
															topStartPercent = if (i - 1 !in months) 50 else 0,
															bottomStartPercent = if (i - 1 !in months) 50 else 0,
															topEndPercent = if (i + 1 !in months) 50 else 0,
															bottomEndPercent = if (i + 1 !in months) 50 else 0
														)
													)
													.background(materialColors700[(i + 5) % 14])
											)
										}
									}
								}
							}
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.recipe.collectLatest { recipe ->
				binding.recipe = recipe
				title = recipe.title
				val lang = if (Build.VERSION.SDK_INT >= 26)
					Locale.lookupTag(Locale.LanguageRange.parse(recipe.language.toLanguageTag() + ";q=1.0"), getString(R.string.availableLanguages).split(",")) ?: recipe.language.toLanguageTag()
				else recipe.language.toLanguageTag()
				val timeStrings = getString(R.string.localisedTimeStrings).split(";").find { it.substringBefore(":") == lang }?.split(":") ?: List(5) { "" }
				val dashWords = timeStrings[1].ifEmpty { getString(R.string.to) }
				val hString = timeStrings[2].ifEmpty { getString(R.string.hours_for_regex) }
				val minString = timeStrings[3].ifEmpty { getString(R.string.minutes_for_regex) }
				val sString = timeStrings[4].ifEmpty { getString(R.string.seconds_for_regex) }
				binding.recipeDetailImage.visibility = recipe.image.let { image ->
					val imageFile = File(File(application.filesDir, "images"), "${recipe.id}.jpg")
					if (imageFile.exists()) {
						binding.recipeDetailImageDrawable.load(File(File(application.filesDir, "images"), "${recipe.id}.jpg")) {
							addLastModifiedToFileCacheKey(true)
						}
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
					binding.recipeDetailCategory.chipBackgroundColor = ColorStateList.valueOf(materialColors700.getRandom(it).toArgb())
				}
				recipe.cuisine?.let {
					binding.recipeDetailCuisine.chipBackgroundColor = ColorStateList.valueOf(materialColors900.getRandom(it).toArgb())
				}
				recipe.instructions?.let {
					binding.recipeDetailInstructions.visibility = View.VISIBLE
					binding.recipeDetailInstructionsRecycler.adapter = InstructionsTextAdapter(
						this@RecipeActivity,
						parseRecipeText(it).splitLines(),
						dashWords = dashWords,
						hString = hString,
						minString = minString,
						sString = sString
					)
				}
				recipe.yieldValue.let { yieldValue ->
					binding.recipeDetailYields.visibility = View.VISIBLE
					binding.recipeDetailYieldsValue.apply {
						keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
						hint = (yieldValue ?: 1.0).toStringForCooks(thousands = false)
						if (yieldValue != null) {
							text = SpannableStringBuilder(hint)
						}
						fillYieldsUnit(yieldValue, recipe.yieldUnit)
						addTextChangedListener { editable ->
							val scaleFactor = editable.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toDoubleOrNull()?.div(recipe.yieldValue ?: 1.0)
							recipe.ingredients.scale(scaleFactor).let { list ->
								binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list, scaleFactor)
								binding.recipeDetailInstructionsRecycler.adapter = recipe.instructions?.let {
									InstructionsTextAdapter(
										this@RecipeActivity,
										parseRecipeText(it).splitLines(),
										list,
										scaleFactor,
										dashWords,
										hString,
										minString,
										sString
									)
								}
								fillYieldsUnit(recipe.yieldValue, recipe.yieldUnit)
							}
						}
						setOnFocusChangeListener { _, hasFocus ->
							if (!hasFocus) {
								(getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(binding.root.windowToken, 0)
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
					binding.recipeDetailNotesText.movementMethod = LinkMovementMethod.getInstance()
					binding.recipeDetailNotesText.text = parseRecipeText(it)
				}
				recipe.ingredients.let { list ->
					binding.recipeDetailIngredients.visibility = View.VISIBLE
					if (savedInstanceState?.getString("YIELD_VALUE").isNullOrEmpty()) {
						binding.recipeDetailIngredientsRecycler.adapter = IngredientTableAdapter(this@RecipeActivity, list)
					} else {
						savedInstanceState.putString("YIELD_VALUE", null)
					}
				}
				if (intent.hasExtra("RECIPE_YIELD_AMOUNT")) {
					val requestedYieldAmount = intent.getDoubleExtra("RECIPE_YIELD_AMOUNT", 0.0)
					val requestedYieldUnit = intent.getStringExtra("RECIPE_YIELD_UNIT")
					if (requestedYieldUnit.isNullOrEmpty() && recipe.yieldUnit != null) {
						scale(requestedYieldAmount)
					}
					else if (requestedYieldUnit == recipe.yieldUnit) {
						binding.recipeDetailYieldsValue.setText(requestedYieldAmount.toStringForCooks(thousands = false))
					}
					intent.removeExtra("RECIPE_YIELD_AMOUNT")
				}
				binding.recipeDetailLess.setOnClickListener {
					binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
						RecipeUtils.lessYield(
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toDoubleOrNull()
								?: recipe.yieldValue ?: 1.0
						).toStringForCooks(thousands = false)
					)
				}
				binding.recipeDetailMore.setOnClickListener {
					binding.recipeDetailYieldsValue.text = SpannableStringBuilder(
						RecipeUtils.moreYield(
							binding.recipeDetailYieldsValue.text.toString().replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toDoubleOrNull()
								?: recipe.yieldValue ?: 1.0
						).toStringForCooks(thousands = false))
				}
				binding.recipeDetailReset.setOnClickListener {
					binding.recipeDetailYieldsValue.setText("")
				}
				binding.recipeDetailCopy.apply {
					if (recipe.ingredients.isEmpty())
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
				binding.recipeDetailPreparations.apply {
					if (recipe.preparations.isEmpty())
						visibility = View.GONE
					else {
						visibility = View.VISIBLE
						binding.recipeDetailPreparationsCount.text = resources.getQuantityString(
							R.plurals.prepared_times,
							recipe.preparations.size,
							recipe.preparations.size
						)
						binding.recipeDetailPreparationsTime.text = getString(
							R.string.last_time,
							DateUtils.getRelativeTimeSpanString(
								recipe.preparations.last().time,
								Date().time,
								DAY_IN_MILLIS
							)
						)
						val preparationsDialog = MaterialAlertDialogBuilder(this@RecipeActivity)
							.setTitle(R.string.prepared_on)
							.setView(
								RecyclerPreparationsBinding.inflate(layoutInflater).apply {
									preparationsRecycler.adapter = PreparationsAdapter(this@RecipeActivity, recipe.preparations.asReversed())
									preparationsRecycler.layoutManager = FlexboxLayoutManager(this@RecipeActivity)
								}.root
							)
							.setPositiveButton(R.string.ok) { _, _ -> }
							.create()
						setOnClickListener {
							preparationsDialog.show()
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.recipeDates.collectLatest { (created, modified) ->
				created?.let {
					binding.recipeDetailCreated.visibility = View.VISIBLE
					binding.recipeDetailCreatedDate.text = it
				}
				modified?.let {
					binding.recipeDetailModified.visibility = View.VISIBLE
					binding.recipeDetailModifiedDate.text = it
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

	// Parses text as markdown or html (if markwon instance null, that depends on user preference)
	private fun parseRecipeText(text: String): Spanned {
		return markwon?.toMarkdown(text) ?: text.replace("\n", "<br/>").parseAsHtml()
	}

	private fun fillYieldsUnit(value: Double?, unit: String?) {
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

	private fun shareRecipe(format: String) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				val filename = binding.recipeDetailTitle.text.toString().ifBlank { getString(R.string.recipe) }
				(application as TournantApplication).writeRecipesToExportDir(setOf(intent.getLongExtra("RECIPE_ID", 0L)), filename, format)
				val uri = FileProvider.getUriForFile(
					application,
					BuildConfig.APPLICATION_ID + ".fileprovider",
					File(File(filesDir, "export"), "$filename.$format")
				)
				ShareCompat.IntentBuilder(this@RecipeActivity)
					.setStream(uri)
					.setType("application/$format")
					.startChooser()
			}
		}
	}

	override fun openRecipe(refId: Long, yieldAmount: Double?, yieldUnit: String?) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
			if (yieldAmount != null) {
				putExtra("RECIPE_YIELD_AMOUNT", yieldAmount)
				putExtra("RECIPE_YIELD_UNIT", yieldUnit)
			}
		})
	}

	override fun scale(scaleFactor: Double) {
		binding.root.clearFocus()
		val oldYieldValue = binding.recipeDetailYieldsValue.hint.toString().parseLocalFormattedDouble() ?: 1.0
		binding.recipeDetailYieldsValue.setText((oldYieldValue * scaleFactor).toStringForCooks(thousands = false))
	}

	override fun showAlarmDialog(minutes: Int) {
		val calendar = Calendar.getInstance().apply {
			add(Calendar.MINUTE, minutes)
		}
		MaterialTimePicker.Builder()
			.setTitleText(R.string.set_alarm)
			.setHour(calendar[Calendar.HOUR_OF_DAY])
			.setMinute(calendar[Calendar.MINUTE])
			.setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
			.setPositiveButtonText(R.string.ok)
			.setNegativeButtonText(R.string.cancel)
			.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
			.build()
			.also { picker -> picker.addOnPositiveButtonClickListener { setAlarm(picker.hour, picker.minute) } }
			.show(supportFragmentManager, "SetAlarmDialog")
	}

	private fun setAlarm(hour: Int, minute: Int) {
		try {
			startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
				putExtra(AlarmClock.EXTRA_MESSAGE, binding.recipe?.title)
				putExtra(AlarmClock.EXTRA_HOUR, hour)
				putExtra(AlarmClock.EXTRA_MINUTES, minute)
			})
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_suitable_application, Toast.LENGTH_LONG).show()
		}
	}

	@SuppressLint("SetTextI18n")
	override fun showTimerDialog(seconds: Int) {
		val customView = InputFieldTimeBinding.inflate(LayoutInflater.from(this), null, false).apply {
			minutesField.apply {
				doOnTextChanged { text, _, _, _ ->
					minutesMinus.isEnabled = ((text.toString().toIntOrNull() ?: 0) != 0)
					secondsMinus.isEnabled = (text.toString().toIntOrNull() ?: 0) != 0 || (secondsField.text.toString().toIntOrNull() ?: 0) != 0
				}
				setText((seconds / 60).toString())
			}
			secondsField.apply {
				doOnTextChanged { text, _, _, _ ->
					secondsMinus.isEnabled = (text.toString().toIntOrNull() ?: 0) != 0 || (minutesField.text.toString().toIntOrNull() ?: 0) != 0
				}
				setText((seconds % 60).toString())
			}
			secondsField.filters += InputFilter { source, _, _, dest, _, _ ->
				if (((dest.toString() + source.toString()).toIntOrNull() ?: 0) < 60) null else ""
			}
			minutesPlus.setOnClickListener {
				minutesField.setText(((minutesField.text.toString().toIntOrNull() ?: 0) + 1).toString())
			}
			minutesMinus.setOnClickListener {
				minutesField.setText((minutesField.text.toString().toInt() - 1).toString())
			}
			secondsPlus.setOnClickListener {
				val value = secondsField.text.toString().toIntOrNull() ?: 0
				if (value == 59) {
					secondsField.setText(0.toString())
					minutesPlus.performClick()
				}
				else {
					secondsField.setText(((secondsField.text.toString().toIntOrNull() ?: 0) + 1).toString())
				}
			}
			secondsMinus.setOnClickListener {
				val value = secondsField.text.toString().toIntOrNull() ?: 0
				if (value == 0) {
					minutesMinus.performClick()
					secondsField.setText(59.toString())
				}
				else {
					secondsField.setText((value - 1).toString())
				}
			}
		}
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.set_timer)
			.setView(customView.root)
			.setPositiveButton(R.string.ok) { _, _ ->
				val min = customView.minutesField.text.toString().toIntOrNull() ?: 0
				val s = customView.secondsField.text.toString().toIntOrNull() ?: 0
				if ((min + s) != 0)
					startTimer(min * 60 + s)
			}
			.setNegativeButton(R.string.cancel, null)
			.show()
	}

	private fun startTimer(seconds: Int) {
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

	private fun logPreparation() {
		MaterialDatePicker.Builder.datePicker()
			.setCalendarConstraints(
				CalendarConstraints.Builder()
					.setValidator(DateValidatorPointBackward.now())
					.build()
			)
			.setTitleText(getString(R.string.prepared_on))
			.setSelection(MaterialDatePicker.todayInUtcMilliseconds())
			.build()
			.apply {
				addOnPositiveButtonClickListener {
					viewModel.addPreparation(Date(it))
				}
			}
			.show(supportFragmentManager, "DatePicker")
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options_recipe, menu)
		if (application.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE).getInt(PREF_MODE, 0) == MODE_SYNCED)
			menu.removeItem(R.id.edit)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.log_preparation -> { logPreparation(); true }
			R.id.share_json -> { shareRecipe("json"); true }
			R.id.share_zip -> { shareRecipe("zip"); true }
			R.id.share_gourmand -> {
				(application as TournantApplication).withGourmandIssueCheck(this, setOf(intent.getLongExtra("RECIPE_ID", 0L))) {
					shareRecipe("xml")
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

	override fun removePreparation(date: Date) {
		viewModel.removePreparation(date)
	}
}