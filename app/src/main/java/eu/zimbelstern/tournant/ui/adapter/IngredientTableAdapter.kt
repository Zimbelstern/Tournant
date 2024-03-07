package eu.zimbelstern.tournant.ui.adapter

import android.content.res.Resources
import android.graphics.Paint
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.databinding.InputFieldScaleBinding
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding
import eu.zimbelstern.tournant.parseLocalFormattedFloat
import eu.zimbelstern.tournant.toStringForCooks
import java.text.DecimalFormatSymbols

class IngredientTableAdapter(
	private val ingredientTableInterface: IngredientTableInterface,
	ingredientList: List<Ingredient>,
	private val scale: Float? = null
) : RecyclerView.Adapter<IngredientTableAdapter.IngredientTableViewHolder>() {

	private val tableRows = createTableRows(ingredientList, scale)

	class IngredientTableViewHolder(val binding: RecyclerItemIngredientsBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientTableViewHolder {
		val binding = RecyclerItemIngredientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

		binding.ingredientAmount.minimumWidth = tableRows.maxOf {
			binding.ingredientAmountValue.paint.measureText(it.amountString.plus(it.unitString))
		}.toInt()

		return IngredientTableViewHolder(binding)
	}

	override fun onBindViewHolder(holder: IngredientTableViewHolder, position: Int) {
		val row = tableRows[position]

		holder.binding.ingredientGroupName.text = row.groupString
		holder.binding.ingredientAmountValue.text = row.amountString
		holder.binding.ingredientAmountUnit.text = row.unitString
		holder.binding.ingredientItem.text = row.itemString

		// For ingredient references
		row.refId?.let { refId ->
			holder.binding.ingredientItem.apply {
				paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
				setOnClickListener {
					ingredientTableInterface.openRecipe(refId, row.amountString.substringBefore('-').parseLocalFormattedFloat(), row.unitString.trim())
				}
			}
		}

		if (row.isIngredient) {
			val ingredientViews = holder.binding.run { listOf(ingredientAmountValue, ingredientAmountUnit, ingredientItem) }

			ingredientViews.forEach { checkabletextView ->
				checkabletextView.setOnClickListener {
					ingredientViews.forEach {
						it.isChecked = !it.isChecked
					}
					row.checked = !row.checked
				}
			}

			if (row.amountString.isNotEmpty()) {
				val amount = row.amountString.substringBefore('-').parseLocalFormattedFloat() ?: return
				ingredientViews.forEach {
					it.setOnLongClickListener {
						val customView = InputFieldScaleBinding.inflate(LayoutInflater.from(holder.binding.root.context), holder.binding.root, false)
						customView.inputLayout.apply {
							hint = row.itemString
							suffixText = row.unitString
						}
						customView.inputField.apply {
							keyListener = DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance().decimalSeparator)
							setText(amount.toStringForCooks(thousands = false))
							requestFocus()
						}
						MaterialAlertDialogBuilder(holder.binding.root.context)
							.setTitle(R.string.scale_to)
							.setView(customView.root)
							.setPositiveButton(R.string.ok) { dialog, _ ->
								val newAmount = customView.inputField.text.toString().parseLocalFormattedFloat()
								if (newAmount != null)
									ingredientTableInterface.scale(
										newAmount / amount * (scale ?: 1f)
									)
								dialog.dismiss()
							}
							.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss()}
							.show()
							.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
						true
					}
				}
			}
		}
	}

	override fun getItemCount(): Int {
		return tableRows.size
	}

	private fun createTableRows(ingredientList: List<Ingredient>, scale: Float?): List<IngredientRow> {
		val scaleOrEmpty = if (scale != null && scale != 1f) {
			"(${scale.toStringForCooks()}x)"
		} else ""
		return mutableListOf<IngredientRow>().apply {
			var group: String? = null
			ingredientList.forEach {
				if (it.group != group) {
					if (it.position != 0) {
						add(IngredientRow("", "", "", "", null))
					}
					if (!it.group.isNullOrEmpty()) {
						add(IngredientRow(it.group ?: "", "", "", "", null))
					}
					group = it.group
				}
				var amountString = it.amount?.toStringForCooks() ?: scaleOrEmpty
				it.amountRange?.let { maxAmount ->
					amountString += "-${maxAmount.toStringForCooks()}"
				}
				add(IngredientRow(
					"",
					"$amountString ",
					it.unit?.plus(" ") ?: "",
					if (it.optional)
						ingredientTableInterface.getResources().getString(R.string.optional, it.item ?: it.refId.toString())
					else
						it.item ?: "?",
					it.refId
				))
			}
		}
	}

	fun ingredientsToString(): String {
		return tableRows.joinToString("\n") {
			it.groupString + it.amountString + it.unitString + it.itemString + if (it.checked) " \u2713" else ""
		}
	}

	inner class IngredientRow(
		val groupString: String,
		val amountString: String,
		val unitString: String,
		val itemString: String,
		val refId: Long?) {
		val isIngredient = amountString.isNotEmpty() || unitString.isNotEmpty() || itemString.isNotEmpty()
		var checked: Boolean = false
	}

	interface IngredientTableInterface {
		fun getResources(): Resources
		fun openRecipe(refId: Long, yieldAmount: Float?, yieldUnit: String?)
		fun scale(scaleFactor: Float)
	}

}