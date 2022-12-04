package eu.zimbelstern.tournant

import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient (
	val amount: String?,
	val unit: String?,
	val item: String?,
	val key: String?,
	val optional: Boolean?
) : IngredientListElement() {

	fun withScaledAmount(factor: Float): Ingredient {
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

}