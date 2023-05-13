package eu.zimbelstern.tournant.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

	companion object { private const val TAG = "RecipeDao" }

	// Standalone mode: saves recipes in the database
	suspend fun insertRecipesWithIngredients(recipes: List<RecipeWithIngredients>) {
		// TODO: Should throw an error when called from synced mode

		// Stores recipe information except for the ingredients, retrieves the generated ID
		recipes.forEach {
			it.recipe.id = insertRecipe(it.recipe)
		}

		// Stores the ingredients, replaces gourmand refIds with the correct new ones
		recipes.forEach {
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
				// For referenced recipes
				if (ingredient.refId != null) {
					val newRef = getRecipeIdByGourmandId(ingredient.refId!!)
					if (newRef != null)
						ingredient.refId = getRecipeIdByGourmandId(ingredient.refId!!)
					else
						throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}
			insertIngredients(it.ingredients)
		}
	}

	// Synced mode: compares a list of recipes with the database
	suspend fun compareAndUpdateGourmandRecipes(recipes: List<RecipeWithIngredients>) {
		// TODO: Should throw an error when called from standalone mode

		Log.d(TAG, "Updating recipes...")
		// Remove recipes and ingredients not found in file
		getDeprecatedRecipes(recipes.mapNotNull { it.recipe.gourmandId }).forEach {
			Log.d(TAG, "${it.recipe.title} was removed")
			deleteRecipe(it.recipe)
		}

		// Update recipe properties
		recipes.forEach {
			if (it.recipe.gourmandId == null) {
				Log.e(TAG, "Recipe ${it.recipe.title} does not have a Gourmand id")
				return
			}

			val storedRecipe = getRecipeByGourmandId(it.recipe.gourmandId)
			if (storedRecipe == null) {
				Log.d(TAG, "${it.recipe.title} is new")
				it.recipe.id = insertRecipe(it.recipe)
			} else {
				it.recipe.id = storedRecipe.recipe.id
				if (storedRecipe.recipe != it.recipe) {
					// Recipe properties have changed
					Log.d(TAG, "${it.recipe.title} has changed")
					updateRecipe(it.recipe)
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
					val newRef = getRecipeIdByGourmandId(refId)
					ingredient.refId = newRef ?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}

			// Compare ingredients
			val storedIngredients = getRecipeByGourmandId(it.recipe.gourmandId)?.ingredients
			if (storedIngredients != null) {
				storedIngredients.forEach { ing ->
					val new = it.ingredients.find { newIng ->
						ing.refId?.equals(newIng.refId) ?: ing.item.equals(newIng.item)
					}
					if (new == null) {
						Log.d(TAG, "${ing.refId ?: ing.item} was removed")
						deleteIngredient(ing)
					} else {
						it.ingredients.remove(new)
						new.id = ing.id
						if (new != ing) {
							Log.d(TAG, "${ing.refId ?: ing.item} has changed")
							updateIngredient(new)
						} else {
							Log.v(TAG, "${ing.refId ?: ing.item} has not changed")
						}
					}
				}
				Log.d(TAG, "New ingredients: ${it.ingredients.map { ing -> ing.refId ?: ing.item }.joinToString(", ")}")
				insertIngredients(it.ingredients)
			}
		}
	}


	// GETTING
	@Transaction
	@Query("SELECT * FROM recipe WHERE id = :id")
	abstract fun getRecipeById(id: Long): RecipeWithIngredients?

	@Query("SELECT title FROM recipe WHERE id = :id")
	abstract fun getRecipeTitleById(id: Long): String?

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredients?

	@Query("SELECT id FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeIdByGourmandId(gourmandId: Long): Long?

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId NOT IN (:gourmandIds)")
	abstract fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredients>

	@Query("SELECT id, title, category, cuisine, rating, image FROM recipe ORDER BY title ASC")
	abstract fun getPagedRecipeDescriptions(): PagingSource<Int, RecipeDescription>

	@Query("SELECT COUNT(*) FROM recipe")
	abstract fun getRecipeCount(): Flow<Int>

	@Query("SELECT DISTINCT category FROM recipe")
	abstract fun getCategories(): Flow<List<String?>>

	@Query("SELECT DISTINCT cuisine FROM recipe")
	abstract fun getCuisines(): Flow<List<String?>>


	// INSERTING
	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertRecipe(recipe: Recipe): Long

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertIngredients(ingredients: List<Ingredient>)


	// UPDATING
	@Update
	abstract suspend fun updateRecipe(recipe: Recipe)

	@Update
	abstract suspend fun updateIngredient(ingredient: Ingredient)


	// DELETING
	@Delete
	abstract suspend fun deleteRecipe(recipe: Recipe)

	@Delete
	abstract suspend fun deleteIngredient(ingredient: Ingredient)

	@Query("DELETE FROM Recipe")
	abstract suspend fun deleteAllRecipes()

}