package eu.zimbelstern.tournant.data.room

import android.util.Log
import androidx.room.Transaction
import java.util.Date

class RecipeRepository(private val dao: RecipeDao) {

	companion object { private const val TAG = "RecipeRepository" }

	fun getRecipeById(id: Long) = dao.getRecipeById(id)
	fun getRecipesById(ids: Set<Long>) = dao.getRecipesById(ids)
	fun getReferencedRecipes(ids: Set<Long>) = dao.getReferencedRecipes(ids)
	fun getRecipeTitlesWithIds() = dao.getRecipeTitlesWithIds()
	fun getRecipeTitleById(id: Long) = dao.getRecipeTitleById(id)
	fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int) = dao.getRecipeDescriptions(query, orderedBy, offset, limit, month)
	fun getKeywords(id: Long) = dao.getKeywords(id)
	fun getRecipeCount() = dao.getRecipeCount()
	fun getRecipeIds(query: String) = dao.getRecipeIds(query)
	fun getDependentRecipeIds(ids: Set<Long>) = dao.getDependentRecipeIds(ids)
	fun getAllCategories() = dao.getAllCategories()
	fun getAllCuisines() = dao.getAllCuisines()
	fun getAllKeywords() = dao.getAllKeywords()
	fun getCategories(query: String) = dao.getCategories(query)
	fun getCuisines(query: String) = dao.getCuisines(query)
	fun getKeywords(query: String) = dao.getKeywords(query)
	fun getSources() = dao.getSources()
	fun getYieldUnits() = dao.getYieldUnits()
	fun getIngredientItems() = dao.getIngredientItems()
	fun getIngredientUnits() = dao.getIngredientUnits()
	suspend fun deleteRecipesByIds(ids: Set<Long>) = dao.deleteRecipesByIds(ids)
	suspend fun deleteAllRecipes() = dao.deleteAllRecipes()
	suspend fun pinRecipe(recipePinEntity: RecipePinEntity) = dao.pinRecipe(recipePinEntity)
	suspend fun unpinRecipe(id: Long) = dao.unpinRecipe(id)

	// Synced mode: compares a list of recipes with the database
	suspend fun compareAndUpdateGourmandRecipes(recipes: List<RecipeWithIngredientsAndPreparations>) {
		Log.d(TAG, "Updating recipes...")
		// Remove recipes and ingredients not found in file
		dao.getDeprecatedRecipes(recipes.mapNotNull { it.recipe.gourmandId }).forEach {
			Log.d(TAG, "${it.recipe.title} was removed")
			dao.deleteRecipe(it.recipe)
		}

		// Update recipe properties
		recipes.forEach {
			if (it.recipe.gourmandId == null) {
				Log.e(TAG, "Recipe ${it.recipe.title} does not have a Gourmand id")
				return
			}

			val storedRecipe = dao.getRecipeByGourmandId(it.recipe.gourmandId)
			if (storedRecipe == null) {
				Log.d(TAG, "${it.recipe.title} is new")
				it.recipe.id = dao.insertRecipe(it.recipe)
			} else {
				it.recipe.id = storedRecipe.recipe.id
				if (storedRecipe.recipe != it.recipe) {
					// Recipe properties have changed
					Log.d(TAG, "${it.recipe.title} has changed")
					dao.updateRecipe(it.recipe)
				} else {
					Log.v(TAG, "${it.recipe.title} has not changed")
				}
			}
		}

		// Update ingredients
		recipes.forEach {
			if (it.recipe.gourmandId == null) return@forEach
			Log.d(TAG, "Storing ingredients of ${it.recipe.title}")

			// Update reference IDs
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
				ingredient.refId?.let { refId ->
					val newRef = dao.getRecipeIdByGourmandId(refId)
					ingredient.refId = newRef ?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}

			// Compare ingredients
			val storedIngredients = dao.getRecipeByGourmandId(it.recipe.gourmandId)?.ingredients
			if (storedIngredients != null) {
				storedIngredients.forEach { ing ->
					val new = it.ingredients.find { newIng ->
						ing.refId?.equals(newIng.refId) ?: ing.item.equals(newIng.item)
					}
					if (new == null) {
						Log.d(TAG, "${ing.refId ?: ing.item} was removed")
						dao.deleteIngredient(ing)
					} else {
						if (new != ing) {
							Log.d(TAG, "${ing.refId ?: ing.item} has changed")
							dao.updateIngredient(new)
						} else {
							Log.v(TAG, "${ing.refId ?: ing.item} has not changed")
						}
					}
				}
				Log.d(TAG, "New ingredients: ${it.ingredients.map { ing -> ing.refId ?: ing.item }.joinToString(", ")}")
				it.ingredients.forEach { ingredient -> dao.insertIngredient(ingredient) }
			}
		}
	}

	suspend fun upsertSingleRecipe(recipe: RecipeWithIngredientsAndPreparations): Long {
		return if (recipe.recipe.id == 0L) {
			dao.insertRecipe(recipe.recipe).also { id ->
				recipe.ingredients.forEach {
					it.recipeId = id
					dao.insertIngredient(it)
				}
				recipe.keywords.forEach {
					it.recipeId = id
					dao.insertKeyword(it)
				}
				recipe.preparations.forEach {
					it.recipeId = id
					dao.insertPreparationDate(it)
				}
			}
		}
		else {
			dao.updateRecipe(recipe.recipe)
			recipe.ingredients.forEach {
				dao.insertIngredient(it)
			}
			dao.deleteIngredientsNotInList(recipe.recipe.id, recipe.ingredients.map { it.position })
			recipe.keywords.forEach {
				dao.insertKeyword(it)
			}
			dao.deleteKeywordsNotInList(recipe.recipe.id, recipe.keywords.map { it.position })
			recipe.preparations.forEach {
				dao.insertPreparationDate(it)
			}
			dao.deletePreparationDatesNotInList(recipe.recipe.id, recipe.preparations.map { it.date.time })
			recipe.recipe.id
		}
	}

	// Standalone mode: saves recipes in the database
	@Transaction
	suspend fun insertRecipesWithIngredientsAndPreparations(recipes: List<RecipeWithIngredientsAndPreparations>): List<RecipeWithIngredientsAndPreparations> {

		// Stores recipe information except for the ingredients, retrieves the generated ID
		recipes.forEach {
			// Save previous id for json parsed recipes
			it.recipe.prevId = it.recipe.id.takeUnless { id -> id == 0L } ?: it.recipe.gourmandId?.toLong()
			it.recipe.id = 0L
			// Insert recipe in database and save id
			it.recipe.id = dao.insertRecipe(it.recipe)
		}

		// Stores the ingredients, replaces gourmand refIds with the correct new ones
		recipes.forEach {
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
				// For referenced recipes
				if (ingredient.refId != null) {
					ingredient.refId = recipes.find { rwi -> rwi.recipe.prevId == ingredient.refId }?.recipe?.id
						?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}
			it.ingredients.forEach { ingredient -> dao.insertIngredient(ingredient) }
			it.keywords.forEach { kw ->
				kw.recipeId = it.recipe.id
				dao.insertKeyword(kw)
			}
		}

		return recipes
	}

	suspend fun addPreparation(recipeId: Long, date: Date) {
		dao.getPreparation(recipeId, date.time)?.let {
			dao.updatePreparationDate(it.copy(count = it.count + 1))
		} ?: dao.insertPreparationDate(PreparationEntity(recipeId, date, 1))
	}

	suspend fun removePreparation(recipeId: Long, date: Date) {
		dao.getPreparation(recipeId, date.time)?.let {
			if (it.count > 1)
				dao.updatePreparationDate(it.copy(count = it.count - 1))
			else
				dao.deletePreparationDate(PreparationEntity(recipeId, date, 1))
		}
	}

}