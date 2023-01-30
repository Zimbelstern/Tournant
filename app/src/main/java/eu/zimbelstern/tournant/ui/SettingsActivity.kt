package eu.zimbelstern.tournant.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import eu.zimbelstern.tournant.Constants.Companion.PREF_COLOR_THEME
import eu.zimbelstern.tournant.Constants.Companion.PREF_SCREEN_ON
import eu.zimbelstern.tournant.R

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
	}

	class SettingsFragment : PreferenceFragmentCompat() {

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey)

			findPreference<ListPreference>("color_theme")?.apply {
				val options = arrayOf(
					AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
					AppCompatDelegate.MODE_NIGHT_NO,
					AppCompatDelegate.MODE_NIGHT_YES
				)
				entryValues = arrayOf("AUTO", "LIGHT", "DARK")
				summary = entry
				setOnPreferenceChangeListener { _, value ->
					val opt = options[entryValues.indexOf(value)]
					requireActivity().getSharedPreferences(requireActivity().packageName + "_preferences", Context.MODE_PRIVATE)
						.edit()
						.putInt(PREF_COLOR_THEME, opt)
						.apply()
					AppCompatDelegate.setDefaultNightMode(opt)
					true
				}
			}

			findPreference<CheckBoxPreference>("keep_screen_on")?.setOnPreferenceChangeListener { _, value ->
				requireActivity().getSharedPreferences(requireActivity().packageName + "_preferences", Context.MODE_PRIVATE)
					.edit()
					.putBoolean(PREF_SCREEN_ON, value as Boolean)
					.apply()
				true
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