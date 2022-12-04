package eu.zimbelstern.tournant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.databinding.RecyclerItemTextBinding

class InstructionsTextAdapter(text: String) : RecyclerView.Adapter<InstructionsTextAdapter.InstructionTextViewHolder>() {

	private val paragraphs = text.split("\n")

	class InstructionTextViewHolder(val binding: RecyclerItemTextBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionTextViewHolder {
		val binding = RecyclerItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return InstructionTextViewHolder(binding)
	}

	override fun onBindViewHolder(holder: InstructionTextViewHolder, position: Int) {
		holder.binding.instructionText.text = paragraphs[position]
		if (paragraphs[position].isNotEmpty()) holder.binding.root.setOnClickListener {
			if (holder.binding.instructionChecked.isVisible) {
				holder.binding.instructionText.setTextColor(ContextCompat.getColor(holder.binding.root.context, android.R.color.tab_indicator_text))
				holder.binding.instructionChecked.visibility =  View.INVISIBLE
			} else {
				holder.binding.instructionText.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.gray))
				holder.binding.instructionChecked.visibility =  View.VISIBLE
			}
		}
	}

	override fun getItemCount(): Int {
		return paragraphs.size
	}

}