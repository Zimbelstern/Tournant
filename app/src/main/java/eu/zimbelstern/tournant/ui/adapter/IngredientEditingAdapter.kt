package eu.zimbelstern.tournant.ui.adapter

import android.annotation.SuppressLint
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine
import eu.zimbelstern.tournant.data.RecipeTitleId
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientEditingBinding
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientEditingGroupBinding
import eu.zimbelstern.tournant.toStringForCooks
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class IngredientEditingAdapter(
	private val ingredientEditingInterface: IngredientEditingInterface,
	private val ingredientLines: MutableList<IngredientLine>,
	private var titlesWithIds: List<RecipeTitleId>,
	private var ingredientSuggestions: List<String>
): RecyclerView.Adapter<ViewHolder>() {

	companion object {
		private const val VIEW_TYPE_INGREDIENT = 0
		private const val VIEW_TYPE_GROUP = 1
	}

	class IngredientViewHolder(val binding: RecyclerItemIngredientEditingBinding) : ViewHolder(binding.root)
	class GroupTitleViewHolder(val binding: RecyclerItemIngredientEditingGroupBinding) : ViewHolder(binding.root)

	override fun getItemViewType(position: Int): Int {
		return if (ingredientLines[position] is Ingredient) 0 else 1
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return if (viewType == VIEW_TYPE_INGREDIENT)
			IngredientViewHolder(RecyclerItemIngredientEditingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		else
			GroupTitleViewHolder(RecyclerItemIngredientEditingGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {

		if (holder.itemViewType == VIEW_TYPE_INGREDIENT) {

			holder as IngredientViewHolder
			val ingredient = ingredientLines[position] as Ingredient

			holder.binding.ingredient = ingredient

			holder.binding.editAmount.apply {
				keyListener = DigitsKeyListener.getInstance("0123456789-" + DecimalFormatSymbols.getInstance().decimalSeparator)
				setText(ingredient.amount.toStringForCooks().plus(
					ingredient.amountRange.let {
						if (it != null) "-" + it.toStringForCooks() else ""
					}
				))
				doAfterTextChanged {
					if (it.toString().isEmpty()) {
						ingredient.amount = null
						ingredient.amountRange = null
					} else {
						val numbers = it.toString().split("-")
						try {
							ingredient.amount = NumberFormat.getInstance().parse(numbers[0])?.toDouble()
							ingredient.amountRange = if (numbers.size == 2) NumberFormat.getInstance().parse(numbers[1])?.toDouble() else null
						} catch (_: Exception) { }
					}
				}
			}

			holder.binding.editItem.setRawInputType(InputType.TYPE_CLASS_TEXT)

			if (ingredient.refId == null) {
				holder.binding.editItem.setSimpleItems(ingredientSuggestions.toTypedArray())
				holder.binding.editItem.threshold = 3
			} else {
				holder.binding.editItemField.visibility = View.GONE
				holder.binding.editRefField.visibility = View.VISIBLE
				holder.binding.editRef.setText(titlesWithIds.find { it.id == ingredient.refId }?.title ?: "")
				holder.binding.editRef.setAdapter(
					ArrayAdapter(
						holder.binding.root.context,
						android.R.layout.simple_dropdown_item_1line,
						titlesWithIds.map { it.title }
					)
				)
				holder.binding.editRef.doAfterTextChanged { editable ->
					ingredient.refId = titlesWithIds.find { it.title == editable.toString() }?.id ?: 0L
				}
			}

			holder.binding.editPosition.setOnTouchListener { _, event ->
				if (event.action == MotionEvent.ACTION_DOWN) {
					ingredientEditingInterface.startDrag(holder)
				}
				false
			}

			holder.binding.editOptions.setOnClickListener { view ->
				PopupMenu(view.context, view).apply {
					inflate(R.menu.options_ingredient)
					if (ingredient.optional)
						menu.findItem(R.id.toggle_optional).title = view.context.getString(R.string.make_mandatory)
					setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.remove_ingredient -> {
								ingredientLines.removeAt(holder.bindingAdapterPosition)
								notifyItemRemoved(holder.bindingAdapterPosition)
								true
							}

							R.id.toggle_optional -> {
								ingredient.optional = !ingredient.optional
								notifyItemChanged(holder.bindingAdapterPosition)
								true
							}

							else -> false
						}
					}
					show()
				}
			}
		}

		if (holder.itemViewType == VIEW_TYPE_GROUP) {

			holder as GroupTitleViewHolder
			val group = ingredientLines[position] as IngredientGroupTitle

			holder.binding.group = group

			if (group.title == null) {
				holder.binding.showTitle.text = (ingredientLines.subList(0, position).findLast { it is IngredientGroupTitle } as IngredientGroupTitle).title
			}

			holder.binding.editOptions.setOnClickListener { view ->
				PopupMenu(view.context, view).apply {
					inflate(R.menu.options_ingredient)
					menu.removeItem(R.id.toggle_optional)
					setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.remove_ingredient -> {
								ingredientLines.removeAt(holder.bindingAdapterPosition)
								val stopIndex = holder.bindingAdapterPosition + ingredientLines.subList(
										holder.bindingAdapterPosition,
										ingredientLines.size
									).indexOfFirst {
										it is IngredientGroupTitle
									}
								ingredientLines.removeAt(stopIndex)
								notifyItemRemoved(holder.bindingAdapterPosition)
								notifyItemRemoved(stopIndex)
								true
							}
							else -> false
						}
					}
					show()
				}
			}
		}
	}

	override fun onViewAttachedToWindow(holder: ViewHolder) {
		if (focus) {
			(holder as? IngredientViewHolder)?.binding?.editAmount?.requestFocus()
			(holder as? GroupTitleViewHolder)?.binding?.editTitle?.requestFocus()
			focus = false
		}
	}

	override fun getItemCount(): Int {
		return ingredientLines.size
	}

	private var focus = false
	fun onItemInserted() {
		focus = true
	}

	interface IngredientEditingInterface {
		fun startDrag(holder: ViewHolder)
	}

}