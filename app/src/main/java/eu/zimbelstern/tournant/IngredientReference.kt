package eu.zimbelstern.tournant

import kotlinx.parcelize.Parcelize

@Parcelize
data class IngredientReference(
	val refId: Int,
	val amount: String,
	val name: String
) : IngredientListElement() {

	fun withScaledAmount(factor: Float): IngredientReference {
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
}