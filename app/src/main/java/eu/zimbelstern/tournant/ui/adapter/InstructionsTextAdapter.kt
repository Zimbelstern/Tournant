package eu.zimbelstern.tournant.ui.adapter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.databinding.RecyclerItemTextBinding
import eu.zimbelstern.tournant.findDurationsByRegex
import eu.zimbelstern.tournant.findFirstAmount
import eu.zimbelstern.tournant.findFirstIngredientWithAmount
import eu.zimbelstern.tournant.toRangeList
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.withFractionsToDouble
import kotlin.math.roundToInt

class InstructionsTextAdapter(
	private val instructionsTextInterface: InstructionsTextInterface,
	private val paragraphs: List<Spanned>,
	private val ingredients: List<Ingredient> = listOf(),
	private val scale: Double? = 1.0,
	private val dashWords: String,
	private val hString: String,
	private val minString: String,
	private val sString: String
	) : RecyclerView.Adapter<InstructionsTextAdapter.InstructionTextViewHolder>() {

	class InstructionTextViewHolder(val binding: RecyclerItemTextBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionTextViewHolder {
		val binding = RecyclerItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		binding.instructionText.movementMethod = LinkMovementMethod.getInstance()
		return InstructionTextViewHolder(binding)
	}

	override fun onBindViewHolder(holder: InstructionTextViewHolder, position: Int) {
		val paragraph = SpannableStringBuilder(paragraphs[position])
		if (scale != null && scale != 1.0) {
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
						amount.value.withFractionsToDouble()?.let {
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

		if (paragraph.isNotEmpty()) {
			val free = (0..paragraph.length).toMutableList()
			paragraph.findDurationsByRegex(dashWords, "(h)|$hString").forEach {
				paragraph.setSpan(
					object : ClickableSpan() {
						override fun onClick(widget: View) {
							instructionsTextInterface.showAlarmDialog((it.first * 60).roundToInt())
						}
					},
					it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				free.removeAll(it.second)
			}
			paragraph.findDurationsByRegex(dashWords, "(min)|$minString").forEach {
				paragraph.setSpan(
					object : ClickableSpan() {
						override fun onClick(widget: View) {
							instructionsTextInterface.showTimerDialog((it.first * 60).roundToInt())
						}
					},
					it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				free.removeAll(it.second)
			}
			paragraph.findDurationsByRegex(dashWords, "(s)|$sString").forEach {
				paragraph.setSpan(
					object : ClickableSpan() {
						override fun onClick(widget: View) {
							instructionsTextInterface.showTimerDialog(it.first.roundToInt())
						}
					},
					it.second.first, it.second.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				free.removeAll(it.second)
			}
			free.toRangeList().forEach {
				paragraph.setSpan(
					object : ClickableSpan() {
						override fun onClick(widget: View) {
							holder.binding.instructionText.apply { isChecked = !isChecked }
						}
						override fun updateDrawState(ds: TextPaint) { }
					}, it.first , it.last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
		}
		holder.binding.instructionText.movementMethod = LinkMovementMethod.getInstance()
		holder.binding.instructionText.text = paragraph
	}

	override fun getItemCount(): Int {
		return paragraphs.size
	}

	interface InstructionsTextInterface {
		fun showAlarmDialog(minutes: Int)
		fun showTimerDialog(seconds: Int)
	}

}