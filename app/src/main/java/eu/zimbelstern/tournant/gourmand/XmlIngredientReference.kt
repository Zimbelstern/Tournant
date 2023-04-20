package eu.zimbelstern.tournant.gourmand

import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import eu.zimbelstern.tournant.toStringForCooks
import eu.zimbelstern.tournant.withFractionsToFloat
import kotlinx.parcelize.Parcelize

@Parcelize
data class XmlIngredientReference(
	val refId: Int,
	val amount: String,
	val name: String
) : XmlIngredientListElement() {

	fun withScaledAmount(factor: Float): XmlIngredientReference {
		if (factor == 1f) {
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
		return if (amount.contains("-")) {
			Ingredient(
				amount.split("-")[0].withFractionsToFloat(),
				amount.split("-")[1].withFractionsToFloat(),
				null,
				refId.toLong(),
				inGroup,
				false
			)
		} else {
			Ingredient(
				amount.withFractionsToFloat(),
				null,
				null,
				refId.toLong(),
				inGroup,
				false)
		}
	}

}