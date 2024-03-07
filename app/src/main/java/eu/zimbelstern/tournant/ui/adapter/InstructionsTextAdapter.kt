package eu.zimbelstern.tournant.ui.adapter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.databinding.RecyclerItemTextBinding
import eu.zimbelstern.tournant.findDurationsByRegex
import eu.zimbelstern.tournant.findFirstAmount
import eu.zimbelstern.tournant.findFirstIngredientWithAmount
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.withFractionsToFloat
import kotlin.math.roundToInt

class InstructionsTextAdapter(
	private val instructionsTextInterface: InstructionsTextInterface,
	text: String,
	private val ingredients: List<Ingredient> = listOf(),
	private val scale: Float? = 1f
	) : RecyclerView.Adapter<InstructionsTextAdapter.InstructionTextViewHolder>() {

	private val paragraphs = text.split("<br/>")

	class InstructionTextViewHolder(val binding: RecyclerItemTextBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionTextViewHolder {
		val binding = RecyclerItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return InstructionTextViewHolder(binding)
	}

	override fun onBindViewHolder(holder: InstructionTextViewHolder, position: Int) {
		val paragraph = SpannableStringBuilder(paragraphs[position].parseAsHtml())

		if (scale != null && scale != 1f) {
			ingredients.filter { it.amount != null }.forEach { ingredient ->
				var cursor = 0
				while (cursor < paragraph.length) {
					val match = paragraph.findFirstIngredientWithAmount(holder.binding.root.context.getString(R.string.to), ingredient, cursor) ?: break
					Log.d("", "Recalculating ingredient string ${match.value}...: (${match.range})")
					// Ingredient detected
					// Set whole string italic
					paragraph.setSpan(
						StyleSpan(Typeface.ITALIC),
						match.range.first,
						match.range.last + 1,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					// Move cursor to the start of the match
					cursor = match.range.first
					// Keep track of inserted strings
					var insertLengths = 0
					// And start extracting numbers
					while (cursor <= match.range.last + insertLengths)  {
						// Search inside the matched string
						val amount = paragraph.findFirstAmount(cursor.. (match.range.last + insertLengths)) ?: break
						Log.d("", "...with amount ${amount.value} (${amount.range})")
						// Amount detected
						// Strike through the match
						paragraph.setSpan(
							StrikethroughSpan(),
							amount.range.first,
							amount.range.last + 1,
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
						)
						// Create new amount string
						amount.value.withFractionsToFloat()?.let {
							(it * scale).toStringForCooks().also { newAmount ->
								Log.d("", "...-> $newAmount")
								// Insert new string
								paragraph.insert(amount.range.last + 1, " $newAmount")
								// Make it bold
								paragraph.setSpan(
									StyleSpan(Typeface.BOLD),
									amount.range.last + 2,
									amount.range.last + newAmount.length + 2,
									Spanned.SPAN_EXCLUSIVE_EXCLUSIVE + 1
								)
								// Keep track that we extended the string
								insertLengths += newAmount.length + 1
								// Move the cursor to the end of the string
								cursor += amount.range.last + newAmount.length + 3
							}
						}
					}
				}
			}
		}

		val resources = holder.binding.root.context.resources
		val to = resources.getString(R.string.to)
		val hourS = "(h)|" + resources.getString(R.string.hours_for_regex)
		val minuteS = "(min)|" + resources.getString(R.string.minutes_for_regex)
		val secondS = "(s)|" + resources.getString(R.string.seconds_for_regex)
		paragraph.findDurationsByRegex(to, hourS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.showAlarmDialog((it.first * 60).roundToInt())
					}
				},
				it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		paragraph.findDurationsByRegex(to, minuteS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.showTimerDialog((it.first * 60).roundToInt())
					}
				},
				it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		paragraph.findDurationsByRegex(to, secondS).forEach {
			paragraph.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						instructionsTextInterface.showTimerDialog(it.first.roundToInt())
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
		fun showAlarmDialog(minutes: Int)
		fun showTimerDialog(seconds: Int)
	}

}