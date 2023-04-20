package eu.zimbelstern.tournant.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import eu.zimbelstern.tournant.getNumberOfDigits
import eu.zimbelstern.tournant.roundToNDigits
import eu.zimbelstern.tournant.toStringForCooks
import kotlinx.parcelize.Parcelize
import kotlin.text.Typography.times

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

	@NonNull
	var recipeId: Long,

	@NonNull
	val position: Int,

	val amount: Float?,
	val amountRange: Float?,
	val unit: String?,
	val item: String?,
	val refId: Long?,
	val group: String?,

	@NonNull
	val optional: Boolean
	) {

	// Constructor for simple ingredient
	// TODO: Set position
	constructor(
		amount: Float?,
		amountRange: Float?,
		unit: String?,
		item: String,
		group: String?,
		optional: Boolean
	) : this(0, 0, 0,
		amount,
		amountRange,
		unit,
		item,
		null,
		group,
		optional
	)

	// Constructor for reference ingredient
	// TODO: Set position
	// TODO: Replace gourmand's refId with ours
	constructor(
		amount: Float?,
		amountRange: Float?,
		unit: String?,
		refId: Long,
		group: String?,
		optional: Boolean
	) : this(0, 0, 0,
		amount,
		amountRange,
		unit,
		null,
		refId,
		group,
		optional
	)

}