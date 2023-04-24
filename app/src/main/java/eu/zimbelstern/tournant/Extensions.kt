package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Ingredient
import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.getNumberOfDigits(): Int {
	var f = this
	var i = 0
	while (f != f.roundToInt().toFloat()) {
		i++
		f *= 10
	}
	return i
}

fun Float.toStringForCooks(): String {
	return this.toString()
		.dropLastWhile { it == '0' }
		.dropLastWhile { !it.isDigit() }
}

fun Float.roundToNDigits(n: Int): Float {
	return (this * 10f.pow(n)).roundToInt() / 10f.pow(n)
}

fun String.getQuantityIntForPlurals(): Int? {
	val float = this.toFloatOrNull() ?: return null
	return if (float > 1 && float < 2) {
		3
	}
	else {
		float.toInt()
	}
}

fun String.withFractionsToFloat(): Float? {
	return try {
		 when {
			 this.contains(" ") -> {
				 this.split(" ")[1].withFractionsToFloat()
					 ?.plus(this.split(" ")[0].toFloat())
			 }

			 this.contains("/") -> {
				 this.split("/")[0].toFloat() / this.split("/")[1].toFloat()
			 }

			 else -> this.toFloat()
		 }
	} catch (_: Exception) {
		null
	}
}

fun MutableList<Ingredient>.scale(factor: Float?): List<Ingredient> {
	return if (factor != null) {
		map {
			it.withScaledAmount(factor)
		}
	}
	else this
}