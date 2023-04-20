package eu.zimbelstern.tournant

import android.app.Application
import eu.zimbelstern.tournant.data.RecipeRoomDatabase
import kotlinx.coroutines.coroutineScope

class TournantApplication : Application() {
	val database: RecipeRoomDatabase by lazy {
		RecipeRoomDatabase.getDatabase(this)
	}
}