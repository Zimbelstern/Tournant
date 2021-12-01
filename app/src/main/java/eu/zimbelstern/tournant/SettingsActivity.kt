package eu.zimbelstern.tournant

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.settings, SettingsFragment())
				.commit()
		}
		supportActionBar?.apply {
			title = getString(R.string.settings)
			setDisplayHomeAsUpEnabled(true)
		}
	}

	class SettingsFragment : PreferenceFragmentCompat() {

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey)
			findPreference<CheckBoxPreference>("keep_screen_on")?.setOnPreferenceChangeListener { _, value ->
				requireActivity().getSharedPreferences(requireActivity().packageName + "_preferences", Context.MODE_PRIVATE)
					.edit()
					.putBoolean("SCREEN_ON", value as Boolean)
					.apply()
				true
			}
		}

	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressed()
		return true
	}

}