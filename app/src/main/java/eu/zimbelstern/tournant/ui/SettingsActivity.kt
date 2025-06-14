package eu.zimbelstern.tournant.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_DECIMAL_SEPARATOR_COMMA
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE
import eu.zimbelstern.tournant.Constants.Companion.PREF_FILE_LAST_MODIFIED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MARKDOWN
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.Constants.Companion.PREF_SCREEN_ON
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.TournantApplication
import eu.zimbelstern.tournant.safeInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormatSymbols
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(android.R.id.content, SettingsFragment())
				.commit()
		}
		supportActionBar?.apply {
			title = getString(R.string.settings)
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		enableEdgeToEdge()
		ViewGroupCompat.installCompatInsetsDispatch(window.decorView.rootView)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
			Log.d(localClassName.split(".").last(), "setOnApplyWindowInsetsListener(content)")
			view.updatePadding(
				top = windowInsets.safeInsets().top,
				bottom = windowInsets.safeInsets().bottom
			)
			windowInsets
		}

		@Suppress("DEPRECATION")
		if (Build.VERSION.SDK_INT < 35) {
			window.navigationBarColor = ContextCompat.getColor(this, R.color.bar_color)
		}
	}

	class SettingsFragment : PreferenceFragmentCompat() {

		companion object {
			private const val TAG = "SettingsFragment"
		}

		override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
			super.onViewCreated(view, savedInstanceState)
			ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
				v.updatePadding(
					left = windowInsets.safeInsets().left,
					right = windowInsets.safeInsets().right,
				)
				WindowInsetsCompat.CONSUMED
			}
			view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.surface))
		}

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey)
			val sharedPrefs = requireActivity().getSharedPreferences(
				requireActivity().packageName + "_preferences",
				MODE_PRIVATE
			)

			findPreference<ListPreference>("mode")?.apply {
				val options = arrayOf(MODE_STANDALONE, MODE_SYNCED)
				entryValues = arrayOf("STANDALONE", "SYNC")
				setDefaultValue(entryValues[0])
				val mode = sharedPrefs.getInt(PREF_MODE, MODE_STANDALONE)
				value = entryValues[mode - 1].toString()
				summary = context.resources.getStringArray(R.array.mode_options_summary)[mode - 1]
				setOnPreferenceChangeListener { _, value ->
					val opt = options[entryValues.indexOf(value)]
					sharedPrefs
						.edit {
    						putInt(PREF_MODE, opt)
						}
					summary = context.resources.getStringArray(R.array.mode_options_summary)[opt - 1]
					findPreference<Preference>("file")?.isEnabled = opt == MODE_SYNCED
					if (opt == MODE_SYNCED && sharedPrefs.getString(PREF_FILE, "").isNullOrEmpty()) {
						(activity as SettingsActivity).chooseFile()
					}
					true
				}
			}

			findPreference<Preference>("file")?.apply {
				isEnabled = findPreference<ListPreference>("mode")?.value == "SYNC"
				summary = sharedPrefs.getString(PREF_FILE, "")
				setOnPreferenceClickListener {
					(activity as SettingsActivity).chooseFile()
					true
				}
			}

			sharedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
				if (key == PREF_FILE) {
					findPreference<Preference>("file")?.summary = sharedPrefs.getString(key, "")
				}
			}

			findPreference<SwitchPreference>("markdown")?.apply {
				isChecked = sharedPrefs.getBoolean(PREF_MARKDOWN, true)
				setOnPreferenceChangeListener { _, value ->
					sharedPrefs
						.edit {
							putBoolean(PREF_MARKDOWN, value as Boolean)
						}
					true
				}
			}

			findPreference<SwitchPreference>("keep_screen_on")?.apply {
				isChecked = sharedPrefs.getBoolean(PREF_SCREEN_ON, true)
				setOnPreferenceChangeListener { _, value ->
					sharedPrefs
						.edit {
							putBoolean(PREF_SCREEN_ON, value as Boolean)
						}
					true
				}
			}

			findPreference<ListPreference>("color_theme")?.apply {
				val options = arrayOf(MODE_NIGHT_FOLLOW_SYSTEM, MODE_NIGHT_NO, MODE_NIGHT_YES)
				entryValues = arrayOf("AUTO", "LIGHT", "DARK")
				setDefaultValue(entryValues[0])
				val mode = sharedPrefs.getInt(PREF_COLOR_THEME, MODE_NIGHT_FOLLOW_SYSTEM)
				value = entryValues[options.indexOf(mode)].toString()
				summary = entry
				setOnPreferenceChangeListener { _, value ->
					val opt = options[entryValues.indexOf(value)]
					sharedPrefs
						.edit {
							putInt(PREF_COLOR_THEME, opt)
						}
					AppCompatDelegate.setDefaultNightMode(opt)
					true
				}
			}

			findPreference<ListPreference>("language")?.apply {
				val availableLocales = getString(R.string.availableLanguages)
					.split(",")
					.map { Locale.forLanguageTag(it) }
					.sortedBy { it.displayName }

				val thisLocale = AppCompatDelegate.getApplicationLocales().get(0)
					?: LocaleListCompat.getDefault().get(0)

				val foundLocale = availableLocales.find { it.toLanguageTag() == thisLocale?.toLanguageTag() }
					?: availableLocales.find { it.language == thisLocale?.language }

				Log.d(TAG, "availableLocales: ${availableLocales.map { it.toLanguageTag() }}")
				Log.d(TAG, "thisLocale: ${thisLocale?.toLanguageTag()}")
				Log.d(TAG, "foundLocale: ${foundLocale?.toLanguageTag()}")

				entries = availableLocales.map { it.displayName.replaceFirstChar { char -> char.titlecase() } }.toTypedArray()
				entryValues = availableLocales.map { it.toLanguageTag() }.toTypedArray()

				foundLocale?.let {
					value = it.toLanguageTag()
					setDefaultValue(it.toLanguageTag())
					summary = it.displayName.replaceFirstChar { char -> char.titlecase() }
				}

				setOnPreferenceChangeListener { _, value ->
					AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value as String))
					true
				}
			}

			findPreference<SwitchPreference>("decimal_separator")?.apply {
				isChecked = sharedPrefs.getBoolean(PREF_DECIMAL_SEPARATOR_COMMA, DecimalFormatSymbols.getInstance().decimalSeparator == ',')
				setOnPreferenceChangeListener { _, value ->
					sharedPrefs
						.edit {
							putBoolean(PREF_DECIMAL_SEPARATOR_COMMA, value as Boolean)
						}
					true
				}
			}

			findPreference<Preference>("delete_all_recipes")?.apply {
				isEnabled = sharedPrefs.getInt(PREF_MODE, MODE_STANDALONE) == MODE_STANDALONE
				setOnPreferenceClickListener {
					MaterialAlertDialogBuilder(context)
						.setTitle(R.string.delete_all_recipes)
						.setMessage(R.string.delete_all_recipes_sure)
						.setPositiveButton(R.string.ok) { _, _ ->
							(activity?.application as? TournantApplication)?.run {
								MainScope().launch {
									withContext(Dispatchers.IO) {
										val imageDir = File(context.applicationContext.filesDir, "images")
										if (imageDir.exists()) {
											imageDir.listFiles()?.forEach {
												it.delete()
											}
											imageDir.delete()
										}
										database.recipeDao().deleteAllRecipes()
									}
									withContext(Dispatchers.Main) {
										Toast.makeText(
											applicationContext,
											R.string.done,
											Toast.LENGTH_SHORT
										).show()
									}
								}
							}
						}
						.setNegativeButton(R.string.cancel, null)
						.show()
					true
				}
			}
		}

	}

	private fun chooseFile() {
		getRecipeFileUri.launch(arrayOf("application/octet-stream", "application/xml", "text/html", "text/xml"))
	}
	private val getRecipeFileUri = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			for (permission in contentResolver.persistedUriPermissions) {
				// Permissions from files before
				contentResolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
			try {
				val inputStream = contentResolver.openInputStream(it)
				if (inputStream == null) {
					Toast.makeText(this, getString(R.string.inputstream_null), Toast.LENGTH_LONG).show()
				} else {
					getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
						.edit {
							putString(PREF_FILE, it.toString())
								.putLong(PREF_FILE_LAST_MODIFIED, -1)
						}
				}
			} catch (e: Exception) {
				Toast.makeText(this, getString(R.string.unknown_file_error, e.message), Toast.LENGTH_LONG).show()
			}
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == android.R.id.home) {
			finish()
			true
		} else
			super.onOptionsItemSelected(item)
	}

}