package eu.zimbelstern.tournant.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "Ingredient",
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
data class IngredientEntity(
	@ColumnInfo(index = true)
	var recipeId: Long,
	var position: Int,
	var amount: Double?,
	var amountRange: Double?,
	var unit: String?,
	var item: String?,
	var refId: Long?,
	var group: String?,
	var optional: Boolean
)