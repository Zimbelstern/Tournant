package eu.zimbelstern.tournant.data

data class RecipeDescription(
	val id: Long,
	val title: String,
	val description: String?,
	val category: String?,
	val cuisine: String?,
	val rating: Float?,
	val image: ByteArray?, // TODO: Replace with thumbnail
	val preptime: Int?,
	val cooktime: Int?,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is RecipeDescription) return false

		if (id != other.id) return false
		if (title != other.title) return false
		if (description != other.description) return false
		if (category != other.category) return false
		if (cuisine != other.cuisine) return false
		if (rating != other.rating) return false
		if (image != null) {
			if (other.image == null) return false
			if (!image.contentEquals(other.image)) return false
		} else if (other.image != null) return false
		if (preptime != other.preptime) return false
		if (cooktime != other.cooktime) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + (description?.hashCode() ?: 0)
		result = 31 * result + (category?.hashCode() ?: 0)
		result = 31 * result + (cuisine?.hashCode() ?: 0)
		result = 31 * result + (rating?.hashCode() ?: 0)
		result = 31 * result + (image?.contentHashCode() ?: 0)
		result = 31 * result + (preptime ?: 0)
		result = 31 * result + (cooktime ?: 0)
		return result
	}
}