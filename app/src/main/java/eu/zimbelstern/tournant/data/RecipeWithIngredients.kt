package eu.zimbelstern.tournant.data

import androidx.room.Embedded
import androidx.room.Relation

data class RecipeWithIngredients(
	@Embedded
	val recipe: Recipe,

	@Relation(
		parentColumn = "id",
		entityColumn = "recipeId"
	)
	val ingredients: MutableList<Ingredient>
)