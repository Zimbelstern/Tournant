package eu.zimbelstern.tournant

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.text.toSpanned
import androidx.core.view.WindowInsetsCompat
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.Stack
import java.util.TimeZone
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val separator = DecimalFormatSymbols.getInstance().decimalSeparator
val floatingNumber = """\d{1,3}(?:[$separator/]\d{1,2})?"""

fun Double.getNumberOfDigits(): Int {
	var f = this
	var i = 0
	while (f != f.roundToLong().toDouble()) {
		i++
		f *= 10
	}
	return i
}

/*
    Returns a localised string omitting trailing zeros, with or without thousands separator
*/
fun Double?.toStringForCooks(thousands: Boolean = true): String {
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

fun String.parseLocalFormattedDouble(): Double? {
	return replace(Regex("[^0-9$separator]"), "").replace(separator, '.').toDoubleOrNull()
}

fun Double.roundToNDigits(n: Int): Double {
	return (this * 10f.pow(n)).roundToLong() / 10.0.pow(n)
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

fun String.withFractionsToDouble(separator: Char = eu.zimbelstern.tournant.separator): Double? {
	return try {
		 when {
			 this.contains(" ") -> {
				 this.split(" ")[1].withFractionsToDouble(separator)
					 ?.plus(this.split(" ")[0].toDouble())
			 }

			 this.contains("/") -> {
				 this.split("/")[0].toDouble() / this.split("/")[1].toDouble()
			 }

			 else -> Regex("[^0-9$separator]").replace(this, "").replace(separator, '.').toDouble()
		 }
	} catch (_: Exception) {
		null
	}
}

fun String.extractFractionsToDouble(separator: Char = eu.zimbelstern.tournant.separator): Pair<Double?, String?> {
	val fraction =
		if (get(0).isDigit()) {
			split(" ").takeWhile { it.matches(Regex("^[0-9$separator/ ]+$")) }.joinToString(" ")
		} else null
	val remainingString =
		if (fraction?.length?.equals(length) == false) {
			substring(fraction.length).trim()
		}
		else null
	return Pair(fraction?.withFractionsToDouble(separator), remainingString)
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

fun MutableList<Ingredient>.scale(factor: Double?): List<Ingredient> {
	return if (factor != null) {
		map {
			it.withScaledAmount(factor)
		}
	}
	else this
}

fun MutableList<Ingredient>.addGroupTitles(): MutableList<IngredientLine> {
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

fun MutableList<IngredientLine>.hideGroupTitles(): MutableList<Ingredient> {
	val newList = mutableListOf<Ingredient>()
	var group: String? = null
	for (item in this) {
		if (item is Ingredient && (item.item?.isBlank() == false || item.refId?.equals(0L) == false)) {
			newList.add(item.apply {
				item.group = group
			})
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

fun SpannableStringBuilder.setClickableSpan(range: IntRange, onClick: (View) -> Unit) =
	setSpan(
		object : ClickableSpan() {
			override fun onClick(v: View) = onClick(v)
		},
		range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
	)


data class TimeExpression(val seconds: Int, val position: IntRange)

@Suppress("RegExpUnnecessaryNonCapturingGroup")
fun Spanned.findDurationsByRegex(dashWords: String, timeUnitWords: String, scale: Int): Sequence<TimeExpression> {
	return Regex("""(?<=\s|^)(?:($floatingNumber)(?:\s?\p{Pd}\s?|\s$dashWords\s))?($floatingNumber)\s?(?:$timeUnitWords)(?=\W|$)""")
		.findAll(this)
		.mapNotNull {
			it.groups.filterNotNull()[1].value
				.withFractionsToDouble(separator)
				?.times(scale)
				?.roundToInt()
				?.let { s ->
					it.groups[0]?.range?.let { p ->
						TimeExpression(s, p)
					}
				}
		}
}

fun Spanned.findTimeExpressions(dashWords: String, hoursWords: String, minutesWords: String, secondsWords: String): List<TimeExpression> {

	val hours = findDurationsByRegex(dashWords, hoursWords, 3600)
	val minutes = findDurationsByRegex(dashWords, minutesWords, 60)
	val seconds = findDurationsByRegex(dashWords, secondsWords, 1)

	val prime = "['‘]"
	val doubleprime = """(?:(?:$prime$prime)|"|″)"""

	val minutesAbbr = Regex("""(?<=\s|^)(\d+)${prime}(?=\s|$)""", RegexOption.MULTILINE)
		.findAll(this)
		.mapNotNull {
			it.groups[1]?.value?.toIntOrNull()?.times(60)?.let { s ->
				it.groups[0]?.range?.let { p ->
					TimeExpression(s, p)
				}
			}
		}

	val secondsAbbr = Regex("""(?<=\s|^)(\d+)$doubleprime(?=\s|$)""", RegexOption.MULTILINE)
		.findAll(this)
		.mapNotNull {
			it.groups[1]?.value?.toIntOrNull()?.let { s ->
				it.groups[0]?.range?.let { p ->
					TimeExpression(s, p)
				}
			}
		}

	val minutesSecondsAbbr = Regex("""(?<=\s|^)(\d+)${prime}(\d+)$doubleprime(?=\s|$)""", RegexOption.MULTILINE)
		.findAll(this)
		.mapNotNull {
			it.groups[1]?.value?.toIntOrNull()?.times(60)?.let { s1 ->
				it.groups[2]?.value?.toIntOrNull()?.let { s2 ->
					it.groups[0]?.range?.let { p ->
						TimeExpression(s1 + s2, p)
					}
				}
			}
		}

	return (hours + minutes + seconds + minutesAbbr + secondsAbbr + minutesSecondsAbbr).toList()
}

fun Spanned.findFirstIngredientWithAmount(dashWords: String, ingredient: Ingredient, from: Int): MatchResult? {
	val unit = Regex.escape(ingredient.unit ?: "")
	val item = Regex.escape(ingredient.item ?: "")
	return Regex("""(?:($floatingNumber)(?:\s?\p{Pd}\s?|\s$dashWords\s))?($floatingNumber)\s?$unit\s$item""")
		.find(this, from)
}

fun Spanned.findFirstAmount(range: IntRange): MatchResult? {
	return Regex(floatingNumber).find(this, range.first).takeIf {
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

fun WindowInsetsCompat.safeInsets() : androidx.core.graphics.Insets {
	return getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
}

fun getAppOrSystemLocale(): Locale =
	AppCompatDelegate.getApplicationLocales().get(0)
		?: LocaleListCompat.getDefault().get(0)
		?: Locale.getDefault()

fun Date.shiftToLocalDayStart(): Date =
	Date(time - TimeZone.getDefault().getOffset(time))

@Suppress("unused")
fun <T> T.logit(description: String? = null): T {
	Log.e("logit()", (description ?: this?.javaClass?.name) + ": $this")
	return this
}

@Suppress("unused")
fun <T, R> T.logit(description: String? = null, lambda: T.() -> R): T {
	Log.e("logit()", (description ?: this?.javaClass?.name) + ": ${lambda()}")
	return this
}
