package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import kotlinx.parcelize.Parcelize

@Parcelize
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
	var id: Long,

	@ColumnInfo(index = true)
	var recipeId: Long,
	var position: Int,
	var amount: Float?,
	var amountRange: Float?,
	var unit: String?,
	var item: String?,
	var refId: Long?,
	var group: String?,
	var optional: Boolean
) : Parcelable {

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

	fun withScaledAmount(factor: Float): Ingredient {
		if (factor == 1f) {
			return this
		}

		val amountScaled = amount.let {
			it?.times(factor)
			?.roundToNDigits(it.getNumberOfDigits() + 2)
		}

		val amountRangeScaled = amountRange.let {
			it?.times(factor)
			?.roundToNDigits(it.getNumberOfDigits() + 2)
		}

		return this.copy(amount = amountScaled, amountRange = amountRangeScaled)
	}

}