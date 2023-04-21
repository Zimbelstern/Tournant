package eu.zimbelstern.tournant.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
	ForeignKey(
		entity = Recipe::class,
		parentColumns = ["id"],
		childColumns = ["recipeId"],
		onUpdate = ForeignKey.CASCADE,
		onDelete = ForeignKey.CASCADE
	)
])
data class Ingredient(
	// Constructor for Room entity
	@PrimaryKey(autoGenerate = true)
	val id: Long,

	var recipeId: Long,
	var position: Int,
	val amount: Float?,
	val amountRange: Float?,
	val unit: String?,
	val item: String?,
	var refId: Long?,
	val group: String?,
	val optional: Boolean
	) {

	// Constructor for simple ingredient
	constructor(
		position: Int,
		amount: Float?,
		amountRange: Float?,
		unit: String?,
		item: String,
		group: String?,
		optional: Boolean
	) : this(0, 0,
		position,
		amount,
		amountRange,
		unit,
		item,
		null,
		group,
		optional
	)

	// Constructor for reference ingredient
	constructor(
		position: Int,
		amount: Float?,
		amountRange: Float?,
		unit: String?,
		refId: Long,
		group: String?,
		optional: Boolean
	) : this(0, 0,
		position,
		amount,
		amountRange,
		unit,
		null,
		refId,
		group,
		optional
	)

}