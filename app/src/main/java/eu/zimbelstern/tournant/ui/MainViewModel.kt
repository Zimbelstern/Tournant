package eu.zimbelstern.tournant.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE_LAST_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_SORT
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.ChipData
import eu.zimbelstern.tournant.data.RecipeList
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import okio.use
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val application: TournantApplication) : AndroidViewModel(application) {

	companion object { private const val TAG = "MainViewModel" }


	var syncedFileName = MutableStateFlow<String?>(null)
	val waitingForRecipes = MutableStateFlow(false)


	private val recipeJsonAdapter: JsonAdapter<RecipeList> = Moshi.Builder().build().adapter(RecipeList::class.java)


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
			.edit()
			.putInt(PREF_SORT, orderBy)
			.apply()
	}


	// RECIPES
	val recipeDescriptions = combine(searchQuery, orderedBy) { s, o -> Pair(s, o) }.flatMapLatest { query ->
		Pager(PagingConfig(pageSize = 10, enablePlaceholders = false)) {
			recipeDao.getPagedRecipeDescriptions(query.first ?: "", query.second)
		}.flow.cachedIn(viewModelScope)
	}

	val countAllRecipes = recipeDao.getRecipeCount().stateIn(viewModelScope, SharingStarted.Lazily, -1)

	val idsRecipesFiltered = countAllRecipes.combine(searchQuery) { _, query ->
		withContext(Dispatchers.IO) {
			recipeDao.getRecipeIds(query ?: "").toSet()
		}
	}.stateIn(viewModelScope, SharingStarted.Lazily, setOf())

	// CATEGORIES & CUISINES
	private val colors = application.resources.obtainTypedArray(R.array.material_colors_700)
	private val rippleColors = application.resources.obtainTypedArray(R.array.material_colors_900)
	private fun createChipData(strings: List<String>): List<ChipData> {
		val list = mutableListOf<ChipData>()
		strings.forEach {
			val pseudoRandomInt = Random(it.hashCode()).nextInt(colors.length())
			val color = colors.getColorStateList(pseudoRandomInt) ?: return@forEach
			val rippleColor = rippleColors.getColorStateList(pseudoRandomInt) ?: return@forEach
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
					val format = MimeTypeMap.getSingleton().getExtensionFromMimeType(application.contentResolver.getType(uri))
					val inputStream = application.contentResolver.openInputStream(uri)
					if (inputStream == null) {
						Log.e(TAG, "Couldn't update; inputstream null")
						withContext(Dispatchers.Main) {
							Toast.makeText(application, application.getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
						}
					} else {
						val parsedRecipes = when (format) {
							"json" -> {
								recipeJsonAdapter.fromJson(inputStream.source().buffer())?.recipes?.onEach { it.clearIds() }
							}
							"xml" -> {
								GourmetXmlParser(application.getDecimalSeparator()).parse(inputStream)
							}
							else -> {
								error("Wrong file format")
							}
						}
						if (parsedRecipes.isNullOrEmpty()) {
							withContext(Dispatchers.Main) {
								Toast.makeText(application, application.getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
							}
						}   else {
							insertRecipes(parsedRecipes)
						}
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

	fun getRecipeTitle(id: Long) = recipeDao.getRecipeTitleById(id)

	fun writeRecipesToExportDir(recipeIds: Set<Long>, filename: String, format: String) {

		val recipes = recipeDao.getRecipesById(recipeIds)
		val refs = recipeDao.getReferencedRecipes(recipeIds)

		if (format != "zip") {
			// Read externally saved images, compress and add to recipe objects
			(recipes + refs).forEach {
				val imageFile = File(File(application.filesDir, "images"), "${it.recipe.id}.jpg")
				if (imageFile.exists()) {
					imageFile.inputStream().use { inputStream ->
						val byteArrayOutputStream = ByteArrayOutputStream()
						val image = BitmapFactory.decodeStream(inputStream)
						Bitmap.createScaledBitmap(image, 256, (image.height * 256f / image.width).roundToInt(), true)
							.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
						it.recipe.image = byteArrayOutputStream.toByteArray()
					}
				}
			}
		}

		File(application.filesDir, "export").mkdir()

		// Write text file with recipes
		val recipeFile = File(File(application.filesDir, "export"), if (format == "xml") "$filename.xml" else "$filename.json")
		recipeFile.outputStream().use {
			it.write(
				if (format == "xml")
					GourmetXmlWriter(application.getDecimalSeparator()).serialize(recipes + refs)
				else
					recipeJsonAdapter.toJson(RecipeList(recipes + refs)).encodeToByteArray()
			)
		}

		if (format == "zip") {
			ZipOutputStream(BufferedOutputStream(File(File(application.filesDir, "export"), "$filename.zip").outputStream())).use { zipOS ->
				zipOS.putNextEntry(ZipEntry("$filename.json"))
				recipeFile.inputStream().use { it.copyTo(zipOS) }

				(recipes + refs).map{ it.recipe.id }.forEach { recipeId ->
					File(File(application.filesDir, "images"), "$recipeId.jpg").let { imageFile ->
						if (imageFile.exists()) {
							zipOS.putNextEntry(ZipEntry("$recipeId.jpg"))
							imageFile.inputStream().use{ it.copyTo(zipOS) }
						}
					}
				}
			}
		}

	}

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