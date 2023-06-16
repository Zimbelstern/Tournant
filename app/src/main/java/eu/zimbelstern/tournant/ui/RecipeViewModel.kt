package eu.zimbelstern.tournant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.zimbelstern.tournant.data.RecipeDao
import kotlinx.coroutines.flow.onEach

class RecipeViewModel(private val recipeDao: RecipeDao, recipeId: Long) : ViewModel() {

	val recipe = recipeDao.getRecipeFlowById(recipeId).onEach { recipeWithIngredients ->
		recipeWithIngredients.ingredients.forEach {
			it.refId?.let { refId ->
				it.item = recipeDao.getRecipeTitleById(refId)
			}
		}
	}

}

class RecipeViewModelFactory(private val recipeDao: RecipeDao, private val recipeId: Long) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeViewModel(recipeDao, recipeId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}