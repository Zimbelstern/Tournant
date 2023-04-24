package eu.zimbelstern.tournant

import kotlin.math.ceil
import kotlin.math.roundToInt

class RecipeUtils {
	companion object {
		fun moreYield(currentYield: Float): Float {
			return if (currentYield >= 1) {
				currentYield.roundToInt() + 1f
			}
			else if (currentYield >= 0.5f) {
				1f
			}
			else if (currentYield >= 0.25f) {
				0.5f
			}
			else {
				currentYield
			}
		}

		fun lessYield(currentYield: Float): Float {
			return if (currentYield > 1) {
				ceil(currentYield) - 1
			}
			else if (currentYield > 0.5f) {
				0.5f
			}
			else if (currentYield > 0.25f){
				0.25f
			}
			else {
				currentYield
			}
		}
	}
}