package eu.zimbelstern.tournant.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.addLastModifiedToFileCacheKey
import com.google.android.material.textfield.TextInputLayout
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.data.Season
import eu.zimbelstern.tournant.databinding.ActivityRecipeEditingBinding
import eu.zimbelstern.tournant.getAppOrSystemLocale
import eu.zimbelstern.tournant.move
import eu.zimbelstern.tournant.safeInsets
import eu.zimbelstern.tournant.ui.adapter.IngredientEditingAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

class RecipeEditingActivity : AppCompatActivity(), IngredientEditingAdapter.IngredientEditingInterface {

	companion object {
		private const val TAG = "RecipeEditingActivity"
	}

	private lateinit var binding: ActivityRecipeEditingBinding
	private val viewModel: RecipeEditingViewModel by viewModels {
		RecipeEditingViewModelFactory(
			(application as TournantApplication).database.recipeDao(),
			intent.getLongExtra("RECIPE_ID", 0L)
		)
	}

	private var imageChanged = false
	private var imageRemoved = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (!intent.hasExtra("RECIPE_ID")) {
			Log.e(TAG, "No recipe provided")
			finish()
			return
		}

		binding = ActivityRecipeEditingBinding.inflate(layoutInflater)

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
			title = getString(R.string.edit)
		}

		val imageChooser = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
			if (uri != null)
				try {
					val rotation = contentResolver.openInputStream(uri)?.use { inputStream ->
						ExifInterface(inputStream).rotationDegrees.takeUnless { it == 0 }
					}
					contentResolver.openInputStream(uri)?.use { inputStream ->
						val sourceImage = BitmapFactory.decodeStream(inputStream)
						val rotatedImage = rotation?.let {
							Log.e(TAG, "Rotate bitmap by $it degrees")
							val matrix = Matrix().apply { postRotate(it.toFloat()) }
							Bitmap.createBitmap(sourceImage, 0, 0, sourceImage.width, sourceImage.height, matrix, true)
						} ?: sourceImage

						File(application.filesDir, "images").mkdir()
						File(File(application.filesDir, "images"), "tmp.jpg").outputStream().use { outputStream ->
							rotatedImage.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
							imageChanged = true
							imageRemoved = false
						}

						sourceImage.recycle()
						rotatedImage.recycle()

						binding.editImage.load(File(File(application.filesDir, "images"), "tmp.jpg")) {
							addLastModifiedToFileCacheKey(true)
						}
						binding.editImageRemove.visibility = View.VISIBLE
					} ?: Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
				}
				catch (e: Exception) {
					Log.e(TAG, e.message.toString())
					Toast.makeText(this, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
				}
		}

		binding.editImageAdd.setOnClickListener {
			imageChooser.launch("image/*")
		}

		binding.editImageRemove.setOnClickListener {
			binding.editImage.setImageDrawable(null)
			binding.editImageRemove.visibility = View.GONE
			imageRemoved = true
			imageChanged = false
		}

		binding.unsetRating.setOnClickListener {
			viewModel.recipe.value.rating = null
			binding.editRating.rating = 0f
		}

		binding.editYieldValue.apply {
			keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
		}

		binding.editYieldUnit.hint = getString(R.string.optional, getString(R.string.unit))

		var language by mutableStateOf("")
		binding.editLanguage.setContent {
			TournantTheme {
				Surface {
					Box (Modifier.fillMaxWidth()) {
						var expanded by remember { mutableStateOf(false) }

						Box(Modifier.height(IntrinsicSize.Min)) {
							OutlinedTextField(
								modifier = Modifier.fillMaxWidth(),
								value = language,
								onValueChange = {},
								label = { Text(stringResource(R.string.language)) },
								readOnly = true,
								singleLine = true,
								trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "") }
							)
							Surface(
								modifier = Modifier
									.fillMaxSize()
									.padding(top = 8.dp)
									.clickable { expanded = true },
								color = Color.Transparent,
							) {}
						}

						if (expanded) {
							LanguageSelectionDialog { locale ->
								locale?.let {
									binding.recipe?.language = it
									language = it.displayName
								}
								expanded = false
							}
						}
					}
				}
			}
		}

		var keywords by mutableStateOf("")
		binding.editKeywords.setContent {
            val focusManager = LocalFocusManager.current
			TournantTheme {
				Surface {
					OutlinedTextField(
						value = keywords,
						onValueChange = { input ->
							keywords = input
							binding.recipe?.keywords = LinkedHashSet(
								input.split(",")
									.map { it.trim() }
									.filter { it.isNotBlank() }
							)
						},
						label = { Text(getString(R.string.keywords) + " (" + getString(R.string.comma_separated) + ")") },
						keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
						keyboardActions = KeyboardActions(onNext = {
							focusManager.clearFocus()
							binding.editSource.requestFocus()
						}),
						singleLine = true
					)
				}
			}
		}

		var from by mutableStateOf<Int?>(null)
		var until by mutableStateOf<Int?>(null)
		@OptIn(ExperimentalMaterialApi::class)
		binding.editSeason.setContent {
			TournantTheme {
				Surface {
					Column {
						Text(
							stringResource(R.string.season),
							Modifier.padding(top = 16.dp),
							color = MaterialTheme.colors.onSurface,
							fontSize = 14.sp
						)
						Row(verticalAlignment = Alignment.CenterVertically) {
							var fromExpanded by remember { mutableStateOf(false) }
							var untilExpanded by remember { mutableStateOf(false) }
							ExposedDropdownMenuBox(
								expanded = fromExpanded,
								onExpandedChange = { fromExpanded = true },
								modifier = Modifier.weight(1f)
							) {
								OutlinedTextField(
									value = from?.let {
										Calendar.getInstance().run {
											set(Calendar.MONTH, it)
											getDisplayName(Calendar.MONTH, Calendar.LONG, getAppOrSystemLocale())
										}
									} ?: "",
									onValueChange = {},
									label = { Text(stringResource(R.string.from)) },
									readOnly = true,
									singleLine = true,
									trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "") }
								)
								DropdownMenu(
									expanded = fromExpanded,
									onDismissRequest = { fromExpanded = false },
									modifier = Modifier.exposedDropdownSize()
								) {
									for (i in 0..11) {
										DropdownMenuItem(
											onClick = {
												from = i
												if (until == null)
													untilExpanded = true
												else
													binding.recipe?.season = Season.createOrNull(from, until)
												fromExpanded = false
											}
										) {
											Calendar.getInstance().run {
												set(Calendar.MONTH, i)
												Text(getDisplayName(Calendar.MONTH, Calendar.LONG, getAppOrSystemLocale()) ?: "")
											}
										}
									}
								}
							}
							Spacer(Modifier.width(8.dp))
							ExposedDropdownMenuBox(
								expanded = untilExpanded,
								onExpandedChange = { untilExpanded = it },
								modifier = Modifier.weight(1f)
							) {
								OutlinedTextField(
									value = until?.let {
										Calendar.getInstance().run {
											set(Calendar.MONTH, it)
											getDisplayName(Calendar.MONTH, Calendar.LONG, getAppOrSystemLocale())
										}
									} ?: "",
									onValueChange = {},
									enabled = from != null,
									label = { Text(stringResource(R.string.until)) },
									readOnly = true,
									singleLine = true,
									trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "") },
								)
								if (from != null)
								DropdownMenu(
									modifier = Modifier.exposedDropdownSize(),
									expanded = untilExpanded,
									onDismissRequest = {
										untilExpanded = false
										if (until == null) {
											until = from
											binding.recipe?.season = Season.createOrNull(from, until)
										}
									}
								) {
									for (i in 0..11) {
										DropdownMenuItem(
											{
												until = i
												binding.recipe?.season = Season.createOrNull(from, until)
												untilExpanded = false
											}
										) {
											Calendar.getInstance().run {
												set(Calendar.MONTH, i)
												Text(getDisplayName(Calendar.MONTH, Calendar.LONG, getAppOrSystemLocale()) ?: "")
											}
										}
									}
								}
							}
							Spacer(Modifier.width(8.dp))
							val focusManager = LocalFocusManager.current
							IconButton(
								{
									from = null
									until = null
									binding.recipe?.season = null
									focusManager.clearFocus(true)
								},
									modifier = Modifier
										.padding(top = 4.dp)
										.clip(CircleShape)
										.size(42.dp)
										.padding(6.dp)
										.background(MaterialTheme.colors.onSurface.copy(.25f))
							) {
								Icon(
									imageVector = Icons.Filled.Clear,
									tint = MaterialTheme.colors.onSurface,
									contentDescription = stringResource(R.string.reset)
								)
							}
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.recipe.collectLatest { recipe ->
				binding.recipe = recipe
				if (recipe.id != 0L) {
					val imageFile = File(File(application.filesDir, "images"), "${recipe.id}.jpg")
					if (imageFile.exists()) {
							binding.editImage.load(imageFile) {
								addLastModifiedToFileCacheKey(true)
							}
						binding.editImageRemove.visibility = View.VISIBLE
					}
					else recipe.image?.let { image ->
						binding.editImage.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.size))
						binding.editImageRemove.visibility = View.VISIBLE
					}
				}
				keywords = recipe.keywords.joinToString(", ")
				language = recipe.language.displayName
				from = recipe.season?.from
				until = recipe.season?.until
			}
		}

		lifecycleScope.launch {
			viewModel.categoryStrings.collectLatest {
				if (it.isNotEmpty())
					binding.editCategory.apply {
						setSimpleItems(it.toTypedArray())
						setOnClickListener { if (!enoughToFilter()) showDropDown() }
						threshold = 1
					}
			}
		}

		lifecycleScope.launch {
			viewModel.cuisineStrings.collectLatest {
				if (it.isNotEmpty())
					binding.editCuisine.apply {
						setSimpleItems(it.toTypedArray())
						setOnClickListener { if (!enoughToFilter()) showDropDown() }
						threshold = 1
					}
			}
		}

		lifecycleScope.launch {
			viewModel.sourceStrings.collectLatest {
				if (it.isNotEmpty())
					binding.editSource.apply {
						setSimpleItems(it.toTypedArray())
						setOnClickListener { if (!enoughToFilter()) showDropDown() }
						threshold = 1
					}
			}
		}

		lifecycleScope.launch {
			viewModel.yieldUnitStrings.collectLatest {
				if (it.isNotEmpty())
					binding.editYieldUnit.apply {
						setSimpleItems(it.toTypedArray())
						setOnClickListener { if (!enoughToFilter()) showDropDown() }
						threshold = 1
					}
			}
		}

		lifecycleScope.launch {
			combine(
				viewModel.ingredients,
				viewModel.titlesWithIds,
				viewModel.ingredientItemSuggestions,
				viewModel.ingredientUnitSuggestions
			) { ingredients, titles, ingredientItemSuggestions, ingredientUnitSuggestions ->
				IngredientEditingAdapter(
					this@RecipeEditingActivity,
					ingredients,
					titles,
					ingredientItemSuggestions,
					ingredientUnitSuggestions
				)
			}.collectLatest { adapter ->
				binding.editIngredients.adapter = adapter

				itemTouchHelper.attachToRecyclerView(binding.editIngredients)

				binding.editIngredientsNewIngredient.setOnClickListener {
					viewModel.ingredients.value.add(Ingredient(null, null, null, "", null, null, false))
					adapter.onItemInserted()
					adapter.notifyItemInserted(adapter.itemCount + 1)
				}

				binding.editIngredientsNewReference.setOnClickListener {
					viewModel.ingredients.value.add(Ingredient(null, null, null, null, 0, null, false))
					adapter.onItemInserted()
					adapter.notifyItemInserted(adapter.itemCount + 1)
				}

				binding.editIngredientsNewGroup.setOnClickListener {
					viewModel.ingredients.value.add(IngredientGroupTitle(""))
					adapter.onItemInserted()
					adapter.notifyItemInserted(adapter.itemCount + 1)
					viewModel.ingredients.value.add(IngredientGroupTitle(null))
					adapter.onItemInserted()
					adapter.notifyItemInserted(adapter.itemCount + 1)
				}
			}
		}

		lifecycleScope.launch {
			delay(1000)
			runOnUiThread {
				for (view in listOf(binding.editTitle, binding.editCategory, binding.editCuisine, binding.editSource, binding.editLink)) {
					((view.parent as ViewGroup).parent as TextInputLayout).isHintAnimationEnabled = true
				}
			}
		}

	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.save -> {
				if (viewModel.recipe.value.id != 0L) {
					val imageFile = File(File(application.filesDir, "images"), "${viewModel.recipe.value.id}.jpg")
					if (imageChanged) {
						viewModel.recipe.value.image = null
						File(File(application.filesDir, "images"), "tmp.jpg").renameTo(imageFile)
					}

					if (imageRemoved) {
						viewModel.recipe.value.image = null
						if (imageFile.exists())
							imageFile.delete()
					}
				}

				viewModel.saveRecipe()
				item.isEnabled = false
				val p = (binding.root.parent as ViewGroup)
				p.removeAllViews()
				p.addView(layoutInflater.inflate(R.layout.activity_main_loading, p, false))

				lifecycleScope.launch {
					viewModel.savedWithId.collect {
						if (it > 0) {
							val imageFile = File(File(application.filesDir, "images"), "${it}.jpg")
							if (imageChanged) {
								File(File(application.filesDir, "images"), "tmp.jpg").renameTo(imageFile)
							}

							if (imageRemoved) {
								if (imageFile.exists())
									imageFile.delete()
							}
							finish()
						}
					}
				}

				true
			}

			android.R.id.home -> {
				finish()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options_recipe_editing, menu)
		return true
	}

	private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

		override fun isItemViewSwipeEnabled() = false
		override fun isLongPressDragEnabled() = false

		override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
			return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
		}

		override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
			val from = viewHolder.bindingAdapterPosition
			val to = target.bindingAdapterPosition
			viewModel.ingredients.value.move(from, to)
			(binding.editIngredients.adapter as IngredientEditingAdapter).notifyItemMoved(from, to)
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

	})

	override fun startDrag(holder: RecyclerView.ViewHolder) {
		itemTouchHelper.startDrag(holder)
	}

	@Composable
	fun LanguageSelectionDialog(onSelected: (Locale?) -> Unit) {
		Dialog(
			onDismissRequest = { onSelected(null) }
		) {
			Surface(
				modifier = Modifier.fillMaxWidth(.9f),
				elevation = 8.dp,
				shape = RoundedCornerShape(4.dp)
			) {
				LazyColumn {
					item {
						Spacer(Modifier.height(4.dp))
					}
					item {
						LanguageCaption(stringResource(R.string.app_language))
					}
					item {
						LanguageSelectionItem(getAppOrSystemLocale(), FontWeight.Normal) { onSelected(it) }
					}
					item {
						Divider(Modifier.padding(12.dp))
					}
					item {
						LanguageCaption(stringResource(R.string.supported_languages))
					}
					val allLanguages = Locale.getAvailableLocales().sortedBy { it.displayName }
					val supportedLanguageTags = getString(R.string.availableLanguages).split(",")
					allLanguages
						.filter { it.toLanguageTag() in supportedLanguageTags }
						.forEach { locale ->
							item {
								LanguageSelectionItem(locale, FontWeight.Normal) { onSelected(it) }
							}
						}
					item {
						Divider(Modifier.padding(12.dp))
					}
					item {
						LanguageCaption(stringResource(R.string.all_languages))
					}
					allLanguages.forEach { locale ->
						item {
							LanguageSelectionItem(locale, if (locale.toLanguageTag() in supportedLanguageTags) FontWeight.Bold else FontWeight.Light) { onSelected(it) }
						}
					}
				}
			}
		}
	}

	@Composable
	fun LanguageCaption(text: String) {
		Box(Modifier
			.fillMaxWidth()
			.padding(16.dp, 12.dp)) {
			Text(text, fontWeight = FontWeight.Bold)
		}
	}

	@Composable
	fun LanguageSelectionItem(
		locale: Locale,
		weight: FontWeight,
		onSelected: (Locale?) -> Unit
	) {
		Box(
			Modifier
				.fillMaxWidth()
				.clickable { onSelected(locale) }
				.padding(16.dp, 12.dp)
		) {
			Text(
				locale.displayName + " (${locale.toLanguageTag()})",
				fontWeight = weight,
				fontStyle = if (weight == FontWeight.Light) FontStyle.Italic else FontStyle.Normal
			)
		}
	}

}