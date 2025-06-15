package eu.zimbelstern.tournant.data.room

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "Keyword",
	primaryKeys = ["recipeId", "position"],
	foreignKeys = [
		ForeignKey(
			entity = RecipeEntity::class,
			parentColumns = ["id"],
			childColumns = ["recipeId"],
			onUpdate = ForeignKey.Companion.CASCADE,
			onDelete = ForeignKey.Companion.CASCADE
		)
	]
)
class KeywordEntity(
	var recipeId: Long,
	var position: Int,
	var keyword: String
)