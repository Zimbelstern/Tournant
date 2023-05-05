package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Recipe(
	// Constructor for Room entity
	@PrimaryKey(autoGenerate = true)
	var id: Long,

	val gourmandId: Int?,
	val title: String,
	val description: String?,
	val category: String?,
	val cuisine: String?,
	val source: String?,
	val link: String?,
	val rating: Float?,
	val preptime: Int?,
	val cooktime: Int?,
	val yieldValue: Float?,
	val yieldUnit: String?,
	val instructions: String?,
	val notes: String?,
	val image: ByteArray?,
	val thumbnail: ByteArray?
) : Parcelable {

	// Constructor for outdoor (non-room) usage
	constructor(
		gourmandId: Int?,
		title: String,
		description: String?,
		category: String?,
		cuisine: String?,
		source: String?,
		link: String?,
		rating: Float?,
		preptime: Int?,
		cooktime: Int?,
		yieldValue: Float?,
		yieldUnit: String?,
		instructions: String?,
		notes: String?,
		image: ByteArray?,
		thumbnail: ByteArray?
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
		image,
		thumbnail
	)

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
		return result
	}

}