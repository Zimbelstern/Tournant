package eu.zimbelstern.tournant.data

import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.data.room.RecipeWithIngredientsAndPreparations

@JsonClass(generateAdapter = true)
data class RecipeList(
	val recipes: List<RecipeWithIngredientsAndPreparations>
)
