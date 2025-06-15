package eu.zimbelstern.tournant.data.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import eu.zimbelstern.tournant.BuildConfig
import eu.zimbelstern.tournant.Constants.Companion.MODE_STANDALONE
import eu.zimbelstern.tournant.Constants.Companion.MODE_SYNCED
import eu.zimbelstern.tournant.Constants.Companion.PREF_MODE
import eu.zimbelstern.tournant.getAppOrSystemLocale
import kotlin.reflect.full.declaredFunctions

@Database(
	entities = [RecipeEntity::class, IngredientEntity::class, KeywordEntity::class, PreparationEntity::class],
	exportSchema = true,
	version = 6,
	autoMigrations = [AutoMigration(1, 2), AutoMigration(2, 3), AutoMigration(4, 5)]
)
abstract class RecipeRoomDatabase : RoomDatabase() {
	abstract fun recipeDao(): RecipeDao

	companion object {
		@Volatile
		private var INSTANCE: RecipeRoomDatabase? = null
		fun getDatabase(context: Context): RecipeRoomDatabase {
			return INSTANCE ?: synchronized(this) {
				@Suppress("KotlinConstantConditions")
				val instance = if (BuildConfig.FLAVOR == "demo") {
					Class.forName("eu.zimbelstern.tournant.demo.DemoDatabase").kotlin.run {
						declaredFunctions.find { it.name == "create" }!!.call(objectInstance, context)
					} as RecipeRoomDatabase
				} else Room.databaseBuilder(
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
					.addMigrations(MIGRATION_3_4, MIGRATION_5_6)
					.build()
				INSTANCE = instance
				return instance
			}
		}

		val MIGRATION_3_4 = object : Migration(3, 4) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE Ingredient RENAME TO IngredientOld")
				db.execSQL("CREATE TABLE IF NOT EXISTS Ingredient (`recipeId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `amount` REAL, `amountRange` REAL, `unit` TEXT, `item` TEXT, `refId` INTEGER, `group` TEXT, `optional` INTEGER NOT NULL, PRIMARY KEY (recipeId, position), FOREIGN KEY(`recipeId`) REFERENCES `Recipe`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
				db.execSQL("INSERT INTO Ingredient SELECT recipeId, position, amount, amountRange, unit, item, refId, `group`, optional FROM IngredientOld")
				db.execSQL("DROP TABLE IngredientOld")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_Ingredient_recipeId` ON Ingredient (recipeId)")
				db.execSQL("ALTER TABLE Preparation RENAME TO PreparationOld")
				db.execSQL("CREATE TABLE IF NOT EXISTS Preparation (`recipeId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY (recipeId, date), FOREIGN KEY(`recipeId`) REFERENCES `Recipe`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
				db.execSQL("INSERT INTO Preparation SELECT recipeId, date, COUNT(*) FROM PreparationOld GROUP BY recipeId, date")
				db.execSQL("DROP TABLE PreparationOld")
				db.execSQL("CREATE INDEX IF NOT EXISTS `index_Preparation_recipeId` ON Preparation (recipeId)")
			}
		}

		val MIGRATION_5_6 = object : Migration(5, 6) {
			override fun migrate(db: SupportSQLiteDatabase) {
				val languageTag = getAppOrSystemLocale().toLanguageTag()
				db.execSQL("ALTER TABLE Recipe ADD COLUMN language TEXT NOT NULL DEFAULT `$languageTag`")
			}
		}
	}

}
