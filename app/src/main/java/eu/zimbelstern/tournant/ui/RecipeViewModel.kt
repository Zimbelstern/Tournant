package eu.zimbelstern.tournant.ui

import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.RecipeTitleId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeViewModel(application: TournantApplication, private val recipeId: Long) : AndroidViewModel(application) {

	private val recipeDao = application.database.recipeDao()

	val recipe = recipeDao.getRecipeFlowById(recipeId).onEach { recipeWithIngredients ->
		recipeWithIngredients.ingredients.forEach {
			it.refId?.let { refId ->
				withContext(Dispatchers.IO) {
					it.item = recipeDao.getRecipeTitleById(refId)
				}
			}
		}
	}

	val recipeDates = flow {
		while (true) {
			emit(null)
			delay(MINUTE_IN_MILLIS)
		}
	}.combine(recipe) { _, recipe ->
		Pair(
			recipe.recipe.created?.let {
				DateUtils.getRelativeDateTimeString(application, it.time, MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
			},
			recipe.recipe.modified?.takeIf { it != recipe.recipe.created }?.let {
				DateUtils.getRelativeDateTimeString(application, it.time, MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
			}
		)
	}

	val dependentRecipes = MutableStateFlow(listOf<RecipeTitleId>())

	init {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				dependentRecipes.emit(recipeDao.getDependentRecipeIds(setOf(recipeId)).map {
					RecipeTitleId(it, recipeDao.getRecipeTitleById(it))
				})
			}
		}
	}

}

class RecipeViewModelFactory(private val application: TournantApplication, private val recipeId: Long) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return RecipeViewModel(application, recipeId) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}