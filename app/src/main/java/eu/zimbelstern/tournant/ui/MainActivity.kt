package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.paging.filter
import androidx.recyclerview.widget.ConcatAdapter
import eu.zimbelstern.tournant.CategoriesCuisinesAdapter
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeListAdapter
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.pagination.RecipeDescriptionLoadStateAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

	companion object {
		private const val TAG = "MainActivity"
		private const val LOADING_SCREEN = 0
		private const val WELCOME_SCREEN = 1
		private const val RECIPES_SCREEN = 2
	}

	private lateinit var binding: ActivityMainBinding
	private val viewModel: MainViewModel by viewModels {
		MainViewModelFactory(
			application as TournantApplication
		)
	}

	private var searchMenuItem: MenuItem? = null
	private var searchView: SearchView? = null
	private var mode = 0

	private var restartPending = false
	private var syncedFileChanged = false
	private val sharedPrefsListener = OnSharedPreferenceChangeListener { prefs, key ->
		if (key == PREF_MODE) {
			Log.e(TAG, "Mode changed, restart pending")
			restartPending = true
		}
		if (key == PREF_FILE && prefs.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED) {
			Log.e(TAG, "Synced file changed, resync pending")
			syncedFileChanged = true
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val sharedPrefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
		sharedPrefs.getInt(PREF_COLOR_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).let {
			if (it == AppCompatDelegate.MODE_NIGHT_YES || it == AppCompatDelegate.MODE_NIGHT_NO)
				AppCompatDelegate.setDefaultNightMode(it)
		}
		mode = sharedPrefs.getInt(PREF_MODE, MODE_STANDALONE)
		sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.root.displayedChild = LOADING_SCREEN

		binding.welcomeView.apply {
			@SuppressLint("SetTextI18n")
			appNameAndVersion.text = "${getString(R.string.app_name)} ${getString(R.string.versionName)}"
			chooseFile.apply {
				if (mode == MODE_SYNCED) text = getString(R.string.change_synced_file)
				setOnClickListener {
					if (mode == MODE_STANDALONE)
						importRecipesFromFile()
					else
						startActivity(Intent(context, SettingsActivity::class.java))
				}
			}
			if (mode == MODE_SYNCED) modeInfo.text = getString(R.string.mode_synced_description_short)
		}


		val ccAdapter = CategoriesCuisinesAdapter(this)
		val recipeListAdapter = RecipeListAdapter(this).also {
			it.addOnPagesUpdatedListener {
				binding.recipesView.noRecipesMessage.visibility =
					if (it.itemCount == 0)
						View.VISIBLE
					else
						View.GONE
			}
		}


		binding.recipesView.recipeListRecycler.adapter = ConcatAdapter(
			ccAdapter,
			recipeListAdapter.withLoadStateFooter(RecipeDescriptionLoadStateAdapter())
		)


		// RECIPE COUNT
		supportActionBar?.setDisplayShowTitleEnabled(true)
		lifecycleScope.launch {
			viewModel.recipeCount.collectLatest {
				if (it > 0) {
					supportActionBar?.title = getString(R.string.recipes_with_file_mode, it.toString())
					delay(250)
					binding.root.displayedChild = RECIPES_SCREEN
				} else {
					supportActionBar?.title = getString(R.string.app_name)
					delay(250)
					binding.root.displayedChild = WELCOME_SCREEN
				}
			}
		}


		// CATEGORIES & CUISINES
		lifecycleScope.launch {
			viewModel.filteredCategories.combine(viewModel.filteredCuisines) { categories, cuisines ->
				listOf(categories, cuisines)
			}.collectLatest {
				ccAdapter.updateChipAdapters(it)
				delay(250)
				binding.recipesView.recipeListRecycler.layoutManager?.scrollToPosition(0)
			}
		}


		// RECIPES
		lifecycleScope.launch {
			viewModel.recipeDescriptions
				.combine(viewModel.allCategoriesAndCuisines) { recipes, colors ->
					if (colors.isNotEmpty()) recipes else recipes.filter { false }
				}
				.collectLatest {
					Log.d(TAG, "Recipes updated")
					recipeListAdapter.submitData(it)
				}
		}

		lifecycleScope.launch {
			viewModel.allCategoriesAndCuisines.collectLatest {
				recipeListAdapter.updateColors(it)
			}
		}


		if (intent.action == Intent.ACTION_VIEW) {
			parseAndInsertRecipes(intent.data as Uri)
		}

	}

	private fun importRecipesFromFile() {
		activityResultLauncher.launch(
			arrayOf("application/octet-stream", "application/xml", "text/html", "text/xml")
		)
	}
	private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			parseAndInsertRecipes(it)
		}
	}

	private fun parseAndInsertRecipes(uri: Uri) { // TODO: Move to ViewModel
		try {
			val inputStream = contentResolver.openInputStream(uri)
			if (inputStream == null) {
				Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
			} else {
				lifecycleScope.launch {
					binding.root.displayedChild = LOADING_SCREEN
					withContext(Dispatchers.IO) {
						try {
							val parsedRecipes = GourmetXmlParser().parse(inputStream)
							if (parsedRecipes.isEmpty())
								Toast.makeText(applicationContext, getString(R.string.no_recipes_found), Toast.LENGTH_LONG).show()
							else
								viewModel.insertRecipes(parsedRecipes)
						} catch (e: Exception) {
							withContext(Dispatchers.Main) {
								Toast.makeText(applicationContext, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
							}
						}
						inputStream.close()
					}
				}
			}
		} catch (e: Exception) {
			Toast.makeText(this, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
		}
	}

	fun searchForSomething(query: CharSequence?) {
		if (!query.isNullOrEmpty()) {
			searchMenuItem?.expandActionView()
			searchView?.setQuery(query, true)
		}
	}

	fun openRecipeDetail(recipeId: Long) {
		val intent = Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", recipeId)
		}
		startActivity(intent)
	}

	override fun onStart() {
		if (restartPending) {
			Log.d(TAG, "Restarting...")
			startActivity(Intent(this, MainActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			})
			finish()
			Runtime.getRuntime().exit(0)
		}
		else {
			super.onStart()
			if (syncedFileChanged) {
				binding.root.displayedChild = LOADING_SCREEN
				viewModel.syncWithFile()
				syncedFileChanged = false
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options, menu)

		if (mode == MODE_SYNCED) {
			menu.findItem(R.id.import_recipes)?.isVisible = false
			invalidateOptionsMenu()
		}

		menu.findItem(R.id.show_about)?.title = getString(R.string.about_app_name, getString(R.string.app_name))

		// SEARCH
		searchMenuItem = menu.findItem(R.id.search).apply {

			setOnActionExpandListener(object : OnActionExpandListener {
				override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
					Log.d(TAG, "Search expanded")
					viewModel.searchQuery.value.let {
						if (it == null)
							viewModel.search("")
						else
							(actionView as SearchView).setQuery(it, true)
					}
					return true
				}
				override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
					Log.d(TAG, "Search collapsed")
					if (viewModel.searchQuery.value != null)
						viewModel.search(null)
					return true
				}
			})

			searchView = (actionView as SearchView).apply {
				setOnQueryTextListener(object : SearchView.OnQueryTextListener {
					override fun onQueryTextChange(query: String?): Boolean {
						if (viewModel.searchQuery.value != null || !query.isNullOrEmpty())
							viewModel.search(query)
						return true
					}
					override fun onQueryTextSubmit(query: String?): Boolean {
						clearFocus()
						return true
					}
				})
				queryHint = getString(R.string.type_or_tap_to_search)
				maxWidth = Int.MAX_VALUE
			}

		}

		lifecycleScope.launch {
			viewModel.recipeCount.collectLatest {
				searchMenuItem?.apply {
					if (isVisible != (it > 1)) {
						isVisible = it > 1
						invalidateOptionsMenu()
					}
				}
			}
		}

		lifecycleScope.launch {
			viewModel.searchQuery.collectLatest {
				Log.d(TAG, "Search query: $it")
				if (it != null) {
					searchMenuItem?.apply {
						if (!isActionViewExpanded) {
							searchForSomething(it)
						}
					}
				} else {
					searchMenuItem?.apply {
						if (isActionViewExpanded)
							collapseActionView()
					}
				}
			}
		}

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.import_recipes -> {
				importRecipesFromFile()
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

}