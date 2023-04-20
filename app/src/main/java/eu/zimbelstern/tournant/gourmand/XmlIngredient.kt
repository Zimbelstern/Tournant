package eu.zimbelstern.tournant.gourmand

import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.withFractionsToFloat
import kotlinx.parcelize.Parcelize

@Parcelize
data class XmlIngredient(
	val amount: String?,
	val unit: String?,
	val item: String?,
	val key: String?,
	val optional: Boolean?
) : XmlIngredientListElement() {

	fun withScaledAmount(factor: Float): XmlIngredient {
		if (factor == 1f || amount == null) {
			return this
		}

		val amountScaled = try {
			amount.toFloat()
				.times(factor)
				.roundToNDigits(amount.toFloat().getNumberOfDigits() + 2)
				.toStringForCooks()
		} catch (_: Exception) {
			"${factor}x $amount"
		}

		return this.copy(amount = amountScaled)
	}

	fun toIngredient(inGroup: String? = null): Ingredient {
		return if (amount?.contains("-") == true) {
			Ingredient(
				amount.split("-")[0].withFractionsToFloat(),
				amount.split("-")[1].withFractionsToFloat(),
				unit,
				item ?: "",
				inGroup,
				optional ?: false
			)
		} else {
			Ingredient(
				amount?.withFractionsToFloat(),
				null,
				unit,
				item ?: "",
				inGroup,
				optional ?: false)
		}
	}

}
