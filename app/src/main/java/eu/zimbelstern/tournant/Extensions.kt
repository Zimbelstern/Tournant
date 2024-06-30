package eu.zimbelstern.tournant

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.toSpanned
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Stack
import kotlin.math.pow
import kotlin.math.roundToInt

val separator = DecimalFormatSymbols.getInstance().decimalSeparator

fun Float.getNumberOfDigits(): Int {
	var f = this
	var i = 0
	while (f != f.roundToInt().toFloat()) {
		i++
		f *= 10
	}
	return i
}

/*
    Returns a localised string omitting trailing zeros, with or without thousands separator
*/
fun Float?.toStringForCooks(thousands: Boolean = true): String {
	if (this == null)
		return ""

	var formattedNumber =
		if (this <= toInt()) // i.e. Float == Int -> 123
			NumberFormat.getInstance().format(this)
		else // remove trailing zeros and a trailing decimal separator
			NumberFormat.getInstance().format(this)
				.dropLastWhile { it == '0' }
				.dropLastWhile { !it.isDigit() }

	if (!thousands) {
		formattedNumber = formattedNumber.replace(Regex("[^0-9$separator]"), "")
	}

	return formattedNumber
}

fun String.parseLocalFormattedFloat(): Float? {
	return replace(Regex("[^0-9$separator]"), "").replace(separator, '.').toFloatOrNull()
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

fun String.withFractionsToFloat(separator: Char = eu.zimbelstern.tournant.separator): Float? {
	return try {
		 when {
			 this.contains(" ") -> {
				 this.split(" ")[1].withFractionsToFloat(separator)
					 ?.plus(this.split(" ")[0].toFloat())
			 }

			 this.contains("/") -> {
				 this.split("/")[0].toFloat() / this.split("/")[1].toFloat()
			 }

			 else -> Regex("[^0-9$separator]").replace(this, "").replace(separator, '.').toFloat()
		 }
	} catch (_: Exception) {
		null
	}
}

fun String.extractFractionsToFloat(separator: Char = eu.zimbelstern.tournant.separator): Pair<Float?, String?> {
	val fraction =
		if (get(0).isDigit()) {
			split(" ").takeWhile { it.matches(Regex("^[0-9$separator/ ]+$")) }.joinToString(" ")
		} else null
	val remainingString =
		if (fraction?.length?.equals(length) == false) {
			substring(fraction.length).trim()
		}
		else null
	return Pair(fraction?.withFractionsToFloat(separator), remainingString)
}

fun List<Int>.toRangeList(): List<IntRange> {
	val list = sorted()
	val result = mutableListOf<IntRange>()
	var start = list[0]
	var last = list[0]
	forEach {
		if (it > last + 1) {
			result.add(start..last)
			start = it
		}
		last = it
	}
	result.add(start..last)
	return result.toList()
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

fun Spanned.findDurationsByRegex(dashWords: String, timeUnitWords: String): Sequence<Pair<Float, IntRange>> {
	return Regex("""(\d{1,3}([$separator/]\d{1,2})?((\s?\p{Pd}\s?)|(\s$dashWords\s)))?\d{1,3}([$separator/]\d{1,2})?\s?($timeUnitWords)([^A-Za-z]|$)""").findAll(this).map {
		Pair(
			it.value.takeWhile { char -> char.isDigit() || char == separator || char == '/'}.withFractionsToFloat(separator)!!,
			IntRange(it.range.first, it.range.last.minus(if (it.value.last().isLetter()) 0 else 1))
		)
	}
}

fun Spanned.findFirstIngredientWithAmount(dashWords: String, ingredient: Ingredient, from: Int): MatchResult? {
	return Regex("""(\d{1,3}([$separator/]\d{1,2})?((\s?\p{Pd}\s?)|(\s$dashWords\s)))?\d{1,3}([$separator/]\d{1,2})?\s?${ingredient.unit ?: ""}\s${ingredient.item}""").find(this, from)
}

fun Spanned.findFirstAmount(range: IntRange): MatchResult? {
	return Regex("""\d{1,3}([$separator/]\d{1,2})?""").find(this, range.first).takeIf {
		it == null || it.range.last < range.last
	}
}

fun Spanned.splitLines(): List<Spanned> {
	val positions = Stack<Int>()
	val newSpans = mutableListOf<Spanned>()

	var next = this.indexOf('\n')
	while (next > 0) {
		positions.push(next)
		next = this.indexOf('\n', next + 1)
		if (next >= this.length)
			break
	}
	positions.push(this.length)

	while (positions.isNotEmpty()) {
		val end = positions.pop()
		val start = if (positions.isEmpty()) 0 else positions.peek()
		newSpans.add(0, this.subSequence(start, end).trimStart('\n') as? Spanned ?: SpannableStringBuilder(" ").toSpanned())
	}
	return newSpans
}