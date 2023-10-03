package eu.zimbelstern.tournant.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.RecipeTitleId
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class RecipeViewModel(private val application: TournantApplication, private val recipeId: Long) : AndroidViewModel(application) {

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

	suspend fun writeRecipesToExportDir(filename: String) {
		val recipe = listOf(recipeDao.getRecipeById(recipeId))
		val refs = recipeDao.getReferencedRecipes(setOf(recipeId))
		(recipe + refs).forEach {
			val imageFile = File(File(application.filesDir, "images"), "${it.recipe.id}.jpg")
			if (imageFile.exists()) {
				imageFile.inputStream().use {inputStream ->
					val byteArrayOutputStream = ByteArrayOutputStream()
					val image = BitmapFactory.decodeStream(inputStream)
					Bitmap.createScaledBitmap(image, 256, (image.height * 256f / image.width).roundToInt(), true).compress(
						Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
					it.recipe.image = byteArrayOutputStream.toByteArray()
				}
			}
		}
		File(application.filesDir, "export").mkdir()
		File(File(application.filesDir, "export"), "$filename.xml").outputStream().use {
			it.write(GourmetXmlWriter(application.getDecimalSeparator()).serialize(recipe + refs))
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