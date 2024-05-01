package eu.zimbelstern.tournant.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_COOKTIME
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_CREATED
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_INGREDIENTS_COUNT
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_INSTRUCTIONS_LENGTH
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPTIME
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_RATING
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_TITLE
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_TOTALTIME
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

	companion object { private const val TAG = "RecipeDao" }

	// Standalone mode: saves recipes in the database
	@Transaction
	open suspend fun insertRecipesWithIngredients(recipes: List<RecipeWithIngredients>): List<RecipeWithIngredients> {
		// TODO: Should throw an error when called from synced mode

		// Stores recipe information except for the ingredients, retrieves the generated ID
		recipes.forEach {
			// Save previous id for json parsed recipes
			if (it.recipe.id != 0L) {
				it.recipe.prevId = it.recipe.id
				it.recipe.id = 0
			} else {
				it.recipe.prevId = it.recipe.gourmandId?.toLong()
			}
			// Insert recipe in database and save id
			it.recipe.id = insertRecipe(it.recipe)
		}

		// Stores the ingredients, replaces gourmand refIds with the correct new ones
		recipes.forEach {
			it.ingredients.forEach { ingredient ->
				ingredient.id = 0
				ingredient.recipeId = it.recipe.id
				// For referenced recipes
				if (ingredient.refId != null) {
					ingredient.refId = recipes.find { rwi -> rwi.recipe.prevId == ingredient.refId }?.recipe?.id
						?: throw Error("Error while saving ${it.recipe.title} to database: Referenced recipe not found")
				}
			}
			insertIngredients(it.ingredients)
		}

		return recipes

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
	abstract fun getRecipeById(id: Long): RecipeWithIngredients

	@Transaction
	@Query("SELECT * FROM recipe WHERE id = :id")
	abstract fun getRecipeFlowById(id: Long): Flow<RecipeWithIngredients>

	@Transaction
	@Query("SELECT * FROM recipe WHERE id IN (:ids)")
	abstract fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredients>

	@Transaction
	@Query("""
		WITH RECURSIVE refs(id) AS (
			SELECT refId FROM ingredient WHERE ingredient.recipeId IN (:recipeIds)
			UNION
			SELECT refId FROM ingredient, refs WHERE ingredient.recipeId = refs.id
		)
		SELECT * FROM recipe WHERE id IN refs AND id NOT IN (:recipeIds)
	""")
	abstract fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredients>

	@Query("SELECT id, title FROM recipe ORDER BY title COLLATE LOCALIZED ASC")
	abstract fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>>

	@Query("SELECT title FROM recipe WHERE id = :id")
	abstract fun getRecipeTitleById(id: Long): String

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredients?

	@Query("SELECT id FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeIdByGourmandId(gourmandId: Long): Long?

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId NOT IN (:gourmandIds)")
	abstract fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredients>

	@RewriteQueriesToDropUnusedColumns
	@Query("""
		SELECT
			id, title, description, category, cuisine, rating, image, preptime, cooktime, created, modified, LENGTH(instructions) AS instructionsLength, (SELECT COUNT(*) FROM Ingredient WHERE recipeId = recipe.id) AS ingredientsCount,
			CASE WHEN :orderedBy = $SORTED_BY_TOTALTIME * 2 OR :orderedBy = $SORTED_BY_TOTALTIME * 2 + 1 THEN preptime + cooktime END AS totaltime
		FROM recipe
		WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%'
		ORDER BY
			CASE WHEN :orderedBy = $SORTED_BY_TITLE * 2 THEN title COLLATE LOCALIZED END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_TITLE * 2 + 1 THEN title COLLATE LOCALIZED END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_RATING * 2 AND rating NOTNULL THEN rating ELSE 6 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_RATING * 2 + 1 THEN rating END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPTIME * 2 AND preptime NOTNULL THEN 0 ELSE 1 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPTIME * 2 THEN preptime * 2 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPTIME * 2 + 1 THEN preptime END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_COOKTIME * 2 AND cooktime NOTNULL THEN 0 ELSE 1 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_COOKTIME * 2 THEN cooktime END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_COOKTIME * 2 + 1 THEN cooktime END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_TOTALTIME * 2 AND totaltime NOTNULL THEN 0 ELSE 1 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_TOTALTIME * 2 THEN totaltime END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_TOTALTIME * 2 + 1 THEN totaltime END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_CREATED * 2 AND created NOTNULL THEN 0 ELSE 1 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_CREATED * 2 THEN created END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_CREATED * 2 + 1 THEN created END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_MODIFIED * 2 AND modified THEN 0 ELSE 1 END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_MODIFIED * 2 THEN modified END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_MODIFIED * 2 + 1 THEN modified END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_INSTRUCTIONS_LENGTH * 2 THEN instructionsLength END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_INSTRUCTIONS_LENGTH * 2 + 1 THEN instructionsLength END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_INGREDIENTS_COUNT * 2 THEN ingredientsCount END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_INGREDIENTS_COUNT * 2 + 1 THEN ingredientsCount END DESC,
			title COLLATE LOCALIZED
	""")
	abstract fun getPagedRecipeDescriptions(query: String, orderedBy: Int): PagingSource<Int, RecipeDescription>

	@Query("SELECT COUNT(*) FROM recipe")
	abstract fun getRecipeCount(): Flow<Int>

	@Query("SELECT id FROM recipe WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%'")
	abstract fun getRecipeIds(query: String): List<Long>

	@Query("""
		WITH RECURSIVE deps(id) AS (
			SELECT recipeId FROM ingredient WHERE ingredient.refId IN (:recipeIds)
			UNION
			SELECT recipeId FROM ingredient, deps WHERE ingredient.refId = deps.id
		)
		SELECT * FROM deps WHERE id NOT IN (:recipeIds)
	""")
	abstract fun getDependentRecipeIds(recipeIds: Set<Long>): List<Long>

	@Query("SELECT DISTINCT category FROM recipe WHERE category IS NOT NULL ORDER BY category COLLATE LOCALIZED ASC")
	abstract fun getAllCategories(): Flow<List<String>>

	@Query("SELECT DISTINCT cuisine FROM recipe WHERE cuisine IS NOT NULL ORDER BY cuisine COLLATE LOCALIZED ASC")
	abstract fun getAllCuisines(): Flow<List<String>>


	@Query("""
		SELECT category AS string, COUNT(*) AS count
		FROM recipe
		WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%') AND category IS NOT NULL
		GROUP BY category ORDER BY category COLLATE LOCALIZED ASC
	""")
	abstract fun getCategories(query: String): Flow<List<StringAndCount>>

	@Query("""
		SELECT cuisine AS string, COUNT(*) AS count
		FROM recipe
		WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'  OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%')
			AND cuisine IS NOT NULL
		GROUP BY cuisine ORDER BY cuisine COLLATE LOCALIZED ASC
	""")
	abstract fun getCuisines(query: String): Flow<List<StringAndCount>>

	@Query("SELECT DISTINCT source FROM recipe WHERE source IS NOT NULL ORDER BY source COLLATE LOCALIZED ASC")
	abstract fun getSources(): Flow<List<String>>

	@Query("SELECT DISTINCT yieldUnit FROM recipe WHERE yieldUnit IS NOT NULL ORDER BY yieldUnit COLLATE LOCALIZED ASC")
	abstract fun getYieldUnits(): Flow<List<String>>

	@Query("SELECT DISTINCT item FROM ingredient WHERE item IS NOT NULL ORDER BY item COLLATE LOCALIZED ASC")
	abstract fun getIngredientItems(): Flow<List<String>>


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

	@Query("DELETE FROM Recipe WHERE id IN (:recipeIds)")
	abstract suspend fun deleteRecipesByIds(recipeIds: Set<Long>)

	@Delete
	abstract suspend fun deleteIngredient(ingredient: Ingredient)

	@Query("DELETE FROM Ingredient WHERE id IN (:ingredientIds)")
	abstract suspend fun deleteIngredientsById(ingredientIds: Set<Long>)

	@Query("DELETE FROM Recipe")
	abstract suspend fun deleteAllRecipes()

}

data class StringAndCount(val string: String, val count: Int)