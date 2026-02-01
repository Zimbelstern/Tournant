package eu.zimbelstern.tournant.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.squareup.moshi.JsonDataException
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE_LAST_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_SORT
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.ChipData
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.room.RecipePinEntity
import eu.zimbelstern.tournant.data.room.RecipeWithIngredientsAndPreparations
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.pagination.RecipeDescriptionPagingSource
import eu.zimbelstern.tournant.utils.RecipeJsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.use
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream


@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val application: TournantApplication) : AndroidViewModel(application) {

	companion object { private const val TAG = "MainViewModel" }


	var syncedFileName = MutableStateFlow<String?>(null)
	val waitingForRecipes = MutableStateFlow(false)

	// DAO
	private val recipeDao = application.database.recipeDao()


	// SEARCH
	val searchQuery = MutableStateFlow<String?>(null)
	fun search(query: String?) {
		viewModelScope.launch {
			searchQuery.emit(query)
		}
	}


	// SORTING
	val orderedBy = MutableStateFlow(
		application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
			.getInt(PREF_SORT, 0)
	)
	fun changeOrder(orderBy: Int) {
		viewModelScope.launch {
			orderedBy.emit(orderBy)
		}
		application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
			.edit {
				putInt(PREF_SORT, orderBy)
			}
	}


	// RECIPES
	val countAllRecipes = recipeDao.getRecipeCount().stateIn(viewModelScope, SharingStarted.Lazily, -1)

	val recipeDescriptions = combine(
		searchQuery,
		orderedBy,
		countAllRecipes,
		application.database.invalidationTracker.createFlow("Recipe", "Ingredient", "Preparation", "Keyword", "RecipePin", emitInitialState = true)
	) { searchQuery, orderedBy, _, _ ->
		Pair(searchQuery, orderedBy)
	}.flatMapLatest { (searchQuery, orderedBy) ->
		Pager(PagingConfig(pageSize = 10, initialLoadSize = 30, enablePlaceholders = false)) {
			RecipeDescriptionPagingSource(recipeDao, searchQuery, orderedBy)
		}.flow.cachedIn(viewModelScope)
	}

	val idsRecipesFiltered = countAllRecipes.combine(searchQuery) { _, query ->
		withContext(Dispatchers.IO) {
			recipeDao.getRecipeIds(query ?: "").toSet()
		}
	}.stateIn(viewModelScope, SharingStarted.Lazily, setOf())

	// CATEGORIES & CUISINES

	private fun createChipData(strings: List<String>, keywords: Boolean = false): List<ChipData> {
		val list = mutableListOf<ChipData>()
		strings.forEach {
			val color = (if (keywords) materialColors100 else materialColors700).getRandom(it)
			val rippleColor = (if (keywords) materialColors200 else materialColors900).getRandom(it)
			list.add(ChipData(it, null, color, rippleColor))
		}
		return list
	}

	val allCategories = recipeDao.getAllCategories().map { createChipData(it) }
	val filteredCategories = searchQuery.flatMapLatest { query ->
		if (query != null)
			recipeDao.getCategories(query).combine(allCategories) { filtered, all ->
				filtered.mapNotNull { filter ->
					all.find { it.string == filter.string }.also { it?.count = filter.count }
				}
			}
		else
			listOf(listOf<ChipData>(), listOf()).asFlow()
	}

	val allCuisines = recipeDao.getAllCuisines().map { createChipData(it) }
	val filteredCuisines = searchQuery.flatMapLatest { query ->
		if (query != null)
			recipeDao.getCuisines(query).combine(allCuisines) { filtered, all ->
				filtered.mapNotNull { filter ->
					all.find { it.string == filter.string }.also { it?.count = filter.count }
				}
			}
		else
			listOf(listOf<ChipData>(), listOf()).asFlow()
	}

	val allKeywords = recipeDao.getAllKeywords().map { createChipData(it, keywords = true) }
	val filteredKeywords = searchQuery.flatMapLatest { query ->
		if (query != null)
			recipeDao.getKeywords(query).combine(allKeywords) { filtered, all ->
				filtered.mapNotNull { filter ->
					all.find { it.string == filter.string }.also { it?.count = filter.count }
				}
			}
		else
			listOf(listOf<ChipData>(), listOf()).asFlow()
	}

	// EVENTS
	private val _snackbarEvent = MutableSharedFlow<List<RecipeWithIngredientsAndPreparations>>()
	val snackbarEvent = _snackbarEvent.asSharedFlow()

	init {
		if (application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
			.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED)
				syncWithFile()
	}

	fun parseAndInsertRecipes(uri: Uri) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				waitingForRecipes.emit(true)
				val format = MimeTypeMap.getSingleton().getExtensionFromMimeType(application.contentResolver.getType(uri))
				val inputStream = try {
					application.contentResolver.openInputStream(uri)!!
				} catch (_: Error) {
					Log.e(TAG, "Couldn't update; input not openable")
					withContext(Dispatchers.Main) {
						Toast.makeText(application, application.getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
					}
					return@withContext
				}
				val parsedRecipes = when (format) {
					"json" -> {
						try {
							inputStream.use { readJsonRecipes(it) }
						} catch (e: Error) {
							Toast.makeText(application, application.getString(R.string.unknown_file_error, e), Toast.LENGTH_LONG).show()
							return@withContext
						}
					}
					"xml", "bin" -> {
						try {
							inputStream.use { readXmlRecipes(it) }
						} catch (e: Error) {
							Toast.makeText(application, application.getString(R.string.unknown_file_error, e), Toast.LENGTH_LONG).show()
							return@withContext
						}
					}
					"zip" -> {
						ZipInputStream(inputStream).use { zipIS ->
							try {
								var jsonFound = false
								var recipeList: List<Recipe>? = null
								var entry = zipIS.nextEntry
								while (entry != null) {
									if (entry.isDirectory) {
										error("Malformed zip archive")
									}
									else if (entry.name.endsWith(".json")) {
										if (jsonFound)
											error("Malformed zip archive")
										else
											jsonFound = true
										recipeList = readJsonRecipes(zipIS)
									} else if (entry.name.endsWith(".jpg") && entry.name.dropLast(4).isDigitsOnly()) {
										File(application.filesDir, "import").mkdir()
										File(File(application.filesDir, "import"), entry.name).outputStream().use { fileOS ->
											zipIS.copyTo(fileOS)
										}
									} else {
										error("Malformed zip archive")
									}
									entry = zipIS.nextEntry
								}
								recipeList ?: error("Malformed zip archive")
							} catch (e: Error) {
								error(e.message.toString())
							}
						}
					}
					else -> {
						// Manually detect mime type and open new input stream
						inputStream.use {
							try {
								val firstChar = InputStreamReader(it).readLines()[0][0]
								val newInputStream = application.contentResolver.openInputStream(uri)!!
								when (firstChar) {
									'{' -> readJsonRecipes(newInputStream)
									'<' -> readXmlRecipes(newInputStream)
									else -> throw Error("Unknown file type")
								}
							} catch (e: Error) {
								Toast.makeText(application, application.getString(R.string.unknown_file_error, e), Toast.LENGTH_LONG).show()
								return@withContext
							}
						}
					}
				}
				if (parsedRecipes.isEmpty()) {
					withContext(Dispatchers.Main) {
						Toast.makeText(application, application.getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
					}
				} else {
					try {
						val insertedRecipes = recipeDao.insertRecipesWithIngredientsAndPreparations(parsedRecipes.map { it.toRecipeWithIngredientsAndPreparations() })
						if (format == "zip") {
							insertedRecipes.forEach {
								val imageFile = File(File(application.filesDir, "import"), "${it.recipe.prevId}.jpg")
								if (imageFile.exists()) {
									imageFile.copyTo(File(File(application.filesDir, "images"), "${it.recipe.id}.jpg"))
									imageFile.delete()
								}
							}
						}
						_snackbarEvent.emit(insertedRecipes)
					} catch (e: Error) {
						error(e.message.toString())
					} finally {
						File(application.filesDir, "import").listFiles()?.forEach {
							it.delete()
						}
					}
				}
			}
			waitingForRecipes.emit(false)
		}
	}

	private fun readJsonRecipes(inputStream: InputStream): List<Recipe> {
		val json = inputStream.bufferedReader().readText()
		val recipes = try {
			RecipeJsonAdapter.adapter.fromJson(json)?.recipes
		} catch (e: JsonDataException) {
			Log.w(TAG, "Falling back to old json format because of $e")
			try {
				RecipeJsonAdapter.oldAdapter.fromJson(json)?.recipes?.map { it.toRecipe() }
			} catch (e: JsonDataException) {
				Log.e(TAG, "Malformed json file: $e")
				null
			}
		}
		return recipes ?: listOf()
	}

	private fun readXmlRecipes(inputStream: InputStream): List<Recipe> {
		return GourmetXmlParser(application.getDecimalSeparator()).parse(inputStream)
	}

	fun syncWithFile(notify: Boolean = false) {
		val sharedPrefs = application.getSharedPreferences(application.packageName + "_preferences", Context.MODE_PRIVATE)
		val path = sharedPrefs.getString(PREF_FILE, "")
		if (!path.isNullOrEmpty()) {
			Log.d(TAG, "Syncing with $path")
			val uri = path.toUri()
			val documentFile = DocumentFile.fromSingleUri(application, uri)

			if (documentFile == null) {
				Log.e(TAG, "Couldn't update; documentFile null")
				return
			}

			viewModelScope.launch {
				syncedFileName.emit(documentFile.name)
			}
			val lastModified = documentFile.lastModified()
			val lastUpdated = sharedPrefs.getLong(PREF_FILE_LAST_MODIFIED, -1)
			Log.d(TAG, "Updated: $lastUpdated – Modified: $lastModified")
			if (lastModified > lastUpdated) {
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
								val recipesFromFile = GourmetXmlParser(application.getDecimalSeparator()).parse(inputStream).also {
									if (it.isEmpty()) {
										withContext(Dispatchers.Main) {
											Toast.makeText(application, application.getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
										}
									}
								}
								recipeDao.compareAndUpdateGourmandRecipes(recipesFromFile.map { it.toRecipeWithIngredientsAndPreparations() })
							}
							sharedPrefs.edit { putLong(PREF_FILE_LAST_MODIFIED, lastModified)}
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

	fun getRecipeTitle(id: Long) = recipeDao.getRecipeTitleById(id)

	fun copyRecipesFromExportDir(filename: String, extension: String, toUri: Uri) {
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				application.contentResolver.openOutputStream(toUri)?.use { outputStream ->
					File(File(application.filesDir, "export"), "$filename.$extension").inputStream().copyTo(outputStream)
				}
				withContext(Dispatchers.Main) {
					Toast.makeText(application, R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun getDepRecipes(recipeIds: Set<Long>) = recipeDao.getDependentRecipeIds(recipeIds).toSet()

	fun deleteRecipes(recipeIds: Set<Long>) {
		recipeIds.forEach { id ->
			val imageFile = File(File(application.filesDir, "images"), "${id}.jpg")
			if (imageFile.exists()) {
				imageFile.delete()
			}
		}
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				recipeDao.deleteRecipesByIds(recipeIds)
				withContext(Dispatchers.Main) {
					Toast.makeText(application, R.string.done, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	fun pinRecipes(recipeIds: Set<Long>) {
		viewModelScope.launch { 
			with(Dispatchers.IO) {
				recipeIds.forEach {
					recipeDao.pinRecipe(RecipePinEntity(it))
				}
			}
		}
	}
	
	fun unpinRecipes(recipeIds: Set<Long>) {
		viewModelScope.launch { 
			with(Dispatchers.IO) {
				recipeIds.forEach { 
					recipeDao.unpinRecipe(it)
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