package eu.zimbelstern.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.data.RecipeDao
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import kotlinx.coroutines.launch

class MainViewModel(private val recipeDao: RecipeDao) : ViewModel() {

	fun insertRecipes(recipes: List<RecipeWithIngredients>) {
		viewModelScope.launch {
			recipeDao.insertRecipesWithIngredients(recipes)
		}
	}

}

class MainViewModelFactory(private val recipeDao: RecipeDao) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return MainViewModel(recipeDao) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}