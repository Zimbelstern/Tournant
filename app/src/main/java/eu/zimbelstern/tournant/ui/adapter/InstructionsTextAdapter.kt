package eu.zimbelstern.tournant.ui.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.databinding.RecyclerItemTextBinding
import eu.zimbelstern.tournant.findDurationsByRegex
import kotlin.math.roundToInt

class InstructionsTextAdapter(
	private val instructionsTextInterface: InstructionsTextInterface,
	text: String
) : RecyclerView.Adapter<InstructionsTextAdapter.InstructionTextViewHolder>() {

	private val paragraphs = text.split("<br/>")

	class InstructionTextViewHolder(val binding: RecyclerItemTextBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionTextViewHolder {
		val binding = RecyclerItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return InstructionTextViewHolder(binding)
	}

	override fun onBindViewHolder(holder: InstructionTextViewHolder, position: Int) {
		val paragraph = SpannableString(paragraphs[position].parseAsHtml())
		val resources = holder.binding.root.context.resources
		val to = resources.getString(R.string.to)
		val hourS = "(h)|" + resources.getString(R.string.hours_for_regex)
		val minuteS = "(min)|" + resources.getString(R.string.minutes_for_regex)
		val secondS = "(s)|" + resources.getString(R.string.seconds_for_regex)
		paragraph.findDurationsByRegex(to, hourS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.setAlarm((it.first * 60).roundToInt())
					}
				},
				it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		paragraph.findDurationsByRegex(to, minuteS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.startTimer((it.first * 60).roundToInt())
					}
				},
				it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		paragraph.findDurationsByRegex(to, secondS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.startTimer(it.first.roundToInt())
					}
				},
				it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		holder.binding.instructionText.movementMethod = LinkMovementMethod.getInstance()
		holder.binding.instructionText.text = paragraph

		if (paragraphs[position].isNotEmpty()) holder.binding.instructionText.setOnClickListener {
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

	interface InstructionsTextInterface {
		fun setAlarm(minutes: Int)
		fun startTimer(seconds: Int)
	}

}