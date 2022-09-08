package eu.zimbelstern.tournant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

	companion object {
		private const val FILE_MODE_PREVIEW = 0
		private const val FILE_MODE_IMPORT = 1
		private const val FILE_MODE_LINK = 2
	}

	private lateinit var binding: ActivityMainBinding
	private var searchView: SearchView? = null
	private var titleView: TextView? = null
	private var recipes = MutableLiveData<List<Recipe>>()
	private var fileMode = -1

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		supportActionBar?.also {
			it.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
			it.setCustomView(R.layout.action_bar)
			titleView = it.customView?.findViewById(R.id.action_bar_title)
			searchView = it.customView?.findViewById<SearchView>(R.id.action_bar_search)?.apply {
				if (!isIconified) {
					titleView?.visibility = View.GONE
				}
				setOnSearchClickListener {
					titleView?.visibility = View.GONE
				}
				setOnCloseListener {
					titleView?.visibility = View.VISIBLE
					false
				}
			}
		}

		binding.activityMainCookie.setOnClickListener {
			chooseFile()
		}

		binding.activityMainFileModesExplanation.text =
			HtmlCompat.fromHtml(getString(R.string.file_modes_explanation), FROM_HTML_MODE_COMPACT)

		binding.activityMainRecycler.adapter = RecipeListAdapter(listOf())

		if (intent.action == Intent.ACTION_VIEW)
			showOpenOptions(intent.data as Uri, 2)
		else
			openSavedRecipes(savedInstanceState?.getInt("FILE_MODE"))

		recipes.observe(this) {
			binding.activityMainRecycler.visibility = View.GONE
			binding.activityMainLoading.visibility = View.GONE
			searchView?.apply {
				setQuery(null, false)
				isIconified = true
				visibility = View.GONE
			}
			titleView?.apply {
				text = getString(R.string.app_name)
				titleView?.visibility = View.VISIBLE
			}
			if (it != null) {
				if (it.isEmpty()) {
					if (fileMode == FILE_MODE_PREVIEW)
						openSavedRecipes()
				} else {
					showRecipes(it, fileMode, savedInstanceState?.getCharSequence("SEARCH_QUERY"))
					savedInstanceState?.putCharSequence("SEARCH_QUERY", null)
				}
			} else {
				if (fileMode == FILE_MODE_PREVIEW)
					openSavedRecipes()
			}
			invalidateOptionsMenu()
		}

	}

	/** Checks for an imported or linked recipe file and - if available - parses its contents.
	 * If mode ist null, it uses the file mode stored in the preferences. **/
	private fun openSavedRecipes(mode: Int? = null) {
		fileMode = mode ?: getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getInt("FILE_MODE", -1)
		when (fileMode) {
			FILE_MODE_PREVIEW -> parseRecipes(FileInputStream(File(filesDir, "tmp.xml")))
			FILE_MODE_IMPORT -> parseRecipes(FileInputStream(File(filesDir, "import.xml")))
			FILE_MODE_LINK -> {
				val uri = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).getString("LINKED_FILE_URI", null)?.toUri()
				if (uri != null) {
					try {
						val inputStream = contentResolver.openInputStream(uri)
						if (inputStream == null) {
							Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
						} else {
							parseRecipes(inputStream)
						}
					} catch (e: Exception) {
						Toast.makeText(this, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
					}
				}
			}
			else -> binding.activityMainLoading.visibility = View.GONE
		}
	}

	/** When launched, takes a URI from an OpenDocument intent, caches the (not always) persistable permission
	 * and triggers the choice dialog. **/
	private val getRecipeFileUri = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
			showOpenOptions(it)
		}
	}

	/** Launches [getRecipeFileUri] to get a file. **/
	private fun chooseFile() {
		getRecipeFileUri.launch(arrayOf("application/octet-stream", "application/xml", "text/html", "text/xml"))
	}

	/** Shows the options Preview, Import and Link depending on the context
	 * (link should not show when opened from an ACTION_VIEW intent) and triggers the corresponding methods. **/
	private fun showOpenOptions(uri: Uri, numOfOptions: Int = 3) {
		var choice = 0
		val options = resources.getStringArray(R.array.file_modes).dropLast( 3 - numOfOptions).toTypedArray()
		MaterialAlertDialogBuilder(this)
			.setTitle(getString(R.string.choose_file_option))
			.setSingleChoiceItems(options, 0) { _, which ->
				choice = which
			}
			.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
			.setPositiveButton(getString(R.string.ok)) { _, _ ->
				try {
					val inputStream = contentResolver.openInputStream(uri)
					if (inputStream == null) {
						Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
					} else {
						fileMode = choice
						when (choice) {
							FILE_MODE_PREVIEW -> previewRecipes(inputStream)
							FILE_MODE_IMPORT -> importRecipes(inputStream)
							FILE_MODE_LINK -> linkRecipes(inputStream, uri)
						}
					}
				} catch (e: Exception) {
					Toast.makeText(this, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
				}
			}
			.show()
	}

	/** Stores the inputStream in a temporary file and parses this file. **/
	private fun previewRecipes(inputStream: InputStream) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				inputStream.copyTo(FileOutputStream(File(filesDir, "tmp.xml")))
				parseRecipes(FileInputStream(File(filesDir, "tmp.xml")))
			}
		}
	}

	/** Stores the inputStream in a file, parses the file and stores the file mode in the preferences. **/
	private fun importRecipes(inputStream: InputStream) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				inputStream.copyTo(FileOutputStream(File(filesDir, "import.xml")))
				parseRecipes(FileInputStream(File(filesDir, "import.xml")))
			}
		}
		getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
			.edit()
			.putInt("FILE_MODE", FILE_MODE_IMPORT)
			.apply()
	}

	/** Parses the inputStream, stores the file mode in the preferences and drops obsolete persistedUriPermissions. **/
	private fun linkRecipes(inputStream: InputStream, uri: Uri) {
		parseRecipes(inputStream)
		getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
			.edit()
			.putInt("FILE_MODE", FILE_MODE_LINK)
			.putString("LINKED_FILE_URI", uri.toString())
			.apply()
		for (permission in contentResolver.persistedUriPermissions.dropLast(1)) {
			contentResolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
	}

	/** Parses the inputStream with Dispatchers.IO and posts the recipe list to the live data variable recipes.
	 * In case of errors, posts null to the live data variable. **/
	private fun parseRecipes(inputStream: InputStream) {
		lifecycleScope.launch {
			withContext(Dispatchers.Main) {
				binding.activityMainLoading.visibility = View.VISIBLE
			}
			withContext(Dispatchers.IO) {
				try {
					recipes.postValue(GourmetXmlParser().parse(inputStream).also {
						if (it.isEmpty())
							Toast.makeText(applicationContext, getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
					})
				} catch (e: Exception) {
					withContext(Dispatchers.Main) {
						Toast.makeText(applicationContext, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
					}
				}
				inputStream.close()
			}
		}
	}

	/** Fills the recycler with a RecipeListAdapter, sets the title according to the file mode and enables the search view. **/
	private fun showRecipes(recipes: List<Recipe>, fileMode: Int, filter: CharSequence?) {
		val adapter = RecipeListAdapter(recipes)
		binding.activityMainRecycler.visibility = View.VISIBLE
		binding.activityMainRecycler.adapter = adapter
		titleView?.text = getString(R.string.recipes_with_file_mode, resources.getStringArray(R.array.file_modes_participles)[fileMode])
		searchView?.apply {
			visibility = View.VISIBLE
			isIconified = filter == null || filter.isEmpty()
			setQuery(filter, true)
			clearFocus()
			adapter.filterRecipes(query)
			setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextChange(query: String?): Boolean {
					adapter.filterRecipes(query)
					return true
				}
				override fun onQueryTextSubmit(query: String?) = false
			})
		}
	}

	/** Searches for something. **/
	fun searchForSomething(query: String?) {
		supportActionBar?.customView?.findViewById<SearchView>(R.id.action_bar_search)?.apply {
			setQuery(query, true)
			isIconified = false
			clearFocus()
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		recipes.value?.let {
			outState.putInt("FILE_MODE", fileMode)
			outState.putCharSequence("SEARCH_QUERY", searchView?.query)
		}
		super.onSaveInstanceState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options, menu)
		menu.findItem(R.id.close_file)?.isEnabled = fileMode == FILE_MODE_PREVIEW
		menu.findItem(R.id.show_about)?.title = getString(R.string.about_app_name, getString(R.string.app_name))
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.open_file -> {
				chooseFile()
				true
			}
			R.id.close_file -> {
				recipes.postValue(listOf())
				true
			}
			R.id.show_settings -> {
				startActivity(Intent(this, SettingsActivity::class.java))
				true
			}
			R.id.show_about -> {
				startActivity(Intent(this, AboutActivity::class.java))
				true
			}
			else -> false
		}
	}

	override fun onBackPressed() {
		if (searchView?.isIconified == false) {
			searchView?.setQuery("", false)
			searchView?.isIconified = true
		} else {
			super.onBackPressed()
		}
	}

}