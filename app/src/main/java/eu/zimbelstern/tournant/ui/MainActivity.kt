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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.paging.filter
import androidx.recyclerview.widget.ConcatAdapter
import eu.zimbelstern.tournant.BuildConfig
import eu.zimbelstern.tournant.CategoriesCuisinesAdapter
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_VERSION
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeListAdapter
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import eu.zimbelstern.tournant.pagination.RecipeDescriptionLoadStateAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

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

		if (sharedPrefs.getInt(PREF_VERSION, 0) < BuildConfig.VERSION_CODE) {
			migrate()
		}

		mode = sharedPrefs.getInt(PREF_MODE, MODE_STANDALONE)
		sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

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
					if (!viewModel.waitingForRecipes.value)
						binding.root.displayedChild = RECIPES_SCREEN
				}
			    else  {
					supportActionBar?.title = getString(R.string.app_name)
					if (!viewModel.waitingForRecipes.value)
						binding.root.displayedChild = WELCOME_SCREEN
				}
			}
		}

		lifecycleScope.launch {
			viewModel.waitingForRecipes.collectLatest {
				if (it) {
					Log.d(TAG, "Waiting for recipes")
					binding.root.displayedChild = LOADING_SCREEN
				}
				else {
					Log.d(TAG, "Recipes ready")
					if (viewModel.recipeCount.value > 0)
						binding.root.displayedChild = RECIPES_SCREEN
					else
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
			if (mode == MODE_STANDALONE)
				viewModel.parseAndInsertRecipes(intent.data as Uri)
			else
				Toast.makeText(this, R.string.importing_only_in_standalone_mode, Toast.LENGTH_LONG).show()
			intent.action = ""
		}

	}

	private fun importRecipesFromFile() {
		activityResultLauncher.launch(
			arrayOf("application/octet-stream", "application/xml", "text/html", "text/xml")
		)
	}
	private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			viewModel.parseAndInsertRecipes(it)
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
			restartApplication()
		}
		else {
			super.onStart()
			if (syncedFileChanged) {
				viewModel.syncWithFile()
				syncedFileChanged = false
			}
			if (!viewModel.waitingForRecipes.value) {
				binding.root.displayedChild =
					if (viewModel.recipeCount.value > 0) RECIPES_SCREEN
					else WELCOME_SCREEN
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

	private fun migrate() {
		val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
		if (prefs.getInt(PREF_VERSION, 0) < 11) {
			if (prefs.getInt(PREF_MODE, 0) !in (MODE_STANDALONE..MODE_SYNCED)) {
				prefs.edit().putInt(PREF_MODE, MODE_STANDALONE).apply()
			}
			File(filesDir, "tmp.xml").let {
				if (it.exists())
					it.delete()
			}
			File(filesDir, "import.xml").let {
				if (it.exists()) {
					if (prefs.getInt(PREF_MODE, MODE_STANDALONE) == MODE_STANDALONE) {
						viewModel.parseAndInsertRecipes(it.toUri())
						lifecycleScope.launch {
							viewModel.recipeCount.collectLatest { count ->
								if (count > 0 && it.exists())
									it.delete()
							}
						}
					}
					else it.delete()
				}
			}
			prefs.getString("LINKED_FILE_URI", null)?.let {
				prefs.edit().remove("LINKED_FILE_URI").apply()
				if (prefs.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED) {
					prefs.edit().putString(PREF_FILE, it).apply()
					viewModel.syncWithFile()
				}
			}
		}
		prefs.edit().putInt(PREF_VERSION, BuildConfig.VERSION_CODE).apply()
	}

	private fun restartApplication() {
		Log.d(TAG, "Restarting application...")
		startActivity(Intent(this, MainActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		})
		finish()
		Runtime.getRuntime().exit(0)
	}

}