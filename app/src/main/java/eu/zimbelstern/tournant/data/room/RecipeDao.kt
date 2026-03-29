package eu.zimbelstern.tournant.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.OnConflictStrategy.Companion.IGNORE
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

@Dao
abstract class RecipeDao {

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
			CASE WHEN RecipePin.recipeId IS NOT NULL THEN 1 ELSE 0 END AS pinned,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_TOTALTIME THEN preptime + cooktime END AS totaltime,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN (seasonFrom - :month + 12) % 12 END AS seasonStart,
			CASE WHEN :orderedBy / 2 = $SORTED_BY_SEASON THEN (seasonUntil - :month + 12) % 12 END AS seasonEnd
		FROM recipe
		LEFT JOIN Keyword ON Keyword.recipeId = recipe.id
		LEFT JOIN RecipePin ON RecipePin.recipeId = recipe.id
		WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR keyword LIKE '%' || :query || '%'
		GROUP BY recipe.id
		ORDER BY
			RecipePin.recipeId IS NOT NULL DESC,
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


	// Pins

	@Insert(onConflict = IGNORE)
	abstract suspend fun pinRecipe(recipePin: RecipePinEntity): Long

	@Query("DELETE FROM RecipePin WHERE recipeId = :recipeId")
	abstract suspend fun unpinRecipe(recipeId: Long)

}

data class StringAndCount(val string: String, val count: Int)