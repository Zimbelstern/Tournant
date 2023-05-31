package eu.zimbelstern.tournant.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE_LAST_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.ColorfulString
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class MainViewModel(private val application: TournantApplication) : AndroidViewModel(application) {

	companion object { private const val TAG = "MainViewModel" }


	val waitingForRecipes = MutableStateFlow(false)


	// DAO
	private var recipeDao = application.database.recipeDao()


	// SEARCH
	val searchQuery = MutableStateFlow<String?>(null)
	fun search(query: String?) {
		viewModelScope.launch {
			searchQuery.emit(query)
		}
	}


	// RECIPES
	val recipeDescriptions = Pager(
		PagingConfig(pageSize = 10, enablePlaceholders = false)
	) {
		recipeDao.getPagedRecipeDescriptions()
	}.flow
		.cachedIn(viewModelScope)
		.combine(searchQuery) { recipeDescriptions, searchQuery ->
			if (searchQuery?.isNotBlank() == true) {
				recipeDescriptions.filter {
					it.title.contains(searchQuery, true)
							|| it.category?.contains(searchQuery, true) == true
							|| it.cuisine?.contains(searchQuery, true) == true
				}
			} else recipeDescriptions
		}

	val countAllRecipes = recipeDao.getRecipeCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)

	val idsRecipesFiltered = countAllRecipes.combine(searchQuery) { _, query ->
		withContext(Dispatchers.IO) {
			recipeDao.getRecipeIds(query ?: "").toSet()
		}
	}.stateIn(viewModelScope, SharingStarted.Lazily, setOf())

	// CATEGORIES & CUISINES
	private val colors = application.resources.obtainTypedArray(R.array.material_colors_700)
	private val rippleColors = application.resources.obtainTypedArray(R.array.material_colors_900)
	private fun colorString(strings: List<String?>): List<ColorfulString> {
		val list = mutableListOf<ColorfulString>()
		strings.filterNotNull().forEach {
			val pseudoRandomInt = Random(it.hashCode()).nextInt(colors.length())
			val color = colors.getColorStateList(pseudoRandomInt) ?: return@forEach
			val rippleColor = rippleColors.getColorStateList(pseudoRandomInt) ?: return@forEach
			list.add(ColorfulString(it, color, rippleColor))
		}
		return list
	}

	private val allCategories = recipeDao.getCategories().map { colorString(it) }
	val filteredCategories = allCategories.combine(searchQuery) { allCategories, searchQuery ->
		when {
			searchQuery == null -> listOf()
			searchQuery.isNotBlank() -> allCategories.filter { it.string.contains(searchQuery, true) }
			else -> allCategories
		}
	}

	private val allCuisines = recipeDao.getCuisines().map { colorString(it) }
	val filteredCuisines = allCuisines.combine(searchQuery) { allCuisines, searchQuery ->
		when {
			searchQuery == null -> listOf()
			searchQuery.isNotBlank() -> allCuisines.filter { it.string.contains(searchQuery, true) }
			else -> allCuisines
		}
	}

	val allCategoriesAndCuisines = allCategories.combine(allCuisines) { categories, cuisines ->
		categories + cuisines
	}


	private fun insertRecipes(recipeList: List<RecipeWithIngredients>) {
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

	fun parseAndInsertRecipes(uri: Uri) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				waitingForRecipes.emit(true)
				try {
					val inputStream = application.contentResolver.openInputStream(uri)
					if (inputStream == null) {
						Log.e(TAG, "Couldn't update; inputstream null")
						withContext(Dispatchers.Main) {
							Toast.makeText(application, application.getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
						}
					} else {
						val parsedRecipes = GourmetXmlParser().parse(inputStream).also {
							if (it.isEmpty()) {
								withContext(Dispatchers.Main) {
									Toast.makeText(application, application.getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
								}
							}
						}
						insertRecipes(parsedRecipes)
					}
				} catch (e: Exception) {
					Log.e(TAG, "Couldn't update; $e")
					withContext(Dispatchers.Main) {
						Toast.makeText(application, application.getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
					}
				} finally {
					waitingForRecipes.emit(false)
				}
			}
		}
	}

	fun syncWithFile(notify: Boolean = false) {
		val sharedPrefs = application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
		val path = sharedPrefs.getString(PREF_FILE, "")
		if (!path.isNullOrEmpty()) {
			Log.d(TAG, "Syncing with $path")
			val uri = Uri.parse(path)
			val lastModified = DocumentFile.fromSingleUri(application, uri)?.lastModified()
			val lastUpdated = sharedPrefs.getLong(PREF_FILE_LAST_MODIFIED, -1)
			Log.d(TAG, "Updated: $lastUpdated – Modified: $lastModified")
			if (lastModified != null && lastModified > lastUpdated) {
				viewModelScope.launch {
					withContext(Dispatchers.IO) {
						waitingForRecipes.emit(true)
						try {
							val inputStream = application.contentResolver.openInputStream(uri)
							if (inputStream == null) {
								Log.e(TAG, "Couldn't update; inputstream null")
								withContext(Dispatchers.Main) {
									Toast.makeText(application, application.getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
								}
							} else {
								val recipesFromFile = GourmetXmlParser().parse(inputStream).also {
									if (it.isEmpty()) {
										withContext(Dispatchers.Main) {
											Toast.makeText(application, application.getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
										}
									}
								}
								recipeDao.compareAndUpdateGourmandRecipes(recipesFromFile)
							}
							sharedPrefs.edit().putLong(PREF_FILE_LAST_MODIFIED, lastModified).apply()
						} catch (e: Exception) {
							Log.e(TAG, "Couldn't update; $e")
							withContext(Dispatchers.Main) {
								Toast.makeText(application, application.getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
							}
						} finally {
							waitingForRecipes.emit(false)
						}
					}
				}
			} else {
				if (notify)
					Toast.makeText(application, application.getString(R.string.file_not_changed), Toast.LENGTH_LONG).show()
			}
		}
	}

	suspend fun getRecipeTitle(id: Long) = recipeDao.getRecipeTitleById(id)

	suspend fun writeRecipesToExportDir(recipeIds: Set<Long>, filename: String) {
		val recipes = recipeDao.getRecipesById(recipeIds)
		File(application.filesDir, "export").mkdir()
		File(File(application.filesDir, "export"), "$filename.xml").outputStream().use {
			it.write(GourmetXmlWriter().serialize(recipes))
		}
	}

	fun copyRecipesFromExportDir(filename: String, toUri: Uri) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				application.contentResolver.openOutputStream(toUri)?.use { outputStream ->
					File(File(application.filesDir, "export"), "$filename.xml").inputStream().copyTo(outputStream)
				}
				withContext(Dispatchers.Main) {
					Toast.makeText(application, R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun deleteRecipes(recipeIds: Set<Long>) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.deleteRecipesByIds(recipeIds)
				withContext(Dispatchers.Main) {
					Toast.makeText(application, R.string.done, Toast.LENGTH_SHORT).show()
				}
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