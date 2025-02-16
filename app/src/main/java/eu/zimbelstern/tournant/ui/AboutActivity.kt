package eu.zimbelstern.tournant.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.get
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.safeInsets

class AboutActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(android.R.id.content, AboutFragment())
				.commit()
		}
		supportActionBar?.apply {
			title = getString(R.string.about_app_name, getString(R.string.tournant))
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

	class AboutFragment : PreferenceFragmentCompat() {

		private var egg = 1

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
			setPreferencesFromResource(R.xml.about_preferences, rootKey)
			findPreference<PreferenceCategory>("about")?.title = getString(R.string.about_app_name, getString(R.string.tournant))
			findPreference<Preference>("version")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
				if (egg < 3) egg++ else Toast.makeText(context, getString(R.string.egg), Toast.LENGTH_LONG).show()
				true
			}
			for (key in listOf("", "license", "source", "issues", "translations", "recipes", "donate")) {
				findPreference<Preference>(key)?.onPreferenceClickListener = preferenceKeyAsUrl()
			}
			findPreference<PreferenceCategory>("libraries")?.let {
				for (i in 0 until it.preferenceCount) {
					it[i].apply {
						when (summary) {
							"Apache License, Version 2.0" -> setOnPreferenceClickListener {
								actionViewUri("http://www.apache.org/licenses/LICENSE-2.0")
								true
							}
							"Licensed by Google" -> setOnPreferenceClickListener {
								actionViewUri("https://github.com/bumptech/glide/blob/master/LICENSE")
								true
							}
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