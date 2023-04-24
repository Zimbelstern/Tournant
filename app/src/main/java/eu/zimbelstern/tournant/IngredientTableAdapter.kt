package eu.zimbelstern.tournant

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding
import eu.zimbelstern.tournant.ui.RecipeActivity

class IngredientTableAdapter(
	private val recipeActivity: RecipeActivity,
	ingredientList: List<Ingredient>,
	scale: Float? = null
) : RecyclerView.Adapter<IngredientTableAdapter.IngredientTableViewHolder>() {

	private val tableRows = createTableRows(ingredientList, scale)

	class IngredientTableViewHolder(val binding: RecyclerItemIngredientsBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientTableViewHolder {
		val binding = RecyclerItemIngredientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

		binding.ingredientAmount.minimumWidth = tableRows.maxOf {
			binding.ingredientAmountValue.paint.measureText(it.first[1].plus(it.first[2]))
		}.toInt()

		return IngredientTableViewHolder(binding)
	}

	override fun onBindViewHolder(holder: IngredientTableViewHolder, position: Int) {
		val row = tableRows[position]

		val textViews = listOf(
			holder.binding.ingredientGroupName,
			holder.binding.ingredientAmountValue,
			holder.binding.ingredientAmountUnit,
			holder.binding.ingredientItem
		)

		textViews.forEachIndexed { i, it ->
			it.text = row.first[i]
		}

		// For ingredient references
		row.second?.let { refId ->
			holder.binding.ingredientItem.apply {
				paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
				setOnClickListener {
					recipeActivity.findReferencedRecipe(refId)
				}
			}
		}

		if (textViews.slice(1..3).any { it.text.isNotEmpty() }) {
			holder.binding.root.setOnClickListener {
				if (holder.binding.ingredientChecked.isVisible) {
					textViews.forEach {
						it.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.normal_text_color))
					}
					holder.binding.ingredientChecked.isVisible = false
				} else {
					textViews.forEach {
						it.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.checked_text_color))
					}
					holder.binding.ingredientChecked.isVisible = true
				}
			}
		}
	}

	override fun getItemCount(): Int {
		return tableRows.size
	}

	private fun createTableRows(ingredientList: List<Ingredient>, scale: Float?): List<Pair<List<String>, Long?>> {
		val scaleOrEmpty = if (scale != null && scale != 1f) {
			"(${scale.toStringForCooks()}x)"
		} else ""
		return mutableListOf<Pair<List<String>, Long?>>().apply {
			var group: String? = null
			ingredientList.forEach {
				if (it.group != group) {
					if (it.position != 0) {
						add(
							Pair(
								listOf("", "", "", ""),
								null
							)
						)
					}
					if (!it.group.isNullOrEmpty()) {
						add(Pair(
							listOf(it.group ?: "", "", "", ""),
							null
						))
					}
					group = it.group
				}
				var amountString = it.amount?.toStringForCooks() ?: scaleOrEmpty
				it.amountRange?.let { maxAmount ->
					amountString += "-${maxAmount.toStringForCooks()}"
				}
				add(Pair(listOf(
					"",
					"$amountString ",
					it.unit?.plus(" ") ?: "",
					if (it.optional)
						recipeActivity.getString(R.string.optional, it.item ?: it.refId.toString())
					else
						it.item ?: "¬"
				), it.refId))
			}
		}
	}

}