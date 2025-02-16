package eu.zimbelstern.tournant.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.textfield.TextInputLayout
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.databinding.ActivityRecipeEditingBinding
import eu.zimbelstern.tournant.move
import eu.zimbelstern.tournant.safeInsets
import eu.zimbelstern.tournant.ui.adapter.IngredientEditingAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormatSymbols

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
					contentResolver.openInputStream(uri).use { inputStream ->
						if (inputStream == null)
							Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
						else {
							File(application.filesDir, "images").mkdir()
							File(File(application.filesDir, "images"), "tmp.jpg").outputStream().use { outputStream ->
								val image = BitmapFactory.decodeStream(inputStream)
								image.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
								imageChanged = true
								imageRemoved = false
							}
							Glide.with(this)
								.load(File(File(application.filesDir, "images"), "tmp.jpg"))
								.diskCacheStrategy(DiskCacheStrategy.NONE)
								.skipMemoryCache(true)
								.into(binding.editImage)
							binding.editImageRemove.visibility = View.VISIBLE
						}
					}
				}
				catch (e: Exception) {
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


		lifecycleScope.launch {
			viewModel.recipe.collectLatest { recipe ->
				binding.recipe = recipe
				if (recipe.id != 0L) {
					val imageFile = File(File(application.filesDir, "images"), "${recipe.id}.jpg")
					if (imageFile.exists()) {
						Glide.with(this@RecipeEditingActivity)
							.load(imageFile)
							.signature(ObjectKey(imageFile.lastModified()))
							.into(binding.editImage)
						binding.editImageRemove.visibility = View.VISIBLE
					}
					else recipe.image?.let { image ->
						binding.editImage.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.size))
						binding.editImageRemove.visibility = View.VISIBLE
					}
				}
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
			viewModel.titlesWithIds.combine(viewModel.ingredientStrings) { t, i ->
				Pair(t, i)
			}.combine(viewModel.ingredients) { titlesIngredients, ingredients ->
				IngredientEditingAdapter(this@RecipeEditingActivity, ingredients, titlesIngredients.first, titlesIngredients.second)
			}.collectLatest { adapter ->
				binding.editIngredients.adapter = adapter

				itemTouchHelper.attachToRecyclerView(binding.editIngredients)

				binding.editIngredientsNewIngredient.setOnClickListener {
					viewModel.ingredients.value.add(Ingredient(0, null, null, null, "", null, false))
					adapter.onItemInserted()
					adapter.notifyItemInserted(adapter.itemCount + 1)
				}

				binding.editIngredientsNewReference.setOnClickListener {
					viewModel.ingredients.value.add(Ingredient(0, null, null, null, 0, null, false))
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

	override fun removeIngredient(id: Long) {
		viewModel.ingredientsRemoved.add(id)
	}

}