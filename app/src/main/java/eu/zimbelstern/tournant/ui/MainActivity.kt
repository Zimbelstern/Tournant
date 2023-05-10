package eu.zimbelstern.tournant.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.filter
import androidx.recyclerview.widget.ConcatAdapter
import eu.zimbelstern.tournant.CategoriesCuisinesAdapter
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeListAdapter
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.pagination.RecipeDescriptionLoadStateAdapter
import kotlinx.coroutines.Dispatchers
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

	private lateinit var titleView: TextView
	private lateinit var searchView: SearchView
	private var fileMode = 0

	private var restartPending = false
	private val sharedPrefsListener = OnSharedPreferenceChangeListener { _, key ->
		if (key == PREF_MODE) {
			restartPending = true
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val sharedPrefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
		sharedPrefs.getInt(PREF_COLOR_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).let {
			if (it == AppCompatDelegate.MODE_NIGHT_YES || it == AppCompatDelegate.MODE_NIGHT_NO)
				AppCompatDelegate.setDefaultNightMode(it)
		}
		fileMode = sharedPrefs.getInt(PREF_MODE, MODE_STANDALONE)
		sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.root.displayedChild = LOADING_SCREEN

		val ccAdapter = CategoriesCuisinesAdapter(this)
		val recipeListAdapter = RecipeListAdapter(this).also {
			it.addLoadStateListener { loadStates ->
				if (loadStates.refresh is LoadState.NotLoading && it.itemCount == 0) {
					binding.root.displayedChild = WELCOME_SCREEN
					searchView.visibility = View.GONE
				}
				else {
					binding.root.displayedChild = RECIPES_SCREEN
					searchView.visibility = View.VISIBLE
				}
			}
			it.addOnPagesUpdatedListener {
				binding.recipesView.noRecipesMessage.visibility = if (it.itemCount == 0)
					View.VISIBLE
				else
					View.GONE
			}
		}


		binding.recipesView.recipeListRecycler.adapter = ConcatAdapter(
			ccAdapter,
			recipeListAdapter.withLoadStateFooter(RecipeDescriptionLoadStateAdapter())
		)

		supportActionBar?.apply {
			displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
			setCustomView(R.layout.action_bar)
		}

		titleView = supportActionBar!!.customView!!.findViewById(R.id.action_bar_title)


		// SEARCH
		searchView = supportActionBar!!.customView!!.findViewById<SearchView>(R.id.action_bar_search).apply {
			setOnSearchClickListener {
				viewModel.search(query.toString())
			}
			setOnCloseListener {
				viewModel.search(null)
				false
			}
			setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextChange(query: String?): Boolean {
					viewModel.search(query)
					return true
				}
				override fun onQueryTextSubmit(query: String?): Boolean {
					clearFocus()
					return true
				}
			})
		}

		onBackPressedDispatcher.addCallback(this@MainActivity, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				searchView.apply {
					if (query != null) {
						if (query.isNotEmpty() && hasFocus()) {
							setQuery("", false)
						}
						else {
							setQuery(null, true)
							isIconified = true
						}
					}
				}
			}
		})

		lifecycleScope.launch {
			viewModel.searchQuery.collectLatest {
				if (it != null) {
					searchView.isIconified = false
					findViewById<ImageView>(R.id.search_close_btn).setImageResource(
						if (it.isEmpty()) R.drawable.ic_close
						else R.drawable.ic_backspace
					)
					titleView.visibility = View.GONE
					binding.recipesView.recipeListRecycler.layoutManager?.scrollToPosition(0)
				} else {
					searchView.isIconified = true
					titleView.visibility = View.VISIBLE
				}
			}
		}


		// CATEGORIES & CUISINES
		lifecycleScope.launch {
			viewModel.filteredCategories.combine(viewModel.filteredCuisines) { categories, cuisines ->
				listOf(categories, cuisines)
			}.collectLatest {
				ccAdapter.updateChipAdapters(it)
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
		searchView.apply {
			setQuery(query, true)
		}
	}

	fun openRecipeDetail(recipeId: Long) {
		val intent = Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", recipeId)
		}
		startActivity(intent)
	}

	override fun onStart() {
		super.onStart()
		if (restartPending) {
			val intent = Intent(this, MainActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			startActivity(intent)
			finish()
			Runtime.getRuntime().exit(0)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(
			if (fileMode == MODE_STANDALONE) R.menu.options_standalone else R.menu.options_synced,
			menu
		)
		menu.findItem(R.id.show_about)?.title = getString(R.string.about_app_name, getString(R.string.app_name))
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