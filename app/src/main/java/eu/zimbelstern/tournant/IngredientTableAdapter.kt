package eu.zimbelstern.tournant

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding

class IngredientTableAdapter(private val recipeActivity: RecipeDetail, ingredientList: List<IngredientListElement>) : RecyclerView.Adapter<IngredientTableAdapter.IngredientTableViewHolder>() {

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

		holder.binding.ingredientGroupName.text = row.first[0]
		holder.binding.ingredientAmountValue.text = row.first[1]
		holder.binding.ingredientAmountUnit.text = row.first[2]
		holder.binding.ingredientItem.text = row.first[3]

		// For ingredient references
		row.second?.let { refId ->
			holder.binding.ingredientItem.apply {
				paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
				setOnClickListener {
					recipeActivity.findReferencedRecipe(refId)
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