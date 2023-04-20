package eu.zimbelstern.tournant.data

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.DeleteTable
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

	suspend fun insertRecipeWithIngredients(recipe: RecipeWithIngredients) {
		val id = insertRecipe(recipe.recipe)
		recipe.ingredients.forEach {
			it.recipeId = id
		}
		insertIngredients(recipe.ingredients)
	}

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertRecipe(recipe: Recipe): Long

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertIngredients(ingredients: List<Ingredient>)

	// TODO: Implement UPDATE and DELETE
/*	@Update
	abstract suspend fun update(recipe: Recipe)

	@Delete
	abstract suspend fun delete(recipe: Recipe)*/

	@Transaction
	@Query("SELECT * FROM recipe WHERE id = :id")
	abstract fun getRecipe(id: Int): Flow<RecipeWithIngredients>

	@Transaction
	@Query("SELECT * FROM recipe ORDER BY title ASC")
	abstract fun getRecipes(): Flow<List<RecipeWithIngredients>>

}