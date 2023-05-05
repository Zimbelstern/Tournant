package eu.zimbelstern.tournant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE

@Database(entities = [Recipe::class, Ingredient::class], version = 1, exportSchema = false)
abstract class RecipeRoomDatabase : RoomDatabase() {
	abstract fun recipeDao(): RecipeDao

	companion object {
		@Volatile
		private var INSTANCE: RecipeRoomDatabase? = null
		fun getDatabase(context: Context): RecipeRoomDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					RecipeRoomDatabase::class.java,
					if (context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
						.getInt(PREF_MODE, MODE_STANDALONE) == MODE_SYNCED)
							"synced_recipe_database"
					else "recipe_database"
				)
					// Verbose SQL logging
/*					.setQueryCallback(
					 	object : QueryCallback {
						override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
							Log.d("SQL", "QUERY $sqlQuery ARGS $bindArgs")
						}
					}, Executors.newSingleThreadExecutor()
					)
*/
					.fallbackToDestructiveMigration()
					.build()
				INSTANCE = instance
				return instance
			}
		}
	}
}