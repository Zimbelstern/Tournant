package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine
import java.text.NumberFormat
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

fun Float?.toStringForCooks(): String {
	if (this == null) return ""
	if (this <= this.toInt()) return this.toInt().toString()
	return NumberFormat.getInstance().format(this)
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

fun String.withFractionsToFloat(separator: Char = ".".single()): Float? {
	return try {
		 when {
			 this.contains(" ") -> {
				 this.split(" ")[1].withFractionsToFloat()
					 ?.plus(this.split(" ")[0].toFloat())
			 }

			 this.contains("/") -> {
				 this.split("/")[0].toFloat() / this.split("/")[1].toFloat()
			 }

			 else -> Regex("[^0-9$separator]").replace(this, "").replace(separator, ".".single()).toFloat()
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

fun MutableList<Ingredient>.inflate(): MutableList<IngredientLine> {
	val newList = mutableListOf<IngredientLine>()
	var group: String? = null
	for (item in this) {
		val newgroup = item.group
		if (newgroup != group) {
			if (group != null)
				newList.add(IngredientGroupTitle(null))
			group = newgroup
			if (group != null)
				newList.add(IngredientGroupTitle(group))
		}
		newList.add(item)
	}
	if (group != null)
		newList.add(IngredientGroupTitle(null))
	return newList
}

fun MutableList<IngredientLine>.deflate(): MutableList<Ingredient> {
	val newList = mutableListOf<Ingredient>()
	var group: String? = null
	var i = 0
	for (item in this) {
		if (item is Ingredient && (item.item?.isBlank() == false || item.refId?.equals(0L) == false)) {
			newList.add(item.apply {
				item.group = group
				item.position = i
			})
			i++
		}
		if (item is IngredientGroupTitle)
			group = item.title
	}
	return newList
}

fun MutableList<IngredientLine>.move(from: Int, to: Int) {
	val element = this.removeAt(from)
	this.add(to, element)
	if (element is Ingredient) {
		if (to + 1 < this.size) {
			val successor = this[to + 1]
			if (successor is Ingredient)
				(this[to] as Ingredient).group = successor.group
		}
	}
}