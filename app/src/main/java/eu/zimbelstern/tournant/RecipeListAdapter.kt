package eu.zimbelstern.tournant

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.view.setPadding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import eu.zimbelstern.tournant.data.ColorfulString
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.databinding.RecyclerItemRecipeBinding
import java.io.File

class RecipeListAdapter(private val recipeListInterface: RecipeListInterface)
	: PagingDataAdapter<RecipeDescription, RecipeListAdapter.RecipeListViewHolder>(DIFF_CALLBACK),
	ActionMode.Callback {

	companion object {

		const val TAG = "RecipeListAdapter"

		val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipeDescription>() {
			override fun areItemsTheSame(old: RecipeDescription, new: RecipeDescription): Boolean =
				old.id == new.id

			override fun areContentsTheSame(old: RecipeDescription, new: RecipeDescription): Boolean =
				old == new
		}

		const val PAYLOAD_UPDATE_SELECTED = 1
		const val PAYLOAD_UPDATE_SELECTABLE = 2

	}

	private val selectedItems = mutableMapOf<Long, Int>()

	private var ccColors = mapOf<String, ColorfulString>()
	fun updateColors(colorfulStrings: List<ColorfulString>) {
		ccColors = colorfulStrings.associateBy {
			it.string
		}
	}

	inner class RecipeListViewHolder(val binding: RecyclerItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeListViewHolder {
		val binding = RecyclerItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return RecipeListViewHolder(binding)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onBindViewHolder(holder: RecipeListViewHolder, position: Int) {
		val recipe = getItem(position) ?: return

		if (recipe.id in selectedItems && selectedItems[recipe.id] != position)
			selectedItems[recipe.id] = position

		holder.binding.root.apply {

			isSelected = recipe.id in selectedItems

			setOnClickListener {
				recipeListInterface.openRecipeDetail(recipe.id)
			}

			setOnLongClickListener {
				if (mode == null)
					recipeListInterface.startActionMode(this@RecipeListAdapter)

				if (recipe.id !in selectedItems) {
					selectedItems[recipe.id] = position
				} else {
					selectedItems.remove(recipe.id)
				}

				it.isSelected = recipe.id in selectedItems

				if (selectedItems.isEmpty())
					mode?.finish()
				else
					updateTitle()

				true
			}

		}

		holder.binding.selectorView.apply {
			setOnClickListener {
				holder.binding.root.performLongClick()
			}
			visibility = if (mode != null) View.VISIBLE else View.GONE
		}

		holder.binding.recipeCardImage.apply {
			val imageFile = File(File(context.applicationContext.filesDir, "images"), "${recipe.id}.jpg")
			if (imageFile.exists()) {
				Glide.with(context)
					.load(imageFile)
					.signature(ObjectKey(imageFile.lastModified()))
					.into(this)
			}
			else if (recipe.image != null) {
				setImageBitmap(BitmapFactory.decodeByteArray(recipe.image, 0, recipe.image.size))
				clipToOutline = true
				setPadding(0)
			} else {
				setImageResource(R.drawable.ic_dining)
				setPadding(4 * resources.displayMetrics.density.toInt())
			}
		}

		holder.binding.recipeCardTitle.text = recipe.title

		holder.binding.recipeCardRating.apply {
			rating = recipe.rating ?: 0f
			visibility = if (recipe.rating != null) View.VISIBLE else View.GONE
		}

		holder.binding.recipeCardCategory.apply {
			text = recipe.category
			chipBackgroundColor = ccColors[text]?.color
			rippleColor = ccColors[text]?.rippleColor
			visibility = if (recipe.category != null) View.VISIBLE else View.GONE
			setOnClickListener {
				recipeListInterface.searchForSomething(text)
			}
		}

		holder.binding.recipeCardCuisine.apply {
			text = recipe.cuisine
			chipBackgroundColor = ccColors[text]?.color
			rippleColor = ccColors[text]?.rippleColor
			visibility = if (recipe.cuisine != null) View.VISIBLE else View.GONE
			setOnClickListener {
				recipeListInterface.searchForSomething(text)
			}
		}

	}

	override fun onBindViewHolder(holder: RecipeListViewHolder, position: Int, payloads: MutableList<Any>) {
		Log.d(TAG, "Selected views: $selectedItems")
		if (payloads.isEmpty()) return super.onBindViewHolder(holder, position, payloads)

		Log.d(TAG, "View #$position, payloads: $payloads")

		if (payloads.contains(PAYLOAD_UPDATE_SELECTED)) {
			val recipe = getItem(position) ?: return
			if (recipe.id in selectedItems) {
				Log.v(TAG, "View is selected")
				holder.binding.root.isSelected = true
				if (selectedItems[recipe.id] != position)
					selectedItems[recipe.id] = position
			} else {
				Log.v(TAG, "View is not selected")
				holder.binding.root.isSelected = false
			}
		}

		if (payloads.contains(PAYLOAD_UPDATE_SELECTABLE)) {
			Log.v(TAG, "Action mode is ${mode != null}")
			holder.binding.selectorView.visibility = if (mode != null) View.VISIBLE else View.GONE
		}
	}

	// Action mode implementation
	private var mode: ActionMode? = null

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		this.mode = mode
		mode.menuInflater.inflate(R.menu.action, menu)
		if (recipeListInterface.isReadOnly()) {
			menu.removeItem(R.id.delete_selected)
		}
		notifyItemRangeChanged(0, itemCount, PAYLOAD_UPDATE_SELECTABLE)
		return true
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		return false
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.export_selected -> {
				recipeListInterface.exportRecipes(selectedItems.keys.toSet())
			}
			R.id.share_selected -> {
				recipeListInterface.shareRecipes(selectedItems.keys.toSet())
			}
			R.id.delete_selected -> {
				recipeListInterface.showDeleteDialog(selectedItems.keys.toSet())
			}
			R.id.select_all -> {
				select(recipeListInterface.getFilteredRecipesIds())
			}
		}
		return true
	}

	override fun onDestroyActionMode(mode: ActionMode) {
		val viewsChanged = selectedItems.values.toSet().filter { it != -1 }
		selectedItems.clear()
		this.mode = null
		viewsChanged.forEach {
			notifyItemChanged(it, PAYLOAD_UPDATE_SELECTED)
		}
		notifyItemRangeChanged(0, itemCount, PAYLOAD_UPDATE_SELECTABLE)
	}

	private fun updateTitle() {
		mode?.title = recipeListInterface.getResources().getString(R.string.selected, selectedItems.size)
	}

	fun recipeClosed() {
		(0..itemCount).forEach {
			notifyItemChanged(it)
		}
	}

	fun select(recipeIds: Set<Long>) {
		val newItems = recipeIds.filter { it !in selectedItems.keys }.associateWith { -1 }
		selectedItems.putAll(newItems)
		(0..itemCount).minus(selectedItems.values.toSet()).forEach {
			notifyItemChanged(it, PAYLOAD_UPDATE_SELECTED)
		}
		updateTitle()
	}

	fun finishActionMode() {
		mode?.finish()
	}

	interface RecipeListInterface {
		fun getResources(): Resources
		fun getFilteredRecipesIds(): Set<Long>
		fun isReadOnly(): Boolean
		fun openRecipeDetail(recipeId: Long)
		fun searchForSomething(query: CharSequence?)
		fun startActionMode(adapter: RecipeListAdapter)
		fun exportRecipes(recipeIds: Set<Long>)
		fun shareRecipes(recipeIds: Set<Long>)
		fun showDeleteDialog(recipeIds: Set<Long>)
	}

}