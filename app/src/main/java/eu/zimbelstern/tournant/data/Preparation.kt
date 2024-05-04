package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import eu.zimbelstern.tournant.utils.RoomTypeConverters
import kotlinx.parcelize.Parcelize
import java.util.Date

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
@TypeConverters(RoomTypeConverters::class)
data class Preparation(
	@PrimaryKey(autoGenerate = true)
	var id: Long,

	@ColumnInfo(index = true)
	var recipeId: Long,
	var date: Date,
) : Parcelable {
	constructor(recipeId: Long, date: Date) : this(0, recipeId, date)
}