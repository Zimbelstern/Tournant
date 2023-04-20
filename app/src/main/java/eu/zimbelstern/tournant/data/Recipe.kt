package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlin.math.ceil
import kotlin.math.roundToInt

@Entity
data class Recipe(
	// Constructor for Room entity
	@PrimaryKey(autoGenerate = true)
	val id: Long,

	val gourmandId: Int?,

	@NonNull
	val title: String,

	val description: String?,
	val category: String?,
	val cuisine: String?,
	val source: String?,
	val link: String?,
	val rating: Int?,
	val preptime: Int?,
	val cooktime: Int?,
	val yieldValue: Float?,
	val yieldUnit: String?,
	val instructions: String?,
	val notes: String?,
	val image: ByteArray?
) {

	// Constructor for outdoor (non-room) usage
	constructor(
		gourmandId: Int?,
		title: String,
		description: String?,
		category: String?,
		cuisine: String?,
		source: String?,
		link: String?,
		rating: Int?,
		preptime: Int?,
		cooktime: Int?,
		yieldValue: Float?,
		yieldUnit: String?,
		instructions: String?,
		notes: String?,
		image: ByteArray?
	) : this(0,
		gourmandId,
		title,
		description,
		category,
		cuisine,
		source,
		link,
		rating,
		preptime,
		cooktime,
		yieldValue,
		yieldUnit,
		instructions,
		notes,
		image
	)

}