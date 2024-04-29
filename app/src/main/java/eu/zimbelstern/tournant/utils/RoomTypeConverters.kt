package eu.zimbelstern.tournant.utils

import androidx.room.TypeConverter
import java.util.Date

object RoomTypeConverters {

	@TypeConverter
	fun longToDate(value: Long?) = value?.let { Date(it) }

	@TypeConverter
	fun dateToLong(date: Date?) = date?.time

}