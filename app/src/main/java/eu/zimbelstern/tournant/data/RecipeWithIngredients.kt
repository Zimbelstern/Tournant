package eu.zimbelstern.tournant.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecipeWithIngredients(
	@Embedded
	val recipe: Recipe,

	@Relation(
		parentColumn = "id",
		entityColumn = "recipeId"
	)
	val ingredients: MutableList<Ingredient>
) : Parcelable