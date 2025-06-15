package eu.zimbelstern.tournant.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
import eu.zimbelstern.tournant.utils.RoomTypeConverters
import java.util.Date

@Entity(
	tableName = "Preparation",
	primaryKeys = ["recipeId", "date"],
	foreignKeys = [
		ForeignKey(
			entity = RecipeEntity::class,
			parentColumns = ["id"],
			childColumns = ["recipeId"],
			onUpdate = ForeignKey.CASCADE,
			onDelete = ForeignKey.CASCADE
		)
	]
)
@TypeConverters(RoomTypeConverters::class)
data class PreparationEntity(
	@ColumnInfo(index = true)
	var recipeId: Long,
	val date: Date,
	val count: Int
)