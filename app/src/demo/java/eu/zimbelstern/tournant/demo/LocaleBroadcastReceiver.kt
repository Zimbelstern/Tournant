package eu.zimbelstern.tournant.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LocaleBroadcastReceiver : BroadcastReceiver() {
	companion object {
		const val TAG = "LocaleBroadcastReceiver"
	}

	override fun onReceive(context: Context?, intent: Intent?) {
		val locale = intent?.getStringExtra("locale")
		Log.i(TAG, "Changing locale to $locale")
		AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
	}
}
