package eu.zimbelstern.tournant

import android.app.Application
import android.content.Context
import eu.zimbelstern.tournant.data.RecipeRoomDatabase
import java.text.DecimalFormatSymbols

class TournantApplication : Application() {

	val database: RecipeRoomDatabase by lazy {
		RecipeRoomDatabase.getDatabase(this)
	}

	fun getDecimalSeparator() =
		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
				.getBoolean(Constants.PREF_DECIMAL_SEPARATOR_COMMA, DecimalFormatSymbols.getInstance().decimalSeparator == ','))
			','
		else
			'.'

}