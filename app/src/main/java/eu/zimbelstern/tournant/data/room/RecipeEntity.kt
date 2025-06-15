package eu.zimbelstern.tournant.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import eu.zimbelstern.tournant.getAppOrSystemLocale
import eu.zimbelstern.tournant.utils.RoomTypeConverters
import java.util.Date
import java.util.Locale

@Entity(tableName = "Recipe")
@TypeConverters(RoomTypeConverters::class)
data class RecipeEntity(
	@PrimaryKey(autoGenerate = true)
	var id: Long,
	val gourmandId: Int? = null,
	val title: String,
	val description: String?,
	val language: Locale = getAppOrSystemLocale(),
	val category: String?,
	val cuisine: String?,
	val source: String?,
	val link: String?,
	val rating: Float?,
	val preptime: Int?,
	val cooktime: Int?,
	val yieldValue: Double?,
	val yieldUnit: String?,
	val instructions: String?,
	val notes: String?,
	val image: ByteArray?,
	val thumbnail: ByteArray?,
	val created: Date? = Date(),
	val modified: Date? = created,
) {

	@Transient
	var prevId: Long? = null

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is RecipeEntity) return false

		if (id != other.id) return false
		if (gourmandId != other.gourmandId) return false
		if (rating != other.rating) return false
		if (preptime != other.preptime) return false
		if (cooktime != other.cooktime) return false
		if (yieldValue != other.yieldValue) return false
		if (prevId != other.prevId) return false
		if (title != other.title) return false
		if (description != other.description) return false
		if (language != other.language) return false
		if (category != other.category) return false
		if (cuisine != other.cuisine) return false
		if (source != other.source) return false
		if (link != other.link) return false
		if (yieldUnit != other.yieldUnit) return false
		if (instructions != other.instructions) return false
		if (notes != other.notes) return false
		if (!image.contentEquals(other.image)) return false
		if (!thumbnail.contentEquals(other.thumbnail)) return false
		if (created != other.created) return false
		if (modified != other.modified) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + (gourmandId ?: 0)
		result = 31 * result + (rating?.hashCode() ?: 0)
		result = 31 * result + (preptime ?: 0)
		result = 31 * result + (cooktime ?: 0)
		result = 31 * result + (yieldValue?.hashCode() ?: 0)
		result = 31 * result + (prevId?.hashCode() ?: 0)
		result = 31 * result + title.hashCode()
		result = 31 * result + (description?.hashCode() ?: 0)
		result = 31 * result + language.hashCode()
		result = 31 * result + (category?.hashCode() ?: 0)
		result = 31 * result + (cuisine?.hashCode() ?: 0)
		result = 31 * result + (source?.hashCode() ?: 0)
		result = 31 * result + (link?.hashCode() ?: 0)
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