package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class RecipeWithIngredients(
	@Embedded
	val recipe: Recipe,

	@Relation(
		parentColumn = "id",
		entityColumn = "recipeId"
	)
	val ingredients: MutableList<Ingredient>
) : Parcelable {

	init {
		ingredients.sortBy {
			it.position
		}
	}

}