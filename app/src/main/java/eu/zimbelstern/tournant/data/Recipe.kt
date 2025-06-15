package eu.zimbelstern.tournant.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.data.room.IngredientEntity
import eu.zimbelstern.tournant.data.room.KeywordEntity
import eu.zimbelstern.tournant.data.room.PreparationEntity
import eu.zimbelstern.tournant.data.room.RecipeEntity
import eu.zimbelstern.tournant.data.room.RecipeWithIngredientsAndPreparations
import eu.zimbelstern.tournant.getAppOrSystemLocale
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Locale

@Parcelize
@JsonClass(generateAdapter = true)
data class Recipe(
	val id: Long = 0,
	var gourmandId: Int? = null,
	var title: String,
	var description: String? = null,
	var language: Locale = getAppOrSystemLocale(),
	var category: String? = null,
	var cuisine: String? = null,
	var keywords: LinkedHashSet<String> = linkedSetOf(),
	var source: String? = null,
	var link: String? = null,
	var rating: Float? = null,
	var preptime: Int? = null,
	var cooktime: Int? = null,
	var yieldValue: Double? = null,
	var yieldUnit: String? = null,
	var instructions: String? = null,
	var notes: String? = null,
	var image: ByteArray? = null,
	var thumbnail: ByteArray? = null,
	var created: Date? = Date(),
	var modified: Date? = created,
	val ingredients: MutableList<Ingredient> = mutableListOf(),
	val preparations: MutableList<Date> = mutableListOf(),
) : Parcelable {

	fun toRecipeWithIngredientsAndPreparations(): RecipeWithIngredientsAndPreparations =
		RecipeWithIngredientsAndPreparations(
			RecipeEntity(
				id = id,
				gourmandId = gourmandId,
				title = title,
				description = description,
				language = language,
				category = category,
				cuisine = cuisine,
				source = source,
				link = link,
				rating = rating,
				preptime = preptime,
				cooktime = cooktime,
				yieldValue = yieldValue,
				yieldUnit = yieldUnit,
				instructions = instructions,
				notes = notes,
				image = image,
				thumbnail = thumbnail,
				created = created,
				modified = modified
			),
			ingredients.mapIndexed { i, it -> IngredientEntity(
				recipeId = id,
				position = i,
				amount = it.amount,
				amountRange = it.amountRange,
				unit = it.unit,
				item = it.item,
				refId = it.refId,
				group = it.group,
				optional = it.optional
			) },
			keywords.mapIndexed { i, it -> KeywordEntity(id, i, it) },
			preparations.groupBy { it }.map { (date, elements) -> PreparationEntity(id, date, elements.size) }
		)

	fun processModifications() {
		if (description?.isBlank() == true) description = null
		if (category?.isBlank() == true) category = null
		if (cuisine?.isBlank() == true) cuisine = null
		if (source?.isBlank() == true) source = null
		if (link?.isBlank() == true) link = null
		if (cooktime == 0) cooktime = null
		if (preptime == 0) preptime = null
		if (yieldUnit?.isBlank() == true) yieldUnit = null
		if (yieldValue == 0.0) yieldValue = if (yieldUnit != null) 1.0 else null
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
		if (language != other.language) return false
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
		if (ingredients != other.ingredients) return false
		if (keywords != other.keywords) return false
		if (preparations != other.preparations) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + (gourmandId ?: 0)
		result = 31 * result + title.hashCode()
		result = 31 * result + (description?.hashCode() ?: 0)
		result = 31 * result + language.hashCode()
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
		result = 31 * result + (ingredients.hashCode())
		result = 31 * result + (keywords.hashCode())
		result = 31 * result + (preparations.hashCode())
		return result
	}

}