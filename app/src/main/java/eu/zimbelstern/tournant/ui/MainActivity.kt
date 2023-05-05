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
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayoutManager
import eu.zimbelstern.tournant.ChipGroupAdapter
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.RecipeListAdapter
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.data.RecipeDescription
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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
	private var searchView: SearchView? = null
	private var titleView: TextView? = null
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

		supportActionBar?.also {
			it.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
			it.setCustomView(R.layout.action_bar)
			titleView = it.customView?.findViewById(R.id.action_bar_title)
			searchView = it.customView?.findViewById<SearchView>(R.id.action_bar_search)?.apply {
				if (!isIconified) {
					titleView?.visibility = View.GONE
					binding.recipesView.activityMainCcSearch.visibility = View.VISIBLE
				}
				setOnSearchClickListener {
					titleView?.visibility = View.GONE
					binding.recipesView.activityMainCcSearch.visibility = View.VISIBLE
					onBackPressedDispatcher.addCallback(this@MainActivity, closeSearchOnBackPressedCallback)
				}
				setOnCloseListener {
					titleView?.visibility = View.VISIBLE
					binding.recipesView.activityMainCcSearch.visibility = View.GONE
					false
				}
			}
		}

		lifecycleScope.launch {
			viewModel.allRecipes().collectLatest { allRecipes ->
				Log.d(TAG, "Recipes updated")
				searchView?.apply {
					setQuery(null, false)
					isIconified = true
					visibility = View.GONE
				}
				titleView?.apply {
					text = getString(R.string.app_name)
					titleView?.visibility = View.VISIBLE
				}
				if (allRecipes.isNotEmpty()) {
					showRecipes(allRecipes, savedInstanceState?.getCharSequence("SEARCH_QUERY"))
					savedInstanceState?.putCharSequence("SEARCH_QUERY", null)
					binding.root.displayedChild = RECIPES_SCREEN
				} else {
					binding.root.displayedChild = WELCOME_SCREEN
				}
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

	private fun showRecipes(recipes: List<RecipeDescription>, filter: CharSequence?) {

		val recipeListAdapter = RecipeListAdapter(this, recipes)
		binding.recipesView.activityMainRecipeRecycler.adapter = recipeListAdapter

		val categoryChipGroupAdapter = ChipGroupAdapter(this, recipes.mapNotNull { it.category }.distinct().sorted())
		binding.recipesView.activityMainCcSearchCategoryRecycler.adapter = categoryChipGroupAdapter
		binding.recipesView.activityMainCcSearchCategoryRecycler.layoutManager = FlexboxLayoutManager(this)

		val cuisineChipGroupAdapter = ChipGroupAdapter(this, recipes.mapNotNull { it.cuisine }.distinct().sorted())
		binding.recipesView.activityMainCcSearchCuisineRecycler.adapter = cuisineChipGroupAdapter
		binding.recipesView.activityMainCcSearchCuisineRecycler.layoutManager = FlexboxLayoutManager(this)

		searchView?.apply {
			visibility = View.VISIBLE
			isIconified = filter.isNullOrEmpty()
			setOnQueryTextListener(object : OnQueryTextListener {
				override fun onQueryTextChange(query: String?): Boolean {
					recipeListAdapter.filterRecipes(query)
					binding.recipesView.activityMainRecipeNoRecipes.visibility = if (recipeListAdapter.itemCount == 0) View.VISIBLE else View.GONE
					categoryChipGroupAdapter.filterChips(query)
					binding.recipesView.activityMainCcSearchCategory.visibility = if (categoryChipGroupAdapter.itemCount != 0) View.VISIBLE else View.GONE
					cuisineChipGroupAdapter.filterChips(query)
					binding.recipesView.activityMainCcSearchCuisine.visibility = if (cuisineChipGroupAdapter.itemCount != 0) View.VISIBLE else View.GONE
					binding.recipesView.activityMainCcSearch.visibility = minOf(binding.recipesView.activityMainCcSearchCategory.visibility, binding.recipesView.activityMainCcSearchCuisine.visibility)
					binding.recipesView.activityMainRecipes.fullScroll(View.FOCUS_UP)
					searchView?.requestFocus()
					if (query.isNullOrEmpty())
						findViewById<ImageView>(R.id.search_close_btn).setImageResource(R.drawable.ic_close)
					else
						findViewById<ImageView>(R.id.search_close_btn).setImageResource(R.drawable.ic_backspace)
					return true
				}
				override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
			})
			setQuery(filter, true)
			clearFocus()
		}
	}

	fun searchForSomething(query: CharSequence?) {
		supportActionBar?.customView?.findViewById<SearchView>(R.id.action_bar_search)?.apply {
			setQuery(query, true)
			isIconified = false
			clearFocus()
		}
	}

	fun openRecipeDetail(recipeId: Long) {
		val intent = Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", recipeId)
		}
		startActivity(intent)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putCharSequence("SEARCH_QUERY", searchView?.query)
		super.onSaveInstanceState(outState)
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

	private val closeSearchOnBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			searchView?.setQuery("", false)
			searchView?.isIconified = true
			remove()
		}
	}

}