package eu.zimbelstern.tournant.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import eu.zimbelstern.tournant.toStringForCooks
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Ingredient(
	var amount: Double? = null,
	var amountRange: Double? = null,
	var unit: String? = null,
	var item: String? = null,
	var refId: Long? = null,
	var group: String? = null,
	var optional: Boolean = false
) : Parcelable {

	companion object {
		fun createDummy(n: Int? = null) = when (n) {
			3 -> Ingredient(4.5, null, "dt", null, 3, null, false)
			2 -> Ingredient(2.0, 3.0, null, "Bananas", null, null, true)
			else -> Ingredient(100.0, null, "mg", "Amoxicillin", null, null, false)
		}
		fun createExamples(n: Int) = (1..n).map { createDummy(it) }
	}

	fun removeEmptyValues() {
		if (unit?.isBlank() == true) unit = null
		if (item?.isBlank() == true) item = null
		if (refId != null) item = null
	}

	fun withScaledAmount(factor: Double): Ingredient {
		if (factor == 1.0) {
			return this
		}

		val amountScaled = amount.let {
			it?.times(factor)?.roundToNDigits(it.getNumberOfDigits() + 1)
		}

		val amountRangeScaled = amountRange.let {
			it?.times(factor)?.roundToNDigits(it.getNumberOfDigits() + 1)
		}

		return copy(amount = amountScaled, amountRange = amountRangeScaled)
	}

	fun amountToStringForCooks(appendSpace: Boolean = true) = buildString {
		if (amountRange == null) {
			append(amount.toStringForCooks())
		}
		else {
			append("${amount.toStringForCooks()}–${amountRange.toStringForCooks()}")
		}
		if (unit != null) {
			append(" $unit")
		}
		if (appendSpace && isNotEmpty()) {
			append(" ")
		}
	}

	fun toStringForCooks(optionalWord: String) = buildString {
		append(amountToStringForCooks())
		append(item)
		if (optional) {
			append(" $optionalWord")
		}
	}

}