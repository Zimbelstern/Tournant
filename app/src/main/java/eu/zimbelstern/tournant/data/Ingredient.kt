package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
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
@JsonClass(generateAdapter = true)
data class Ingredient(
	// Constructor for Room entity
	@PrimaryKey(autoGenerate = true)
	var id: Long,

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
) : Parcelable, IngredientLine {

	// Constructor for simple ingredient
	constructor(
		position: Int,
		amount: Double?,
		amountRange: Double?,
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
		amount: Double?,
		amountRange: Double?,
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

	fun removeEmptyValues() {
		if (unit?.isBlank() == true) unit = null
		if (item?.isBlank() == true) item = null
		if (refId != null) item = null
	}

	fun withScaledAmount(factor: Double): Ingredient {
		if (factor == 1.0) {
			return this
		}

		val amountScaled = amount.let {
			it?.times(factor)?.roundToNDigits(it.getNumberOfDigits() + 1)
		}

		val amountRangeScaled = amountRange.let {
			it?.times(factor)?.roundToNDigits(it.getNumberOfDigits() + 1)
		}

		return this.copy(amount = amountScaled, amountRange = amountRangeScaled)
	}

}