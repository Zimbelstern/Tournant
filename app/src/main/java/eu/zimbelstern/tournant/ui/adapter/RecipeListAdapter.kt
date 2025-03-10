package eu.zimbelstern.tournant.ui.adapter

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.text.format.DateUtils.WEEK_IN_MILLIS
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.view.setPadding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_CREATED
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_INGREDIENTS_COUNT
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_INSTRUCTIONS_LENGTH
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPARATIONS_COUNT
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPARED
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.ChipData
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.databinding.RecyclerItemRecipeBinding
import java.io.File
import java.util.Date

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
	private var sortedBy = 0

	private var ccColors = mapOf<String, ChipData>()
	fun updateColors(chipData: List<ChipData>) {
		ccColors = chipData.associateBy {
			it.string
		}
	}

	inner class RecipeListViewHolder(val binding: RecyclerItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeListViewHolder {
		val binding = RecyclerItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		binding.recipeCardImage.clipToOutline = true
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
				setPadding(0)
			} else {
				setImageResource(R.drawable.ic_dining)
				setPadding(4 * resources.displayMetrics.density.toInt())
			}
		}

		holder.binding.recipeCardTitle.text = recipe.title

		holder.binding.recipeCardDescription.apply {
			text = recipe.description
			visibility = if (recipe.description != null) View.VISIBLE else View.GONE
		}

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

		holder.binding.time.apply {
			visibility = if (recipe.preptime == null && recipe.cooktime == null)
				View.GONE
			else {
				setPadding(0, (8 * resources.displayMetrics.density - holder.binding.recipeCardImage.paddingBottom).toInt(), 0, 0)
				holder.binding.timeText.text = listOf(recipe.preptime, recipe.cooktime).joinToString(" / ") {
					if (it == null) "–" else "$it''"
				}
				View.VISIBLE
			}
		}

		holder.binding.sortedBy.visibility =
			if (sortedBy / 2 in listOf(
					SORTED_BY_CREATED,
					SORTED_BY_MODIFIED,
					SORTED_BY_INSTRUCTIONS_LENGTH,
					SORTED_BY_INGREDIENTS_COUNT
			))
				View.VISIBLE
			else
				View.GONE

		holder.binding.sortedBy.apply {
			visibility = View.VISIBLE
			when (sortedBy / 2) {
				SORTED_BY_CREATED -> {
					text = recipe.created?.let { DateUtils.getRelativeDateTimeString(context, it, MINUTE_IN_MILLIS, WEEK_IN_MILLIS, 0) } ?: "–"
					contentDescription = context.getString(R.string.creation_date)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_date), null, null, null)
				}
				SORTED_BY_MODIFIED -> {
					text = recipe.modified?.let { DateUtils.getRelativeDateTimeString(context, it, MINUTE_IN_MILLIS, WEEK_IN_MILLIS, 0) } ?: "–"
					contentDescription = context.getString(R.string.modification_date)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_modification_date), null, null, null)
				}
				SORTED_BY_INSTRUCTIONS_LENGTH -> {
					@Suppress("SetTextI18n")
					text = (recipe.instructionsLength ?: 0).toString()
					contentDescription = context.getString(R.string.instructions_length)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_notes), null, null, null)
				}
				SORTED_BY_INGREDIENTS_COUNT -> {
					@Suppress("SetTextI18n")
					text = recipe.ingredientsCount.toString()
					contentDescription = context.getString(R.string.ingredients_count)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_list_numbered), null, null, null)
				}
				SORTED_BY_PREPARATIONS_COUNT -> {
					@Suppress("SetTextI18n")
					text = recipe.preparationsCount.toString()
					contentDescription = context.getString(R.string.preparations_count)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_calendar_checked), null, null, null)
				}
				SORTED_BY_PREPARED -> {
					text = recipe.prepared?.let { DateUtils.getRelativeTimeSpanString(it, Date().time, DAY_IN_MILLIS) } ?: "–"
					contentDescription = context.getString(R.string.last_prepared)
					setCompoundDrawablesRelativeWithIntrinsicBounds(AppCompatResources.getDrawable(context, R.drawable.ic_calendar_checked), null, null, null)
				}
				else -> {
					visibility = View.GONE
				}
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
			R.id.export_selected_json -> {
				recipeListInterface.exportRecipes(selectedItems.keys.toSet(), "json")
			}
			R.id.export_selected_zip -> {
				recipeListInterface.exportRecipes(selectedItems.keys.toSet(), "zip")
			}
			R.id.export_selected_gourmand -> {
				recipeListInterface.exportRecipes(selectedItems.keys.toSet(), "xml")
			}
			R.id.share_selected_json-> {
				recipeListInterface.shareRecipes(selectedItems.keys.toSet(), "json")
			}
			R.id.share_selected_zip-> {
				recipeListInterface.shareRecipes(selectedItems.keys.toSet(), "zip")
			}
			R.id.share_selected_gourmand -> {
				recipeListInterface.shareRecipes(selectedItems.keys.toSet(), "xml")
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

	fun updateSortedBy(sortOption: Int) {
		sortedBy = sortOption
		(0..itemCount).forEach {
			notifyItemChanged(it)
		}
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
		fun exportRecipes(recipeIds: Set<Long>, format: String)
		fun shareRecipes(recipeIds: Set<Long>, format: String)
		fun showDeleteDialog(recipeIds: Set<Long>)
	}

}