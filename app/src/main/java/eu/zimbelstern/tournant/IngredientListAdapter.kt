package eu.zimbelstern.tournant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding

class IngredientListAdapter(private val ingredients: List<Ingredient>) : RecyclerView.Adapter<IngredientListAdapter.IngredientListViewHolder>() {

	class IngredientListViewHolder(val binding: RecyclerItemIngredientsBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientListViewHolder {
		val binding = RecyclerItemIngredientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

		binding.recyclerItemIngredientsAmount.minimumWidth = ingredients.maxOf { ingredient ->
			var amountString = ""
			ingredient.amount?.let {
				amountString += "$it "
			}
			ingredient.unit?.let {
				amountString += "$it "
			}
			binding.recyclerItemIngredientsAmountValue.paint.measureText(amountString)
		}.toInt()

		return IngredientListViewHolder(binding)
	}

	override fun onBindViewHolder(holder: IngredientListViewHolder, position: Int) {
		holder.binding.recyclerItemIngredientsAmountValue.text = ingredients[position].amount?.let { StringBuilder(it).append(" ") }
		holder.binding.recyclerItemIngredientsAmountUnit.text = ingredients[position].unit?.let { StringBuilder(it).append(" ") }
		holder.binding.recyclerItemIngredientsItem.text =
			if (ingredients[position].optional == true)
				holder.binding.root.context.getString(R.string.optional, ingredients[position].item)
			else
				ingredients[position].item
	}

	override fun getItemCount(): Int {
		return ingredients.size
	}

}