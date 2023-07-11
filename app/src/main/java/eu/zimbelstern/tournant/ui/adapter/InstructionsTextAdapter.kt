package eu.zimbelstern.tournant.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.databinding.RecyclerItemTextBinding

class InstructionsTextAdapter(text: String) : RecyclerView.Adapter<InstructionsTextAdapter.InstructionTextViewHolder>() {

	private val paragraphs = text.split("<br/>")

	class InstructionTextViewHolder(val binding: RecyclerItemTextBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionTextViewHolder {
		val binding = RecyclerItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return InstructionTextViewHolder(binding)
	}

	override fun onBindViewHolder(holder: InstructionTextViewHolder, position: Int) {
		holder.binding.instructionText.text = paragraphs[position].parseAsHtml()
		if (paragraphs[position].isNotEmpty()) holder.binding.root.setOnClickListener {
			if (holder.binding.instructionChecked.isVisible) {
				holder.binding.instructionText.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.normal_text_color))
				holder.binding.instructionChecked.visibility =  View.INVISIBLE
			} else {
				holder.binding.instructionText.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.checked_text_color))
				holder.binding.instructionChecked.visibility =  View.VISIBLE
			}
		}
	}

	override fun getItemCount(): Int {
		return paragraphs.size
	}

}