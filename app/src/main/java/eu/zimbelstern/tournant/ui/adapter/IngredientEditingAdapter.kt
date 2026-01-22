package eu.zimbelstern.tournant.ui.adapter

import android.annotation.SuppressLint
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.textfield.TextInputLayout
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
	private var itemSuggestions: List<String>,
	private var unitSuggestions: List<String>
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

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		when (holder.itemViewType) {
			VIEW_TYPE_INGREDIENT -> fillIngredientInformation(holder as IngredientViewHolder, ingredientLines[position] as Ingredient)
			VIEW_TYPE_GROUP -> fillGroupInformation(holder as GroupTitleViewHolder, ingredientLines[position] as IngredientGroupTitle)
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	fun fillIngredientInformation(holder: IngredientViewHolder, ingredient: Ingredient) {
		with (holder) {
			binding.ingredient = ingredient
			val isRef = ingredient.refId != null

			// Set hints for the first ingredient row
			listOf(
				binding.editAmountContainer to R.string.amount,
				binding.editUnitContainer to R.string.unit,
				binding.editItemContainer to R.string.ingredient
			).forEach {
				it.first.hint = if (ingredientLines.take(bindingAdapterPosition).filterIsInstance<Ingredient>().isEmpty())
					it.first.context.getString(it.second)
				else
					null
			}

			binding.editAmount.apply {
				keyListener = DigitsKeyListener.getInstance("0123456789-" + DecimalFormatSymbols.getInstance().decimalSeparator)
				setText(ingredient.amount.toStringForCooks(false).plus(
					ingredient.amountRange.let {
						if (it != null) "-" + it.toStringForCooks(false) else ""
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

			// Prevents new lines on enter press (even for multiline text fields)
			val onEditorActionListener = TextView.OnEditorActionListener { view, actionId, _ ->
				if (actionId == EditorInfo.IME_NULL) {
					view.focusSearch(View.FOCUS_DOWN)?.requestFocus()
					true
				} else {
					false
				}
			}

			val inputFilter = InputFilter { source: CharSequence, start: Int, end: Int, _: Spanned, _: Int, _: Int ->
				val newText = source.subSequence(start, end).toString()
				if (newText.contains("\n")) newText.replace("\n", "") else null
			}

			binding.editUnit.apply {
				setOnEditorActionListener(onEditorActionListener)
				setRawInputType(InputType.TYPE_CLASS_TEXT)
				setSimpleItems(unitSuggestions.toTypedArray())
				threshold = 3
				filters = arrayOf(inputFilter)
			}

			binding.editItem.apply {
				setOnEditorActionListener(onEditorActionListener)
				setRawInputType(InputType.TYPE_CLASS_TEXT)
				filters = arrayOf(inputFilter)
				setSimpleItems(
					if (isRef)titlesWithIds.map { it.title }.toTypedArray()
					else itemSuggestions.toTypedArray()
				)
				threshold = if (isRef) 0 else 3
				if (isRef) {
					setText(titlesWithIds.find { it.id == ingredient.refId }?.title ?: "")
					doAfterTextChanged { editable ->
						ingredient.refId = titlesWithIds.find { it.title == editable.toString() }?.id
					}
					onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
						if (!hasFocus && ingredient.refId == null) {
							binding.editItem.text = null
						}
					}
				}
			}

			binding.editItemContainer.apply {
				endIconMinSize = 0
				endIconMode = if (isRef) TextInputLayout.END_ICON_DROPDOWN_MENU else TextInputLayout.END_ICON_NONE
			}


			binding.editPosition.setOnTouchListener { _, event ->
				if (event.action == MotionEvent.ACTION_DOWN) {
					ingredientEditingInterface.startDrag(holder)
				}
				false
			}

			binding.editOptions.setOnClickListener { view ->
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
	}

	fun fillGroupInformation(holder: GroupTitleViewHolder, group: IngredientGroupTitle) {

		with (holder) {
			binding.group = group

			if (group.title == null) {
				val group = ingredientLines.take(bindingAdapterPosition).findLast { it is IngredientGroupTitle } as IngredientGroupTitle
				binding.showTitle.text = group.title
			}

			binding.editOptions.setOnClickListener { view ->
				PopupMenu(view.context, view).apply {
					inflate(R.menu.options_ingredient)
					menu.removeItem(R.id.toggle_optional)
					setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.remove_ingredient -> {
								ingredientLines.removeAt(bindingAdapterPosition)
								val stopIndex = bindingAdapterPosition + ingredientLines.subList(
									bindingAdapterPosition,
									ingredientLines.size
								).indexOfFirst {
									it is IngredientGroupTitle
								}
								ingredientLines.removeAt(stopIndex)
								notifyItemRemoved(bindingAdapterPosition)
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