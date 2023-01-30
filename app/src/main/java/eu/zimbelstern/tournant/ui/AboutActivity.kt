package eu.zimbelstern.tournant.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.get
import eu.zimbelstern.tournant.R

class AboutActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_about)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.about, AboutFragment())
				.commit()
		}
		supportActionBar?.apply {
			title = getString(R.string.about_app_name, getString(R.string.app_name))
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}
	}

	class AboutFragment : PreferenceFragmentCompat() {

		private var egg = 1

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.about_preferences, rootKey)
			findPreference<PreferenceCategory>("about")?.title = getString(R.string.about_app_name, getString(R.string.app_name))
			findPreference<Preference>("version")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
				if (egg < 3) egg++ else Toast.makeText(context, getString(R.string.egg), Toast.LENGTH_LONG).show()
				true
			}
			for (key in listOf("", "license", "source", "issues", "donate")) {
				findPreference<Preference>(key)?.onPreferenceClickListener = preferenceKeyAsUrl()
			}
			findPreference<PreferenceCategory>("libraries")?.let {
				for (i in 0 until it.preferenceCount) {
					it[i].apply {
						if (summary == "Apache License, Version 2.0")
							setOnPreferenceClickListener {
								actionViewUri("http://www.apache.org/licenses/LICENSE-2.0")
								true
							}
					}
				}
			}
		}

		private fun preferenceKeyAsUrl() = Preference.OnPreferenceClickListener {
			actionViewUri("https://tournant.zimbelstern.eu/${it.key}")
			true
		}

		private fun actionViewUri(uriString: String) {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
			try {
				startActivity(intent)
			} catch (_: ActivityNotFoundException) {
				Toast.makeText(context, uriString, Toast.LENGTH_LONG).show()
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