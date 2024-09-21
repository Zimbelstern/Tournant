package eu.zimbelstern.tournant.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.room.Room
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.RecipeRoomDatabase
import eu.zimbelstern.tournant.data.RecipeWithIngredients
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
						it.recipeDao().insertRecipesWithIngredients(
							listOf(
								RecipeWithIngredients(
									Recipe(
										title = ContextCompat.getString(context, R.string.muffins),
										category = ContextCompat.getString(context, R.string.pastries),
										cuisine = ContextCompat.getString(context, R.string.american),
										source = ContextCompat.getString(context, R.string.bbc_goodfood),
										rating = 5f,
										preptime = 10,
										cooktime = 25,
										yieldValue = 20f,
										yieldUnit = ContextCompat.getString(context, R.string.yield_muffins),
										instructions = ContextCompat.getString(context, R.string.sample_instructions),
										notes = ContextCompat.getString(context, R.string.sample_modifications),
										image = ByteArrayOutputStream().also {
											BitmapFactory.decodeResource(context.resources, R.drawable.muffin)
												.compress(Bitmap.CompressFormat.JPEG, 75, it)
										}.toByteArray()
									),
									listOf(
										Triple(2f, null, R.string.eggs),
										Triple(125f, R.string.ml, R.string.vegetable_oil),
										Triple(250f, R.string.ml, R.string.milk),
										Triple(250f, R.string.g, R.string.sugar),
										Triple(400f, R.string.g, R.string.flour),
										Triple(3f, R.string.tsp, R.string.baking_powder),
										Triple(1f, R.string.tsp, R.string.salt),
										Triple(100f, R.string.g, R.string.chocolate_chips)
									).mapIndexed { i, item ->
										Ingredient(
											position = i,
											amount = item.first,
											amountRange = null,
											unit = item.second?.let { ContextCompat.getString(context, it) },
											item = ContextCompat.getString(context, item.third),
											group = null,
											optional = false
										)
									}.toMutableList()
								)
							) + listOf(
								listOf(R.string.croissants, R.string.pastries, R.string.french, R.drawable.croissants, null),
								listOf(R.string.pretzels, R.string.pastries, R.string.german, R.drawable.brezel, null),
								listOf(R.string.tiramisu, R.string.dessert, R.string.italian, R.drawable.tiramisu, 4f),
								listOf(R.string.panna_cotta, R.string.dessert, R.string.italian, R.drawable.panna_cotta, 4.5f)
							).map {
								RecipeWithIngredients(
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
										}.toByteArray()
									), mutableListOf()
								)
							}
						)
					}
				}
			}
}
