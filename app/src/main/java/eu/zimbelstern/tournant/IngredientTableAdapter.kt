package eu.zimbelstern.tournant

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding
import eu.zimbelstern.tournant.ui.RecipeActivity

class IngredientTableAdapter(private val recipeActivity: RecipeActivity, ingredientList: List<IngredientListElement>) : RecyclerView.Adapter<IngredientTableAdapter.IngredientTableViewHolder>() {

	private val tableRows = createTableRows(ingredientList)

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

		if (textViews[0].text == "") {
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

	private fun createTableRows(ingredientList: List<IngredientListElement>): List<Pair<List<String>, Int?>> {
		return mutableListOf<Pair<List<String>, Int?>>().apply {
			ingredientList.forEach {
				when (it) {
					is Ingredient -> {
						add(Pair(listOf(
							"",
							it.amount?.plus(" ") ?: "",
							it.unit?.plus(" ") ?: "",
							if (it.optional == true)
								recipeActivity.getString(R.string.optional, it.item)
							else
								it.item ?: ""
						), null))
					}
					is IngredientReference -> {
						add(Pair(
							listOf("", it.amount.plus(" "), "", it.name),
							it.refId)
						)
					}
					is IngredientGroup -> {
						add(Pair(
							listOf(it.name ?: "", "", "", ""),
							null
						))

						addAll(createTableRows(it.list))

						// Condense empty rows
						if (last().first.any { s -> s != "" })
							add(Pair(listOf("","","",""), null))
					}
					else -> listOf("", null)
				}
			}
		}
	}

}