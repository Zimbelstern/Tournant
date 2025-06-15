package eu.zimbelstern.tournant.ui.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemPreparationsBinding
import java.util.Date

class PreparationsAdapter(private val preparationsInterface: PreparationsInterface, private val preparations: MutableList<Date>)
	: RecyclerView.Adapter<PreparationsAdapter.PreparationsViewHolder>() {

	class PreparationsViewHolder(val binding: RecyclerItemPreparationsBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreparationsViewHolder {
		return PreparationsViewHolder(
			RecyclerItemPreparationsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
	}

	override fun onBindViewHolder(holder: PreparationsViewHolder, position: Int) {

		holder.binding.root.apply {
			text = DateFormat.getDateFormat(context).format(preparations[position])
			setOnCloseIconClickListener {
				preparationsInterface.removePreparation(preparations[position])
				preparations.removeAt(position)
				notifyItemRemoved(position)
			}
		}

	}

	override fun getItemCount() = preparations.size

	interface PreparationsInterface {
		fun removePreparation(date: Date)
	}

}