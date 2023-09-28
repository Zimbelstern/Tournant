package eu.zimbelstern.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.data.IngredientLine
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.RecipeDao
import eu.zimbelstern.tournant.deflate
import eu.zimbelstern.tournant.inflate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeEditingViewModel(private val recipeDao: RecipeDao, private val recipeId: Long) : ViewModel() {

	val recipe = MutableStateFlow(Recipe())
	val ingredients = MutableStateFlow(mutableListOf<IngredientLine>())
	val titlesWithIds = recipeDao.getRecipeTitlesWithIds()
	val categoryStrings = recipeDao.getCategories()
	val cuisineStrings = recipeDao.getCuisines()
	val sourceStrings = recipeDao.getSources()
	val yieldUnitStrings = recipeDao.getYieldUnits()
	val ingredientStrings = recipeDao.getIngredientItems()

	init {
		if (recipeId != 0L)
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					recipeDao.getRecipeById(recipeId).let {
						recipe.emit(it.recipe)
						ingredients.emit(it.ingredients.inflate())
					}
				}
			}
	}

	var savedWithId = MutableStateFlow(0L)
	var ingredientsRemoved = mutableListOf<Long>()

	fun saveRecipe() {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.deleteIngredientsById(ingredientsRemoved.toSet())
				recipe.value.removeEmptyValues()
				val ingredientList = ingredients.value.deflate()
				ingredientList.forEach { it.removeEmptyValues() }
				if (recipeId == 0L) {
					val id = recipeDao.insertRecipe(recipe.value.apply { removeEmptyValues() })
					ingredientList.forEach { it.recipeId = id }
					recipeDao.insertIngredients(ingredientList)
					savedWithId.emit(id)
				}
				else {
					recipeDao.updateRecipe(recipe.value)
					ingredientList.filter { it.id != 0L }.forEach {
						recipeDao.updateIngredient(it)
					}
					recipeDao.insertIngredients(ingredientList.filter { it.id == 0L }.onEach {
						it.recipeId = recipeId
					})
					savedWithId.emit(recipe.value.id)
				}
			}
		}
	}

}

class RecipeEditingViewModelFactory(private val recipeDao: RecipeDao, private val recipeId: Long) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeEditingViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeEditingViewModel(recipeDao, recipeId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}