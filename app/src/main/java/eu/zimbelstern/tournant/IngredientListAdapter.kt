package eu.zimbelstern.tournant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding

class IngredientListAdapter(private val strings: List<String?>, private val type: Int) : RecyclerView.Adapter<IngredientListAdapter.IngredientListViewHolder>() {

	companion object {
		const val TYPE_AMOUNT = 1
		const val TYPE_UNIT = 2
		const val TYPE_ITEM = 3
	}

	class IngredientListViewHolder(val binding: RecyclerItemIngredientsBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientListViewHolder {
		val binding = RecyclerItemIngredientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return IngredientListViewHolder(binding)
	}

	override fun onBindViewHolder(holder: IngredientListViewHolder, position: Int) {
		if (strings[position] != null)
			holder.binding.root.text = StringBuilder(strings[position]!!).append("  ").toString()
		else
			holder.binding.root.text = null
		if (type == TYPE_AMOUNT)
			holder.binding.root.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
	}

	override fun getItemCount(): Int {
		return strings.size
	}

}