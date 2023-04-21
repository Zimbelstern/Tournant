package eu.zimbelstern.tournant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

	suspend fun insertRecipesWithIngredients(recipes: List<RecipeWithIngredients>) {
		recipes.forEach {
			it.recipe.id = insertRecipe(it.recipe)
		}
		recipes.forEach {
			it.ingredients.forEach { ingredient ->
				ingredient.recipeId = it.recipe.id
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
	abstract fun getRecipeById(id: Int): Flow<RecipeWithIngredients>

	@Query("SELECT id FROM recipe WHERE gourmandId = :gourmandId")
	abstract fun getRecipeIdByGourmandId(gourmandId: Long): Long?

	@Transaction
	@Query("SELECT * FROM recipe ORDER BY title ASC")
	abstract fun getRecipes(): Flow<List<RecipeWithIngredients>>

}