package eu.zimbelstern.tournant.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.room.Room
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.room.RecipeRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Suppress("unused")
object DemoDatabase {
	fun create(context: Context): RecipeRoomDatabase =
		Room.inMemoryDatabaseBuilder(
			context.applicationContext,
			RecipeRoomDatabase::class.java
		)
			.build().also {
				MainScope().launch {
					withContext(Dispatchers.IO) {
						it.recipeDao().insertRecipesWithIngredientsAndPreparations(
							listOf(
								Recipe(
									title = ContextCompat.getString(context, R.string.muffins),
									category = ContextCompat.getString(context, R.string.pastries),
									cuisine = ContextCompat.getString(context, R.string.american),
									source = ContextCompat.getString(context, R.string.bbc_goodfood),
									rating = 5f,
									preptime = 10,
									cooktime = 25,
									yieldValue = 20.0,
									yieldUnit = ContextCompat.getString(context, R.string.yield_muffins),
									instructions = ContextCompat.getString(context, R.string.sample_instructions),
									notes = ContextCompat.getString(context, R.string.sample_modifications),
									image = ByteArrayOutputStream().also {
										BitmapFactory.decodeResource(context.resources, R.drawable.muffin)
											.compress(Bitmap.CompressFormat.JPEG, 75, it)
									}.toByteArray(),
									ingredients =
										listOf(
											Triple(2, null, R.string.eggs),
											Triple(125, R.string.ml, R.string.vegetable_oil),
											Triple(250, R.string.ml, R.string.milk),
											Triple(250, R.string.g, R.string.sugar),
											Triple(400, R.string.g, R.string.flour),
											Triple(3, R.string.tsp, R.string.baking_powder),
											Triple(1, R.string.tsp, R.string.salt),
											Triple(100, R.string.g, R.string.chocolate_chips)
										).mapIndexed { i, item ->
											Ingredient(
												amount = item.first.toDouble(),
												amountRange = null,
												unit = item.second?.let { ContextCompat.getString(context, it) },
												item = ContextCompat.getString(context, item.third),
												refId = null,
												group = null,
												optional = false
											)
										}.toMutableList()
								).toRecipeWithIngredientsAndPreparations()
							) + listOf(
								listOf(
									R.string.croissants,
									R.string.pastries,
									R.string.french,
									R.drawable.croissants,
									null
								),
								listOf(R.string.pretzels, R.string.pastries, R.string.german, R.drawable.brezel, null),
								listOf(R.string.tiramisu, R.string.dessert, R.string.italian, R.drawable.tiramisu, 4f),
								listOf(
									R.string.panna_cotta,
									R.string.dessert,
									R.string.italian,
									R.drawable.panna_cotta,
									4.5f
								)
							).map {
								Recipe(
									title = ContextCompat.getString(context, it[0] as Int),
									description = if (it[0] == R.string.pretzels)
										ContextCompat.getString(context, R.string.pretzels_description)
									else
										null,
									category = ContextCompat.getString(context, it[1] as Int),
									cuisine = ContextCompat.getString(context, it[2] as Int),
									rating = it[4] as Float?,
									image = ByteArrayOutputStream().apply {
										BitmapFactory.decodeResource(context.resources, it[3] as Int)
											.compress(Bitmap.CompressFormat.JPEG, 75, this)
									}.toByteArray(), ingredients = mutableListOf()
								).toRecipeWithIngredientsAndPreparations()
							}
						)
					}
				}
			}
}
