package eu.zimbelstern.tournant.data.room

import androidx.room.Embedded
import androidx.room.Relation
import com.squareup.moshi.JsonClass
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe

@JsonClass(generateAdapter = true)
data class RecipeWithIngredientsAndPreparations(
	@Embedded
	val recipe: RecipeEntity,

	@Relation(
		parentColumn = "id",
		entityColumn = "recipeId"
	)
	val ingredients: List<IngredientEntity> = listOf(),

	@Relation(
		parentColumn = "id",
		entityColumn = "recipeId"
	)
	val preparations: List<PreparationEntity> = listOf()
) {

	fun toRecipe(): Recipe =
		Recipe(
			id = recipe.id,
			title = recipe.title,
			description = recipe.description,
			category = recipe.category,
			cuisine = recipe.cuisine,
			source = recipe.source,
			link = recipe.link,
			rating = recipe.rating,
			preptime = recipe.preptime,
			cooktime = recipe.cooktime,
			yieldValue = recipe.yieldValue,
			yieldUnit = recipe.yieldUnit,
			instructions = recipe.instructions,
			notes = recipe.notes,
			image = recipe.image,
			thumbnail = recipe.thumbnail,
			created = recipe.created,
			modified = recipe.modified,
			ingredients = ingredients.sortedBy { it.position }.map {
				Ingredient(
					amount = it.amount,
					amountRange = it.amountRange,
					unit = it.unit,
					item = it.item,
					refId = it.refId,
					group = it.group,
					optional = it.optional
				)
			}.toMutableList(),
			preparations = preparations.flatMap { entry -> List(entry.count) { entry.date } }.sorted().toMutableList()
		)

}