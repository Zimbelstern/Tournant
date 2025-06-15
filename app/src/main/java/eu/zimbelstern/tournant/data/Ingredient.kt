package eu.zimbelstern.tournant.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Ingredient(
	var amount: Double?,
	var amountRange: Double?,
	var unit: String?,
	var item: String?,
	var refId: Long?,
	var group: String?,
	var optional: Boolean
) : Parcelable, IngredientLine {

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

}