package eu.zimbelstern.tournant

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.zimbelstern.tournant.data.RecipeList
import eu.zimbelstern.tournant.data.RecipeRoomDatabase
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import eu.zimbelstern.tournant.utils.RecipeJsonAdapter
import okio.use
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DecimalFormatSymbols
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class TournantApplication : Application() {

	val database: RecipeRoomDatabase by lazy {
		RecipeRoomDatabase.getDatabase(this)
	}

	fun writeRecipesToExportDir(recipeIds: Set<Long>, filename: String, format: String) {

		val recipeDao = database.recipeDao()

		val recipes = recipeDao.getRecipesById(recipeIds)
		val refs = recipeDao.getReferencedRecipes(recipeIds)

		if (format != "zip") {
			// Read externally saved images, compress and add to recipe objects
			(recipes + refs).forEach {
				val imageFile = File(File(filesDir, "images"), "${it.recipe.id}.jpg")
				if (imageFile.exists()) {
					imageFile.inputStream().use { inputStream ->
						val byteArrayOutputStream = ByteArrayOutputStream()
						val image = BitmapFactory.decodeStream(inputStream)
						Bitmap.createScaledBitmap(image, 256, (image.height * 256f / image.width).roundToInt(), true)
							.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
						it.recipe.image = byteArrayOutputStream.toByteArray()
					}
				}
			}
		}

		File(filesDir, "export").mkdir()

		// Write text file with recipes
		val recipeFile = File(File(filesDir, "export"), if (format == "xml") "$filename.xml" else "$filename.json")
		recipeFile.outputStream().use {
			it.write(
				if (format == "xml")
					GourmetXmlWriter(getDecimalSeparator()).serialize(recipes + refs)
				else
					RecipeJsonAdapter().toJson(RecipeList(recipes + refs)).encodeToByteArray()
			)
		}

		if (format == "zip") {
			ZipOutputStream(BufferedOutputStream(File(File(filesDir, "export"), "$filename.zip").outputStream())).use { zipOS ->
				zipOS.putNextEntry(ZipEntry("$filename.json"))
				recipeFile.inputStream().use { it.copyTo(zipOS) }

				(recipes + refs).map{ it.recipe.id }.forEach { recipeId ->
					File(File(filesDir, "images"), "$recipeId.jpg").let { imageFile ->
						if (imageFile.exists()) {
							zipOS.putNextEntry(ZipEntry("$recipeId.jpg"))
							imageFile.inputStream().use{ it.copyTo(zipOS) }
						}
					}
				}
			}
		}

	}

	fun getDecimalSeparator() =
		if (getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
				.getBoolean(Constants.PREF_DECIMAL_SEPARATOR_COMMA, DecimalFormatSymbols.getInstance().decimalSeparator == ','))
			','
		else
			'.'

}