package eu.zimbelstern.tournant.data.room

import android.util.Log
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
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPARATIONS_COUNT
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPARED
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_PREPTIME
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_RATING
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_SEASON
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_TITLE
import eu.zimbelstern.tournant.Constants.Companion.SORTED_BY_TOTALTIME
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.data.RecipeTitleId
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
abstract class RecipeDao {

	companion object { private const val TAG = "RecipeDao" }

	@Transaction
	@Query("SELECT * FROM recipe WHERE id = :id")
	abstract fun getRecipeById(id: Long): Flow<RecipeWithIngredientsAndPreparations>

	@Transaction
	@Query("SELECT * FROM recipe WHERE id IN (:ids)")
	abstract fun getRecipesById(ids: Set<Long>): List<RecipeWithIngredientsAndPreparations>

	@Transaction
	@Query("""
		WITH RECURSIVE refs(id) AS (
			SELECT refId FROM ingredient WHERE ingredient.recipeId IN (:recipeIds)
			UNION
			SELECT refId FROM ingredient, refs WHERE ingredient.recipeId = refs.id
		)
		SELECT * FROM recipe WHERE id IN refs AND id NOT IN (:recipeIds)
	""")
	abstract fun getReferencedRecipes(recipeIds: Set<Long>): List<RecipeWithIngredientsAndPreparations>

	@Query("SELECT id, title FROM recipe ORDER BY title COLLATE LOCALIZED ASC")
	abstract fun getRecipeTitlesWithIds(): Flow<List<RecipeTitleId>>

	@Query("SELECT title FROM recipe WHERE id = :id")
	abstract fun getRecipeTitleById(id: Long): String

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeByGourmandId(gourmandId: Int): RecipeWithIngredientsAndPreparations?

