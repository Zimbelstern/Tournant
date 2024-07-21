package eu.zimbelstern.tournant.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.text.toSpannable
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.zimbelstern.tournant.BuildConfig
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_AUTO_BACKUP
import eu.zimbelstern.tournant.Constants.Companion.PREF_BACKUP_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_VERSION
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.databinding.ActivityMainBinding
import eu.zimbelstern.tournant.databinding.SortOptionsBinding
import eu.zimbelstern.tournant.pagination.RecipeDescriptionLoadStateAdapter
import eu.zimbelstern.tournant.ui.adapter.CategoriesCuisinesAdapter
import eu.zimbelstern.tournant.ui.adapter.RecipeListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity(), RecipeListAdapter.RecipeListInterface {

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

	private lateinit var recipeListAdapter: RecipeListAdapter
	private var searchMenuItem: MenuItem? = null
	private var searchView: SearchView? = null
	private var mode = 0

	private var recipeOpen = false
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
	private var scrollTopPending = false

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
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

		findViewById<View>(android.R.id.content).apply {
			viewTreeObserver.addOnPreDrawListener(
				object : ViewTreeObserver.OnPreDrawListener {
					override fun onPreDraw(): Boolean {
						return if (viewModel.countAllRecipes.value > -1) {
							viewTreeObserver.removeOnPreDrawListener(this)
							true
						} else false
					}
				}
			)
		}

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

		binding.welcomeView.buttonNewRecipe.setOnClickListener {
			startActivity(Intent(this, RecipeEditingActivity::class.java).apply {
				putExtra("RECIPE_ID", 0)
			})
		}

		binding.welcomeView.recipesWebsite.setOnClickListener {
			val href = "https://tournant.zimbelstern.eu/recipes"
			try {
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href)))
			} catch (_: ActivityNotFoundException) {
				Toast.makeText(this, href, Toast.LENGTH_LONG).show()
			}
		}

		val ccAdapter = CategoriesCuisinesAdapter(this)
		recipeListAdapter = RecipeListAdapter(this).also {
			it.addOnPagesUpdatedListener {
				binding.recipesView.noRecipesMessage.visibility =
					if (it.itemCount == 0)
						View.VISIBLE
					else
						View.GONE
				if (scrollTopPending) {
					binding.recipesView.recipeListRecycler.scrollToPosition(0)
					scrollTopPending = false
				}
			}
		}


		binding.recipesView.recipeListRecycler.adapter = ConcatAdapter(
			ccAdapter,
			recipeListAdapter.withLoadStateFooter(RecipeDescriptionLoadStateAdapter())
		)


		// Action bar title when: synced -> filename; recipes not empty -> All recipes; else -> Tournant
		lifecycleScope.launch {
			viewModel.syncedFileName.combine(viewModel.countAllRecipes) { filename, count ->
				Pair(filename, count)
			}.collectLatest {
				supportActionBar?.title =
					it.first?.substringBeforeLast(".")
						?: getString(if (it.second > 0) R.string.all_recipes else R.string.app_name)
			}
		}


		// RECIPE COUNT
		supportActionBar?.setDisplayShowTitleEnabled(true)
		lifecycleScope.launch {
			viewModel.waitingForRecipes.combine(viewModel.countAllRecipes) { waiting, count ->
				Pair(waiting, count)
			}.collectLatest {
				binding.root.displayedChild = when {
					it.first -> {
						Log.d(TAG, "Waiting for recipes")
						LOADING_SCREEN
					}
					it.second == 0 -> {
						Log.d(TAG, "Recipe count: 0")
						WELCOME_SCREEN
					}
					else -> {
						Log.d(TAG, "Recipe count: ${it.second}")
						RECIPES_SCREEN
					}
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
			viewModel.recipeDescriptions.collectLatest {
				Log.d(TAG, "Recipes updated")
				recipeListAdapter.submitData(it)
			}
		}

		lifecycleScope.launch {
			combine(viewModel.allCategories, viewModel.allCuisines) { categories, cuisines ->
				categories + cuisines
			}.collectLatest {
				recipeListAdapter.updateColors(it)
			}
		}


		lifecycleScope.launch {
			viewModel.orderedBy.collectLatest {
				recipeListAdapter.updateSortedBy(it)
			}
		}

		if (intent.action == Intent.ACTION_VIEW) {
			if (mode == MODE_STANDALONE)
				viewModel.parseAndInsertRecipes(intent.data as Uri)
			else
				Toast.makeText(this, R.string.importing_only_in_standalone_mode, Toast.LENGTH_LONG).show()
			intent.action = ""
		}

		lifecycleScope.launch { // BACKUP RECIPES
			viewModel.idsRecipesFiltered.collectLatest {
				if (sharedPrefs.getBoolean(PREF_AUTO_BACKUP, false) && mode == MODE_STANDALONE && !sharedPrefs.getString(PREF_BACKUP_FILE, null).isNullOrEmpty()){
					exportRecipes(it, "xml", false)
					val uri = sharedPrefs.getString(PREF_BACKUP_FILE, "")?.toUri()
					if (uri != null) {
						viewModel.copyRecipesFromExportDir("export", "xml", uri)
					}
				}
			}
		}

	}

	private fun importRecipesFromFile() {
		activityResultLauncher.launch(
			arrayOf("application/octet-stream", "application/json", "application/xml", "application/zip", "text/xml")
		)
	}
	private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			viewModel.parseAndInsertRecipes(it)
		}
	}

	// RecipeListInterface

	override fun getFilteredRecipesIds(): Set<Long> {
		return viewModel.idsRecipesFiltered.value
	}

	override fun searchForSomething(query: CharSequence?) {
		if (!query.isNullOrEmpty()) {
			searchMenuItem?.expandActionView()
			searchView?.setQuery(query, true)
		}
	}

	override fun isReadOnly(): Boolean {
		return mode == MODE_SYNCED
	}

	override fun openRecipeDetail(recipeId: Long) {
		recipeOpen = true
		val intent = Intent(this, RecipeActivity::class.java).apply {
			putExtra("RECIPE_ID", recipeId)
		}
		startActivity(intent)
	}

	override fun startActionMode(adapter: RecipeListAdapter) {
		startSupportActionMode(adapter)
	}

	override fun exportRecipes(recipeIds: Set<Long>, format: String, chooseFile: Boolean) {
		fun export() = lifecycleScope.launch {
			Log.d(TAG, "Exporting recipes $recipeIds")
			withContext(Dispatchers.IO) {
				val filename = if (recipeIds.size == 1) viewModel.getRecipeTitle(recipeIds.first()) else getString(R.string.recipes)
				(application as TournantApplication).writeRecipesToExportDir(recipeIds, "export", format)
				if (chooseFile){
					when (format) {
						"json" -> exportJsonActivityResultLauncher.launch("$filename.json")
						"zip" -> exportZipActivityResultLauncher.launch("$filename.zip")
						"xml" -> exportXmlActivityResultLauncher.launch("$filename.xml")
						else -> throw Error("Wrong export format")
					}
				}
			}
			recipeListAdapter.finishActionMode()
		}
		if (format == "xml")
			(application as TournantApplication).withGourmandIssueCheck(this, recipeIds) { export() }
		else
			export()
	}

	private val exportJsonActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
		if (it != null) { viewModel.copyRecipesFromExportDir("export", "json", it) }
	}
	private val exportZipActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
		if (it != null) { viewModel.copyRecipesFromExportDir("export", "zip", it) }
	}
	private val exportXmlActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/xml")) {
		if (it != null) { viewModel.copyRecipesFromExportDir("export", "xml", it) }
	}

	override fun shareRecipes(recipeIds: Set<Long>, format: String) {
		fun share() = lifecycleScope.launch {
			Log.d(TAG, "Sharing recipes $recipeIds")
			withContext(Dispatchers.IO) {
				val filename = if (recipeIds.size == 1) viewModel.getRecipeTitle(recipeIds.first()) else getString(R.string.recipes)
				(application as TournantApplication).writeRecipesToExportDir(recipeIds, filename, format)
				val uri = FileProvider.getUriForFile(
					application,
					BuildConfig.APPLICATION_ID + ".fileprovider",
					File(File(filesDir, "export"), "$filename.$format")
				)
				ShareCompat.IntentBuilder(this@MainActivity)
					.setStream(uri)
					.setType("application/$format")
					.startChooser()
			}
			recipeListAdapter.finishActionMode()
		}
		if (format == "xml")
			(application as TournantApplication).withGourmandIssueCheck(this, recipeIds) {
				share()
			}
		else
			share()
	}

	override fun showDeleteDialog(recipeIds: Set<Long>) {
		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				val depRecipes = viewModel.getDepRecipes(recipeIds)
				if (depRecipes.isEmpty()) {
					val message = if (recipeIds.size == 1) {
						getString(R.string.delete_selected_sure_named, viewModel.getRecipeTitle(recipeIds.first()))
							.toSpannable().apply {
								val start = getString(R.string.delete_selected_sure_named).indexOf('%')
								val end = start + viewModel.getRecipeTitle(recipeIds.first()).length
								setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
							}
					}
					else {
						resources.getQuantityString(R.plurals.delete_selected_sure, recipeIds.size, recipeIds.size)
							.toSpannable().apply {
								val start = getString(R.string.delete_selected_sure_named).indexOf('%')
								val end = start + recipeIds.size.toString().length
								setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
							}
					}
					withContext(Dispatchers.Main) {
						MaterialAlertDialogBuilder(this@MainActivity)
							.setTitle(R.string.delete_selected)
							.setMessage(message)
							.setPositiveButton(R.string.ok) { _, _ ->
								Log.d(TAG, "Deleting recipes $recipeIds")
								viewModel.deleteRecipes(recipeIds)
								recipeListAdapter.finishActionMode()
							}
							.setNegativeButton(R.string.cancel, null)
							.show()
					}
				} else {
					val message = getString(R.string.dependent_recipes_found, depRecipes.joinToString(", ") { viewModel.getRecipeTitle(it) })
						.toSpannable().apply {
							val start = getString(R.string.dependent_recipes_found).length
							val end = length
							setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
						}
					withContext(Dispatchers.Main) {
						MaterialAlertDialogBuilder(this@MainActivity)
							.setTitle(R.string.dependent_recipes)
							.setMessage(message)
							.setPositiveButton(R.string.add_to_selection) { _, _ ->
								recipeListAdapter.select(depRecipes)
							}
							.setNegativeButton(R.string.cancel, null)
							.show()
					}
				}
			}
		}
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
			if (recipeOpen) {
				recipeListAdapter.recipeClosed()
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.options, menu)

		if (mode == MODE_SYNCED) {
			menu.removeItem(R.id.new_recipe)
			menu.removeItem(R.id.import_recipes)
			menu.findItem(R.id.refresh)?.isVisible = true
			invalidateOptionsMenu()
		}

		menu.findItem(R.id.export_all).subMenu?.clearHeader()
		menu.findItem(R.id.share_all).subMenu?.clearHeader()
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
			viewModel.countAllRecipes.collectLatest { count ->
				var revalidate = false
				for (item in mapOf(
					searchMenuItem to (count > 1),
					menu.findItem(R.id.recipe_count) to (count > 0),
					menu.findItem(R.id.select_all) to (count > 0),
				)) {
					if (item.key?.isVisible != item.value) {
						item.key?.isVisible = item.value
						revalidate = true
					}
				}
				if (revalidate)
					invalidateOptionsMenu()
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

		lifecycleScope.launch {
			viewModel.idsRecipesFiltered.collectLatest {
				menu.findItem(R.id.recipe_count).actionView?.findViewById<TextView>(R.id.number)?.text = it.size.toString()
			}
		}

		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.new_recipe -> {
				startActivity(Intent(this, RecipeEditingActivity::class.java).apply {
					putExtra("RECIPE_ID", 0)
				})
				true
			}
			R.id.import_recipes -> {
				importRecipesFromFile()
				true
			}
			R.id.refresh -> {
				viewModel.syncWithFile(true)
				true
			}
			R.id.sorting -> {
				showSortDialog()
				true
			}
			R.id.export_all_json -> {
				exportRecipes(getFilteredRecipesIds(), "json")
				true
			}
			R.id.export_all_zip -> {
				exportRecipes(getFilteredRecipesIds(), "zip")
				true
			}
			R.id.export_all_gourmand -> {
				exportRecipes(getFilteredRecipesIds(), "xml")
				true
			}
			R.id.share_all_json -> {
				shareRecipes(getFilteredRecipesIds(), "json")
				true
			}
			R.id.share_all_zip -> {
				shareRecipes(getFilteredRecipesIds(), "zip")
				true
			}
			R.id.share_all_gourmand -> {
				shareRecipes(getFilteredRecipesIds(), "xml")
				true
			}
			R.id.select_all -> {
				startSupportActionMode(recipeListAdapter)
				recipeListAdapter.select(getFilteredRecipesIds())
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

	private fun showSortDialog() {
		val sortOptionsView = SortOptionsBinding.inflate(layoutInflater)

		(sortOptionsView.sortBy.children.elementAt(viewModel.orderedBy.value.floorDiv(2)) as RadioButton).isChecked = true
		sortOptionsView.asc.isChecked = viewModel.orderedBy.value % 2 == 0
		sortOptionsView.desc.isChecked = !sortOptionsView.asc.isChecked

		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.sort_by)
			.setView(sortOptionsView.root)
			.setPositiveButton(R.string.ok) { dialog, _ ->
				var orderBy = sortOptionsView.sortBy.children.indexOfFirst {
					(it as RadioButton).isChecked
				} * 2
				if (sortOptionsView.desc.isChecked)
					orderBy++
				viewModel.changeOrder(orderBy)
				scrollTopPending = true
				dialog.dismiss()
			}
			.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
			.show()
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
							viewModel.countAllRecipes.collectLatest { count ->
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