package eu.zimbelstern.tournant.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.data.RecipeDao
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import kotlinx.coroutines.launch

class RecipeViewModel(private val recipeDao: RecipeDao) : ViewModel() {

	private var _recipe = MutableLiveData<RecipeWithIngredients>()
	val recipe: LiveData<RecipeWithIngredients> get() = _recipe

	fun pullRecipe(recipeId: Long) {
		viewModelScope.launch {
			val targetRecipe = recipeDao.getRecipeById(recipeId)
			targetRecipe.ingredients.forEach {
				it.refId?.let { refId ->
					it.item = recipeDao.getRecipeTitleById(refId)
				}
			}
			_recipe.postValue(targetRecipe)
		}
	}

	fun setRecipe(recipeWithIngredients: RecipeWithIngredients) {
		_recipe.postValue(recipeWithIngredients)
	}

}

class RecipeViewModelFactory(private val recipeDao: RecipeDao) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeViewModel(recipeDao) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}