	@Query("SELECT id FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeIdByGourmandId(gourmandId: Long): Long?

	@Transaction
	@Query("SELECT * FROM recipe WHERE gourmandId NOT IN (:gourmandIds)")
	abstract fun getDeprecatedRecipes(gourmandIds: List<Int>): List<RecipeWithIngredientsAndPreparations>

	@RewriteQueriesToDropUnusedColumns
	@Query(
		"""
		SELECT
			id, title, description, category, cuisine, rating, seasonFrom, seasonUntil, image, preptime, cooktime, created, modified,
			LENGTH(instructions) AS instructionsLength,
			(SELECT COUNT(*) FROM Ingredient WHERE recipeId = recipe.id) AS ingredientsCount,
			(SELECT COUNT(*) FROM Preparation WHERE recipeId = recipe.id) AS preparationsCount,
			(SELECT date FROM Preparation WHERE recipeId = recipe.id ORDER BY date DESC LIMIT 1) AS prepared,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_TOTALTIME THEN preptime + cooktime END AS totaltime,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN (seasonFrom - :month + 12) % 12 END AS seasonStart,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN (seasonUntil - :month + 12) % 12 END AS seasonEnd
		FROM recipe
		LEFT JOIN Keyword ON recipeId = recipe.id
		WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%'
		GROUP BY recipe.id
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
			CASE WHEN :orderedBy = $SORTED_BY_PREPARATIONS_COUNT * 2 THEN preparationsCount END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPARATIONS_COUNT * 2 + 1 THEN preparationsCount END DESC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPARED * 2 THEN prepared END ASC,
			CASE WHEN :orderedBy = $SORTED_BY_PREPARED * 2 + 1 THEN prepared END DESC,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN
				CASE WHEN seasonFrom IS NOT NULL THEN seasonStart - seasonEnd > 0 ELSE -1 END
			END DESC,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN seasonStart END ASC,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN seasonEnd END ASC,
			title COLLATE LOCALIZED
			LIMIT :limit
			OFFSET :offset
	"""
	)
	abstract fun getRecipeDescriptions(query: String, orderedBy: Int, offset: Int, limit: Int, month: Int): List<RecipeDescription>

	@Query("SELECT keyword FROM Keyword WHERE recipeId = :id ORDER BY position")
	abstract fun getKeywords(id: Long): List<String>

	@Query("SELECT COUNT(*) FROM recipe")
	abstract fun getRecipeCount(): Flow<Int>

	@Query("SELECT id FROM recipe left join keyword on recipeId = recipe.id WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%'")
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

	@Query("SELECT DISTINCT keyword FROM Keyword ORDER BY keyword COLLATE LOCALIZED ASC")
	abstract fun getAllKeywords(): Flow<List<String>>

	@Query("""
		SELECT category AS string, COUNT(*) AS count
		FROM recipe
		LEFT JOIN keyword ON recipeId = recipe.id
		WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%')
			AND category IS NOT NULL
		GROUP BY category ORDER BY category COLLATE LOCALIZED ASC
	""")
	abstract fun getCategories(query: String): Flow<List<StringAndCount>>

	@Query("""
		SELECT cuisine AS string, COUNT(*) AS count
		FROM recipe
		LEFT JOIN keyword ON recipeId = recipe.id
		WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'  OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%')
			AND cuisine IS NOT NULL
		GROUP BY cuisine ORDER BY cuisine COLLATE LOCALIZED ASC
	""")
	abstract fun getCuisines(query: String): Flow<List<StringAndCount>>

	@Query("""
		SELECT keyword AS string, COUNT(*) AS count
		FROM recipe
		LEFT JOIN keyword ON recipeId = recipe.id
		WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'  OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%')
			AND keyword IS NOT NULL
		GROUP BY keyword ORDER BY keyword COLLATE LOCALIZED ASC
	""")
	abstract fun getKeywords(query: String): Flow<List<StringAndCount>>
	
	@Query("SELECT DISTINCT source FROM recipe WHERE source IS NOT NULL ORDER BY source COLLATE LOCALIZED ASC")
	abstract fun getSources(): Flow<List<String>>

	@Query("SELECT DISTINCT yieldUnit FROM recipe WHERE yieldUnit IS NOT NULL ORDER BY yieldUnit COLLATE LOCALIZED ASC")
	abstract fun getYieldUnits(): Flow<List<String>>

	@Query("SELECT DISTINCT item FROM ingredient WHERE item IS NOT NULL ORDER BY item COLLATE LOCALIZED ASC")
	abstract fun getIngredientItems(): Flow<List<String>>

	@Query("SELECT DISTINCT unit FROM ingredient WHERE unit IS NOT NULL ORDER BY unit COLLATE LOCALIZED ASC")
	abstract fun getIngredientUnits(): Flow<List<String>>


	// Recipes

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertRecipe(recipe: RecipeEntity): Long

	@Update
	abstract suspend fun updateRecipe(recipe: RecipeEntity)

	@Delete
	abstract suspend fun deleteRecipe(recipe: RecipeEntity)

	@Query("DELETE FROM Recipe WHERE id IN (:recipeIds)")
	abstract suspend fun deleteRecipesByIds(recipeIds: Set<Long>)

	@Query("DELETE FROM Recipe")
	abstract suspend fun deleteAllRecipes()


	// Ingredients

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertIngredient(ingredient: IngredientEntity)

	@Update
	abstract suspend fun updateIngredient(ingredient: IngredientEntity)

	@Delete
	abstract suspend fun deleteIngredient(ingredient: IngredientEntity)

	@Query("DELETE FROM Ingredient WHERE recipeId = :recipeId AND position NOT IN (:positions)")
	abstract suspend fun deleteIngredientsNotInList(recipeId: Long, positions: List<Int>)


	// Keywords

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertKeyword(preparation: KeywordEntity): Long

	@Query("DELETE FROM Keyword WHERE recipeId = :recipeId AND position not IN (:positions)")
	abstract suspend fun deleteKeywordsNotInList(recipeId: Long, positions: List<Int>)


	// Preparations

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertPreparationDate(preparation: PreparationEntity): Long

	@Update
	abstract suspend fun updatePreparationDate(preparation: PreparationEntity)

	@Delete
	abstract suspend fun deletePreparationDate(preparation: PreparationEntity)

	@Query("DELETE FROM Preparation WHERE recipeId = :recipeId AND date not IN (:dates)")
	abstract suspend fun deletePreparationDatesNotInList(recipeId: Long, dates: List<Long>)

	@Query("SELECT * FROM Preparation WHERE recipeId = :recipeId AND date = :date")
	abstract suspend fun getPreparation(recipeId: Long, date: Long): PreparationEntity?


	suspend fun upsertSingleRecipe(recipe: RecipeWithIngredientsAndPreparations): Long {
		return if (recipe.recipe.id == 0L) {
			insertRecipe(recipe.recipe).also { id ->
				recipe.ingredients.forEach {
					it.recipeId = id
					insertIngredient(it)
				}
				recipe.keywords.forEach {
					it.recipeId = id
					insertKeyword(it)
				}
				recipe.preparations.forEach {
					it.recipeId = id
					insertPreparationDate(it)
				}
			}
		}
		else {
			updateRecipe(recipe.recipe)
			recipe.ingredients.forEach {
				insertIngredient(it)
			}
			deleteIngredientsNotInList(recipe.recipe.id, recipe.ingredients.map { it.position })
			recipe.keywords.forEach {
				insertKeyword(it)
			}
			deleteKeywordsNotInList(recipe.recipe.id, recipe.keywords.map { it.position })
			recipe.preparations.forEach {
				insertPreparationDate(it)
			}
			deletePreparationDatesNotInList(recipe.recipe.id, recipe.preparations.map { it.date.time })
			recipe.recipe.id
		}
	}

	// Standalone mode: saves recipes in the database
	@Transaction
	open suspend fun insertRecipesWithIngredientsAndPreparations(recipes: List<RecipeWithIngredientsAndPreparations>): List<RecipeWithIngredientsAndPreparations> {
		// TODO: Should throw an error when called from synced mode

		// Stores recipe information except for the ingredients, retrieves the generated ID
		recipes.forEach {
			// Save previous id for json parsed recipes
			it.recipe.prevId = it.recipe.id.takeUnless { id -> id == 0L } ?: it.recipe.gourmandId?.toLong()
			it.recipe.id = 0L
			// Insert recipe in database and save id
			it.recipe.id = insertRecipe(it.recipe)
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
			it.ingredients.forEach { ingredient -> insertIngredient(ingredient) }
		}

		return recipes

	}

	// Synced mode: compares a list of recipes with the database
	suspend fun compareAndUpdateGourmandRecipes(recipes: List<RecipeWithIngredientsAndPreparations>) {
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
						if (new != ing) {
							Log.d(TAG, "${ing.refId ?: ing.item} has changed")
							updateIngredient(new)
						} else {
							Log.v(TAG, "${ing.refId ?: ing.item} has not changed")
						}
					}
				}
				Log.d(TAG, "New ingredients: ${it.ingredients.map { ing -> ing.refId ?: ing.item }.joinToString(", ")}")
				it.ingredients.forEach { ingredient -> insertIngredient(ingredient) }
			}
		}
	}

	suspend fun addPreparation(recipeId: Long, date: Date) {
		getPreparation(recipeId, date.time)?.let {
			updatePreparationDate(it.copy(count = it.count + 1))
		} ?: insertPreparationDate(PreparationEntity(recipeId, date, 1))
	}

	suspend fun removePreparation(recipeId: Long, date: Date) {
		getPreparation(recipeId, date.time)?.let {
			if (it.count > 1)
				updatePreparationDate(it.copy(count = it.count - 1))
			else
				deletePreparationDate(PreparationEntity(recipeId, date, 1))
		}
	}

}

data class StringAndCount(val string: String, val count: Int)