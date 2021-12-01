package eu.zimbelstern.tournant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemIngredientsBinding
import java.lang.StringBuilder

class IngredientListAdapter(private val strings: List<String?>) : RecyclerView.Adapter<IngredientListAdapter.IngredientListViewHolder>() {

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
		if (strings[position]?.isDigitsOnly() == true)
			holder.binding.root.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
	}

	override fun getItemCount(): Int {
		return strings.size
	}

}