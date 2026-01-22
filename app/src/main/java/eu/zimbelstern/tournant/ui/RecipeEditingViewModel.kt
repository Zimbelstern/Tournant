package eu.zimbelstern.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.addGroupTitles
import eu.zimbelstern.tournant.data.IngredientLine
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.room.RecipeDao
import eu.zimbelstern.tournant.hideGroupTitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeEditingViewModel(private val recipeDao: RecipeDao, private val recipeId: Long) : ViewModel() {

	val recipe = MutableStateFlow(Recipe(title = ""))
	val ingredients = MutableStateFlow(mutableListOf<IngredientLine>())
	val titlesWithIds = recipeDao.getRecipeTitlesWithIds()
	val categoryStrings = recipeDao.getAllCategories()
	val cuisineStrings = recipeDao.getAllCuisines()
	val sourceStrings = recipeDao.getSources()
	val yieldUnitStrings = recipeDao.getYieldUnits()
	val ingredientItemSuggestions = recipeDao.getIngredientItems()
	val ingredientUnitSuggestions = recipeDao.getIngredientUnits()

	init {
		if (recipeId != 0L)
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					recipeDao.getRecipeById(recipeId).map { it.toRecipe() }.collectLatest {
						recipe.emit(it)
						ingredients.emit(it.ingredients.addGroupTitles())
					}
				}
			}
	}

	var savedWithId = MutableStateFlow(0L)

	fun saveRecipe() {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				val ingredientList = ingredients.value.hideGroupTitles().onEach { it.removeEmptyValues() }
				val id = recipeDao.upsertSingleRecipe(
					recipe.value.apply {
						processModifications()
						ingredients.clear()
						ingredients.addAll(ingredientList)
					}.toRecipeWithIngredientsAndPreparations()
				)
				savedWithId.emit(id)
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