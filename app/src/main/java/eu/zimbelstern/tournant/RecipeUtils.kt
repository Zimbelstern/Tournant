package eu.zimbelstern.tournant

import kotlin.math.ceil
import kotlin.math.roundToInt

class RecipeUtils {
	companion object {
		fun moreYield(currentYield: Double): Double {
			return when {
				currentYield >= 1 -> currentYield.roundToInt() + 1.0
				currentYield >= 0.5 -> 1.0
				currentYield >= 0.25 -> 0.5
				else -> currentYield
			}
		}

		fun lessYield(currentYield: Double): Double {
			return when {
				currentYield > 1 -> ceil(currentYield) - 1
				currentYield > 0.5 -> 0.5
				currentYield > 0.25 -> 0.25
				else -> currentYield
			}
		}
	}
}