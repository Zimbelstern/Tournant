package eu.zimbelstern.tournant.utils

import androidx.databinding.InverseMethod
import eu.zimbelstern.tournant.toStringForCooks
import java.text.NumberFormat

object Converter {

	@InverseMethod("hourToTime")
	@JvmStatic
	fun timeToHour(@Suppress("UNUSED_PARAMETER") oldValue: Int?, value: Int?): String {
		return value?.div(60)?.toString() ?: ""
	}

	@Suppress("Unused")
	@JvmStatic
	fun hourToTime(oldValue: Int?, value: String): Int? {
		val hour = value.toIntOrNull()
		return if (hour != null) hour * 60 + ((oldValue ?: 0) % 60) else oldValue?.mod(60)
	}

	@InverseMethod("minToTime")
	@JvmStatic
	fun timeToMin(@Suppress("UNUSED_PARAMETER") oldValue: Int?, value: Int?): String {
		return value?.mod(60)?.toString() ?: ""
	}

	@Suppress("Unused")
	@JvmStatic
	fun minToTime(oldValue: Int?, value: String): Int? {
		val min = value.toIntOrNull()
		return if (min != null) (oldValue ?: 0) / 60 * 60 + min else oldValue?.div(60)?.times(60)
	}

	@JvmStatic
	fun timeToString(value: Int?): String {
		if (value == null) return ""
		val hourString = if (value >= 60) timeToHour(0, value) + " h" else null
		val minString = if (value % 60 != 0) timeToMin(0, value) + " min" else null
		return listOfNotNull(hourString, minString).joinToString(" ")
	}

	@InverseMethod("stringToDouble")
	@JvmStatic
	fun doubleToString(@Suppress("UNUSED_PARAMETER") oldValue: Double?, value: Double?): String {
		return value?.toStringForCooks(thousands = false) ?: ""
	}

	@Suppress("Unused")
	@JvmStatic
	fun stringToDouble(oldValue: Double?, value: String): Double? {
		return if (value.isEmpty()) null else try {
			NumberFormat.getInstance().parse(value)?.toDouble()
		} catch (_: Exception) {
			oldValue
		}
	}

	@InverseMethod("stringToHtml")
	@JvmStatic fun htmlToString(value: String?): String {
		return value?.replace("<br/>", "\n") ?: ""
	}

	@JvmStatic fun stringToHtml(value: String): String {
		return value
	}

}