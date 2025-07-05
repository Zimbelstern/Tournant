package eu.zimbelstern.tournant.utils

import androidx.room.TypeConverter
import java.util.Date
import java.util.Locale

object RoomTypeConverters {

	@TypeConverter
	fun longToDate(value: Long?) = value?.let { Date(it) }

	@TypeConverter
	fun dateToLong(date: Date?) = date?.time

	@TypeConverter
	fun stringToLocale(value: String): Locale = Locale.getAvailableLocales().find { it.toLanguageTag() == value } ?: Locale.forLanguageTag(value)

	@TypeConverter
	fun localeToString(locale: Locale): String = locale.toLanguageTag()

}