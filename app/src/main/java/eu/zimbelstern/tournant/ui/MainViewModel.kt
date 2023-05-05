package eu.zimbelstern.tournant.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.zimbelstern.tournant.Constants.Companion.FILE_LAST_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val application: TournantApplication) : AndroidViewModel(application) {

	companion object { private const val TAG = "MainViewModel" }

	private var recipeDao = application.database.recipeDao()

	fun allRecipes(): Flow<List<RecipeDescription>> = recipeDao.getAllRecipes()

	fun insertRecipes(recipeList: List<RecipeWithIngredients>) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.insertRecipesWithIngredients(recipeList)
			}
		}
	}

	init {
		if (application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
			.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED)
				syncWithFile()
	}

	private fun syncWithFile() {
		val sharedPrefs = application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
		val path = sharedPrefs.getString(PREF_FILE, "")
		if (!path.isNullOrEmpty()) {
			val uri = Uri.parse(path)
			val lastModified = DocumentFile.fromSingleUri(application, uri)?.lastModified()
			if (lastModified != null && lastModified > sharedPrefs.getLong(FILE_LAST_MODIFIED, -1)) {
				try {
					val inputStream = application.contentResolver.openInputStream(uri)
					if (inputStream == null) {
						Log.e(TAG, "Couldn't update; inputstream null")
					} else {
						viewModelScope.launch {
							withContext(Dispatchers.IO) {
								val recipesFromFile = GourmetXmlParser().parse(inputStream)
								recipeDao.compareAndUpdateGourmandRecipes(recipesFromFile)
							}
						}
					}
				} catch (e: Exception) {
					Log.e(TAG, "Couldn't update; $e")
				}
				sharedPrefs.edit().putLong(FILE_LAST_MODIFIED, lastModified).apply()
			}
		}
	}

}

class MainViewModelFactory(private val application: TournantApplication) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return MainViewModel(application) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}

}