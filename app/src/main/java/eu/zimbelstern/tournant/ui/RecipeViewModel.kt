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
import eu.zimbelstern.tournant.addGroupTitles
import eu.zimbelstern.tournant.data.IngredientLine
import eu.zimbelstern.tournant.data.IngredientLine.IngredientGroupTitle
import eu.zimbelstern.tournant.data.IngredientLine.IngredientItem
import eu.zimbelstern.tournant.data.RecipeTitleId
import eu.zimbelstern.tournant.lessYield
import eu.zimbelstern.tournant.logit
import eu.zimbelstern.tournant.moreYield
import eu.zimbelstern.tournant.parseLocalFormattedDoubleOrNull
import eu.zimbelstern.tournant.separator
import eu.zimbelstern.tournant.toStringForCooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class RecipeViewModel(application: TournantApplication, private val recipeId: Long) : AndroidViewModel(application) {

	private val recipeRepository = application.recipeRepository

	private val _recipeYieldValue = MutableStateFlow<Double?>(null)
	private val _targetYieldValue = MutableStateFlow<Double?>(null)
	private val _yieldFromTextField = MutableStateFlow<String?>(null)

	private val _scaleRatio = combine(_recipeYieldValue, _targetYieldValue) { currentYield, targetYield ->
		targetYield?.div(currentYield ?: 1.0) ?: 1.0
	}

	val yieldValueScaled = combine(_recipeYieldValue, _scaleRatio, _yieldFromTextField) { yield, ratio, textField ->
		textField ?: yield?.times(ratio).toStringForCooks(thousands = false)
	}

	private val _ingredients = MutableStateFlow<List<IngredientLine>>(emptyList())
	val ingredientsScaled = combine(_ingredients, _scaleRatio) { ingredients, scale ->
		ingredients.map { item ->
			when (item) {
				is IngredientGroupTitle -> item
				is IngredientItem -> item.copy(
					ingredient = item.ingredient.copy(
						amount = item.ingredient.withScaledAmount(scale).amount,
						amountRange = item.ingredient.withScaledAmount(scale).amountRange
					)
				)
			}
		}
	}

	val recipe = recipeRepository.getRecipeById(recipeId)
		.map {
			it.toRecipe()
		}
		.onEach { recipe ->
			recipe.ingredients.forEach {
				it.refId?.let { refId ->
					withContext(Dispatchers.IO) {
						it.item = recipeRepository.getRecipeTitleById(refId)
					}
				}
			}
			_ingredients.value = recipe.ingredients.addGroupTitles()
			recipe.yieldValue.let {
				_yieldFromTextField.value = null
				_recipeYieldValue.value = it
			}
		}

	fun toggleChecked(id: Int) {
		_ingredients.update { list ->
			list.mapIndexed { i, ingredient ->
				if (i == id && ingredient is IngredientItem)
					ingredient.copy(isChecked = !ingredient.isChecked)
				else
					ingredient
			}
		}
	}

	fun scale(newYieldValue: Double) {
		_targetYieldValue.value = newYieldValue
	}

	fun scale(newYieldValue: String) {
		if (newYieldValue.all { it.isDigit() || it == separator } && newYieldValue.count { it == separator } <= 1) {
			newYieldValue.parseLocalFormattedDoubleOrNull()?.let {
				_targetYieldValue.value = it
				_yieldFromTextField.value = newYieldValue
			}
			if (newYieldValue.isEmpty()) {
				_targetYieldValue.value = null
				_yieldFromTextField.value = ""
			}
		}
	}

	fun scale(ingredientPosition: Int, scale: String) {
		val unscaledAmount = (_ingredients.value[ingredientPosition] as? IngredientItem)?.ingredient?.amount
		if (unscaledAmount == null ) {
			logit { "RecipeViewModel::scale($ingredientPosition, $scale): unscaledAmount is null" }
			return
		}
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			scale.toDouble() / unscaledAmount * (_recipeYieldValue.value ?: 1.0)
		}
	}

	fun scaleUp() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			(it ?: _recipeYieldValue.value).moreYield()
		}
	}

	fun scaleDown() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			(it ?: _recipeYieldValue.value).lessYield()
		}
	}

	fun scaleReset() {
		_targetYieldValue.update {
			_yieldFromTextField.value = null
			_recipeYieldValue.value
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
				dependentRecipes.emit(recipeRepository.getDependentRecipeIds(setOf(recipeId)).map {
					RecipeTitleId(it, recipeRepository.getRecipeTitleById(it))
				})
			}
		}
	}

	fun addPreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeRepository.addPreparation(recipeId, date)
				withContext(Dispatchers.Main) {
					Toast.makeText(getApplication(), R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun removePreparation(date: Date) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeRepository.removePreparation(recipeId, date)
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