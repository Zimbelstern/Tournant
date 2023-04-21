package eu.zimbelstern.tournant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
					"recipe_database"
				)
					// Uncomment for verbose SQL logging:
/*					.setQueryCallback(object : RoomDatabase.QueryCallback {
						override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
							Log.d("SQL", "QUERY $sqlQuery ARGS $bindArgs")
						}
					}, Executors.newSingleThreadExecutor())*/
					.fallbackToDestructiveMigration()
					.allowMainThreadQueries()
					.build()
				// For debugging purposes TODO: Remove the following line and the one two above
				instance.clearAllTables()
				INSTANCE = instance
				return instance
			}
		}
	}
}