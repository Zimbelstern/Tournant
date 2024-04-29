package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.utils.RoomTypeConverters
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity
@JsonClass(generateAdapter = true)
@TypeConverters(RoomTypeConverters::class)
data class Recipe(
	// Constructor for Room entity
	@PrimaryKey(autoGenerate = true)
	var id: Long,

	@Transient
	var prevId: Long? = null,

	@field:Json(ignore = true)
	val gourmandId: Int? = null,

	var title: String,
	var description: String?,
	var category: String?,
	var cuisine: String?,
	var source: String?,
	var link: String?,
	var rating: Float?,
	var preptime: Int?,
	var cooktime: Int?,
	var yieldValue: Float?,
	var yieldUnit: String?,
	var instructions: String?,
	var notes: String?,
	var image: ByteArray?,
	var thumbnail: ByteArray?,
	var created: Date? = Date(),
	var modified: Date? = null,
) : Parcelable {

	// Constructor for outdoor (non-room) usage
	constructor(
		gourmandId: Int? = null,
		title: String = "",
		description: String? = null,
		category: String? = null,
		cuisine: String? = null,
		source: String? = null,
		link: String? = null,
		rating: Float? = null,
		preptime: Int? = null,
		cooktime: Int? = null,
		yieldValue: Float? = null,
		yieldUnit: String? = null,
		instructions: String? = null,
		notes: String? = null,
		image: ByteArray? = null,
		thumbnail: ByteArray? = null,
		created: Date? = Date(),
		modified: Date? = null,
	) : this(0,
		null,
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
		image,
		thumbnail,
		created,
		modified,
	)

	fun processModifications() {
		if (category?.isBlank() == true) category = null
		if (cuisine?.isBlank() == true) cuisine = null
		if (source?.isBlank() == true) source = null
		if (link?.isBlank() == true) link = null
		if (cooktime == 0) cooktime = null
		if (preptime == 0) preptime = null
		if (yieldUnit?.isBlank() == true) yieldUnit = null
		if (yieldValue == 0f) yieldValue = if (yieldUnit != null) 1f else null
		if (instructions?.isBlank() == true) category = null
		if (notes?.isBlank() == true) category = null
		modified = Date()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Recipe) return false

		if (id != other.id) return false
		if (gourmandId != other.gourmandId) return false
		if (title != other.title) return false
		if (description != other.description) return false
		if (category != other.category) return false
		if (cuisine != other.cuisine) return false
		if (source != other.source) return false
		if (link != other.link) return false
		if (rating != other.rating) return false
		if (preptime != other.preptime) return false
		if (cooktime != other.cooktime) return false
		if (yieldValue != other.yieldValue) return false
		if (yieldUnit != other.yieldUnit) return false
		if (instructions != other.instructions) return false
		if (notes != other.notes) return false
		if (image != null) {
			if (other.image == null) return false
			if (!image.contentEquals(other.image)) return false
		} else if (other.image != null) return false
		if (thumbnail != null) {
			if (other.thumbnail == null) return false
			if (!thumbnail.contentEquals(other.thumbnail)) return false
		} else if (other.thumbnail != null) return false
		if (created != other.created) return false
		if (modified != other.modified) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + (gourmandId ?: 0)
		result = 31 * result + title.hashCode()
		result = 31 * result + (description?.hashCode() ?: 0)
		result = 31 * result + (category?.hashCode() ?: 0)
		result = 31 * result + (cuisine?.hashCode() ?: 0)
		result = 31 * result + (source?.hashCode() ?: 0)
		result = 31 * result + (link?.hashCode() ?: 0)
		result = 31 * result + (rating?.hashCode() ?: 0)
		result = 31 * result + (preptime ?: 0)
		result = 31 * result + (cooktime ?: 0)
		result = 31 * result + (yieldValue?.hashCode() ?: 0)
		result = 31 * result + (yieldUnit?.hashCode() ?: 0)
		result = 31 * result + (instructions?.hashCode() ?: 0)
		result = 31 * result + (notes?.hashCode() ?: 0)
		result = 31 * result + (image?.contentHashCode() ?: 0)
		result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
		result = 31 * result + (created?.hashCode() ?: 0)
		result = 31 * result + (modified?.hashCode() ?: 0)
		return result
	}

}