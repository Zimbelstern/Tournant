package eu.zimbelstern.tournant.ui

import android.text.format.DateUtils
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.RecipeTitleId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class RecipeViewModel(application: TournantApplication, private val recipeId: Long) : AndroidViewModel(application) {

	private val recipeDao = application.database.recipeDao()

	val recipe = recipeDao.getRecipeById(recipeId)
		.map {
			it.toRecipe()
		}
		.onEach {  recipe ->
			recipe.ingredients.forEach {
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
			recipe.created?.let {
				DateUtils.getRelativeDateTimeString(application, it.time, MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)
			},
			recipe.modified?.takeIf { it != recipe.created }?.let {
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

	fun addPreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.addPreparation(recipeId, date)
				withContext(Dispatchers.Main) {
					Toast.makeText(getApplication(), R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun removePreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.removePreparation(recipeId, date)
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