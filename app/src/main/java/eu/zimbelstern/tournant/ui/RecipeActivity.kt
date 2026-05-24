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
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Scale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientLine
import eu.zimbelstern.tournant.data.IngredientLine.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine.IngredientItem
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.databinding.ActivityRecipeBinding
import eu.zimbelstern.tournant.databinding.InputFieldTimeBinding
import eu.zimbelstern.tournant.databinding.RecyclerPreparationsBinding
import eu.zimbelstern.tournant.getAppOrSystemLocale
import eu.zimbelstern.tournant.getQuantityIntForPlurals
import eu.zimbelstern.tournant.safeInsets
import eu.zimbelstern.tournant.separator
import eu.zimbelstern.tournant.shiftToLocalDayStart
import eu.zimbelstern.tournant.splitLines
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.ui.adapter.InstructionsTextAdapter
import eu.zimbelstern.tournant.ui.adapter.PreparationsAdapter
import eu.zimbelstern.tournant.ui.elements.TournantCard
import eu.zimbelstern.tournant.ui.elements.TournantRoundIconButton
import eu.zimbelstern.tournant.ui.elements.TournantRoundedIconButton
import eu.zimbelstern.tournant.ui.elements.TournantUnderlinedTextField
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecipeActivity : AppCompatActivity(), InstructionsTextAdapter.InstructionsTextInterface, PreparationsAdapter.PreparationsInterface {

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

		val keywords = viewModel.recipe.map { it.keywords }
		@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
		binding.recipeDetailKeywords.setContent {
			CompositionLocalProvider(LocalRippleConfiguration provides null) {
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					keywords.collectAsState(emptyList()).value.forEach {
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

		val months = viewModel.recipe.map { it.season?.getIncludedMonths() }
		binding.recipeDetailSeason.setContent {
			months.collectAsState(null).value?.let { months ->
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
				recipe.notes?.let {
					binding.recipeDetailNotes.visibility = View.VISIBLE
					binding.recipeDetailNotesText.movementMethod = LinkMovementMethod.getInstance()
					binding.recipeDetailNotesText.text = parseRecipeText(it)
				}
				if (intent.hasExtra("RECIPE_YIELD_AMOUNT")) {
					val requestedYieldAmount = intent.getDoubleExtra("RECIPE_YIELD_AMOUNT", 0.0)
					val requestedYieldUnit = intent.getStringExtra("RECIPE_YIELD_UNIT")
					if (requestedYieldUnit.isNullOrEmpty() && recipe.yieldUnit != null) {
						viewModel.scale(requestedYieldAmount * (recipe.yieldValue ?: 1.0))
					}
					else if (requestedYieldUnit == recipe.yieldUnit) {
						viewModel.scale(requestedYieldAmount)
					}
					intent.removeExtra("RECIPE_YIELD_AMOUNT")
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
								recipe.preparations.last().shiftToLocalDayStart().time,
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
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiEvents.collect { event ->
					when (event) {
						UiEvent.Shrug -> Toast.makeText(this@RecipeActivity, "\uD83E\uDD37", Toast.LENGTH_SHORT).show()
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

		binding.recipeDetailIngredients.setContent {
			val recipe by viewModel.recipe.collectAsState(Recipe.createEmpty())
			if (recipe.ingredients.isNotEmpty()) {
				TournantTheme {
					IngredientCard(recipe)
				}
			}
		}

	}

	@Composable
	fun IngredientCard(recipe: Recipe) {
		TournantCard(marginEnd = 16.dp, marginBottom = 16.dp) {
			val textMeasurer = rememberTextMeasurer()
			val weighingModeOn by viewModel.weighingModeOn.collectAsState(false)
			Column {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					Text(
						modifier = Modifier.weight(1f),
						text = stringResource(R.string.ingredients),
						style = MaterialTheme.typography.h2
					)
					TournantRoundIconButton(
						icon = Icons.Default.RepeatOne,
						isDark = true,
						onClick = { viewModel.scaleReset() },
						contentDescription = stringResource(R.string.reset)
					)
					TournantRoundIconButton(
						icon = Icons.Default.Remove,
						onClick = { viewModel.scaleDown() },
						contentDescription = stringResource(R.string.less)
					)
					TournantRoundIconButton(
						icon = Icons.Default.Add,
						onClick = { viewModel.scaleUp() },
						contentDescription = stringResource(R.string.more)
					)
				}
				Row(
					Modifier.padding(vertical = 16.dp)
				) {
					val yieldValue by viewModel.yieldValueScaled.collectAsState("")
					val placeholder = recipe.yieldValue.toStringForCooks(thousands = false)
					val italicTextStyle = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)
					Text(
						text = stringResource(R.string.yield),
						style = italicTextStyle
					)
					TournantUnderlinedTextField(
						value = yieldValue,
						onValueChange = {
							viewModel.scale(it)
						},
						textMeasurer = textMeasurer,
						placeholder = placeholder.takeIf { it.isNotEmpty() } ?: "1"
					)
					Text(
						recipe.yieldUnit
							?: pluralStringResource(
								R.plurals.lots,
								(yieldValue.takeIf { it.isNotEmpty() } ?: placeholder).getQuantityIntForPlurals() ?: 3
							),
						style = italicTextStyle
					)
				}
				Row {
					val items by viewModel.ingredientsScaled.collectAsState(listOf())
					IngredientList(
						items,
						Modifier.weight(1f),
						textMeasurer,
						weighingModeOn
					)
					Column(
						verticalArrangement = Arrangement.spacedBy(8.dp),
						horizontalAlignment = Alignment.End
					) {
						TournantRoundedIconButton(
							icon = Icons.Default.ContentCopy,
							onClick = {
								(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
									ClipData.newPlainText(
										getString(R.string.ingredients),
										items.map { it.toStringForCooks(getString(R.string.optional)) }.joinToString("\n")
									)
								)
								Toast.makeText(
									this@RecipeActivity,
									getString(R.string.copied_to_clipboard),
									Toast.LENGTH_SHORT
								).show()
							},
							contentDescription = stringResource(R.string.copy_to_clipboard)
						)
						TournantRoundedIconButton(
							icon = Icons.Default.Scale,
							onClick = { viewModel.toggleWeighingMode() },
							contentDescription = stringResource(R.string.weigh),
							isDark = weighingModeOn
						)
						val weight by viewModel.ingredientWeight.collectAsState(0.0)
						if (weighingModeOn) {
							Text("${weight.toStringForCooks()} g", overflow = TextOverflow.Visible, softWrap = false)
						}
					}
				}
			}
		}
	}

	@Composable
	fun IngredientList(
		items: List<IngredientLine>,
		modifier: Modifier = Modifier,
		textMeasurer: TextMeasurer = rememberTextMeasurer(),
		weighMode: Boolean
	) {
		val amountMaxWidth = with(LocalDensity.current) {
			items.filterIsInstance<IngredientItem>().maxOfOrNull {
				textMeasurer.measure(
					it.ingredient.amountToStringForCooks(),
					MaterialTheme.typography.body1
				).size.width.toDp()
			}
		}
		Column(modifier) {
			items.forEachIndexed { i, item ->
				when (item) {
					is IngredientGroupTitle -> Text(
						text = item.title ?: "",
						style = MaterialTheme.typography.caption,
						modifier = Modifier.padding(bottom = 4.dp).clickable {
							item.title?.let { viewModel.toggleChecked(it) }
						},
					)
					is IngredientItem -> IngredientDisplay(
						item = item,
						id = i,
						amountMaxWidth = amountMaxWidth ?: 0.dp,
						weighMode
					)
				}
			}
		}
	}

	@Preview(showBackground = true)
	@Composable
	fun IngredientListPreview() {
		IngredientList(Ingredient.createExamples(3).mapIndexed { i, ingredient -> IngredientItem(ingredient, i % 2 == 1) }, weighMode = false)
	}

	@OptIn(ExperimentalFoundationApi::class)
	@Composable
	fun IngredientDisplay(item: IngredientItem, id: Int, amountMaxWidth: Dp, weighMode: Boolean) {
		val interactionSource = remember { MutableInteractionSource() }
		var dialogVisible by remember { mutableStateOf(false) }
		var value by remember { mutableStateOf(TextFieldValue("")) }

		Row(
			Modifier
				.alpha(if (weighMode && item.isSelected || !weighMode && item.isChecked) ContentAlpha.disabled else 1f)
				.padding(vertical = 2.dp)
				.combinedClickable(
					onClick = { viewModel.toggleChecked(id) },
					onLongClick = {
						if (item.ingredient.amount != null) {
							value = TextFieldValue(item.ingredient.amount.toStringForCooks(thousands = false))
							dialogVisible = true
						}
					},
					indication = null,
					interactionSource = interactionSource
				)
		) {
			Text(
				text = item.ingredient.amountToStringForCooks(),
				modifier = Modifier.width(amountMaxWidth),
				textAlign = TextAlign.End,
				lineHeight = 24.sp
			)
			val baseItemString = buildAnnotatedString {
				if (item.ingredient.refId == null) {
					append(item.ingredient.item)
				}
				else {
					pushStringAnnotation("LINK_TO_RECIPE", item.ingredient.refId.toString())
					withStyle(SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)) {
						append(item.ingredient.item)
					}
					pop()
				}
			}
			val fullItemString = buildAnnotatedString {
				if (item.ingredient.optional) {
					append(stringResource(R.string.optional, baseItemString))
				} else {
					append(baseItemString)
				}
				if (!weighMode && item.isChecked) {
					withStyle(SpanStyle(fontSize = 14.sp, baselineShift = BaselineShift(0.1f))) {
						append(" ✓")
					}
				}
			}
			val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
			Text(
				text = fullItemString,
				modifier = Modifier.pointerInput(Unit) {
					detectTapGestures(
						onPress = {
							layoutResult.value?.getOffsetForPosition(it)?.let { offset ->
								val annotations = baseItemString.getStringAnnotations(
									tag = "LINK_TO_RECIPE",
									start = offset,
									end = offset
								)
								if (annotations.isNotEmpty()) {
									item.ingredient.refId?.let { refId ->
										openRecipe(refId = refId, item.ingredient.amount, item.ingredient.unit)
									}
								} else {
									viewModel.toggleChecked(id)
								}
							}
						},
						onLongPress = {
							value = TextFieldValue(item.ingredient.amount.toStringForCooks(thousands = false))
							dialogVisible = true
						}
					)
				},
				onTextLayout = { layoutResult.value = it },
				lineHeight = 24.sp
			)
		}
		
		if (dialogVisible) {
			val focusRequester = remember { FocusRequester() }
			AlertDialog(
				title = { Text(stringResource(R.string.scale_to), style = MaterialTheme.typography.h2) },
				text = {
					OutlinedTextField(
						modifier = Modifier.focusRequester(focusRequester),
						value = value,
						onValueChange = { newValue ->
							if (newValue.text.all { it.isDigit() || it == separator } && newValue.text.count { it == separator } <= 1) {
								value = newValue
							}
										},
						label = { Text(item.ingredient.item ?: "") },
						trailingIcon = { Text(item.ingredient.unit ?: "", style = MaterialTheme.typography.body1) },
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
						singleLine = true,
						textStyle = MaterialTheme.typography.body1
					)
				},
				confirmButton = {
					TextButton(
						onClick = {
							viewModel.scale(id, value.text)
							dialogVisible = false
						}
					) {
						Text(stringResource(R.string.ok))
					}
				},
				dismissButton = {
					TextButton(
						onClick = { dialogVisible = false }
					) {
						Text(stringResource(R.string.cancel))
					}
				},
				onDismissRequest = { dialogVisible = false },
			)
			LaunchedEffect(Unit) {
				value = value.copy(selection = TextRange(value.text.length))
				focusRequester.requestFocus()
			}
		}
	}

	// Parses text as markdown or html (if markwon instance null, that depends on user preference)
	private fun parseRecipeText(text: String): Spanned {
		return markwon?.toMarkdown(text) ?: text.replace("\n", "<br/>").parseAsHtml()
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

	fun openRecipe(refId: Long, yieldAmount: Double?, yieldUnit: String?) {
		startActivity(Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", refId)
			if (yieldAmount != null) {
				putExtra("RECIPE_YIELD_AMOUNT", yieldAmount)
				putExtra("RECIPE_YIELD_UNIT", yieldUnit)
			}
		})
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

	override fun removePreparation(date: Date) {
		viewModel.removePreparation(date)
	}
}