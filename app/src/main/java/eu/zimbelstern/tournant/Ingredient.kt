package eu.zimbelstern.tournant

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient (
	val amount: String?,
	val unit: String?,
	val item: String?,
	val key: String?,
	val optional: Boolean?
) : Parcelable {

	fun withScaledAmount(factor: Float): Ingredient {
		if (factor == 1f || amount == null) {
			return this
		}

		val amountScaled = amount.toFloat()
			.times(factor)
			.roundToNDigits(amount.toFloat().getNumberOfDigits() + 2)
			.toStringForCooks()

		return this.copy(amount = amountScaled)
	}

}
