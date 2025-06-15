package eu.zimbelstern.tournant

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.text.toSpannable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.zimbelstern.tournant.data.Cookbook
import eu.zimbelstern.tournant.data.room.RecipeRoomDatabase
import eu.zimbelstern.tournant.gourmand.GourmandIssues
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import eu.zimbelstern.tournant.utils.RecipeJsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.use
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DecimalFormatSymbols
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class TournantApplication : Application() {

	companion object {
		private const val TAG = "TournantApplication"
	}

	val database: RecipeRoomDatabase by lazy {
		RecipeRoomDatabase.getDatabase(this)
	}

	fun withGourmandIssueCheck(context: Context, recipeIds: Set<Long>, onSuccess: (Set<Long>) -> Unit) {
		MainScope().launch {
			val issues = mutableSetOf<String>()
			withContext(Dispatchers.IO) {
				val recipes = database.recipeDao().getRecipesById(recipeIds)
				val refs = database.recipeDao().getReferencedRecipes(recipeIds)
				(recipes + refs).forEach {
					if (it.recipe.description != null)
						issues.add(GourmandIssues.NO_DESCRIPTIONS)
					if (it.recipe.yieldValue?.run { this <= this.toInt() } == false)
						issues.add(GourmandIssues.NO_FRACTIONS_IN_YIELD)
				}
			}
			if (issues.isNotEmpty()) {
				Log.i(TAG, "Exporting issues: $issues")
				val localizedIssues = issues.joinToString("\n") {
					when (it) {
						GourmandIssues.NO_DESCRIPTIONS -> getString(R.string.description)
						GourmandIssues.NO_FRACTIONS_IN_YIELD -> getString(R.string.gourmand165)
						else -> ""
					}
				}
				withContext(Dispatchers.Main) {
					MaterialAlertDialogBuilder(context)
						.setTitle(R.string.limitations)
						.setMessage(
							getString(R.string.missing_features_gourmand, localizedIssues)
								.toSpannable().apply {
									val start = getString(R.string.missing_features_gourmand).length - 2
									val end = length
									setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
								}
						)
						.setPositiveButton(R.string.ok) { _, _ -> onSuccess(recipeIds) }
						.setNegativeButton(R.string.cancel, null)
						.show()
				}
			}
			else {
				onSuccess(recipeIds)
			}
		}
	}

	fun writeRecipesToExportDir(recipeIds: Set<Long>, filename: String, format: String) {

		val recipeDao = database.recipeDao()

		val recipes = recipeDao.getRecipesById(recipeIds).map { it.toRecipe() }
		val refs = recipeDao.getReferencedRecipes(recipeIds).map { it.toRecipe() }

		if (format != "zip") {
			// Read externally saved images, compress and add to recipe objects
			(recipes + refs).forEach {
				val imageFile = File(File(filesDir, "images"), "${it.id}.jpg")
				if (imageFile.exists()) {
					imageFile.inputStream().use { inputStream ->
						val byteArrayOutputStream = ByteArrayOutputStream()
						val image = BitmapFactory.decodeStream(inputStream)
						image.scale(256, (image.height * 256f / image.width).roundToInt())
							.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
						it.image = byteArrayOutputStream.toByteArray()
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
					RecipeJsonAdapter.adapter.toJson(Cookbook(recipes + refs)).encodeToByteArray()
			)
		}

		if (format == "zip") {
			ZipOutputStream(BufferedOutputStream(File(File(filesDir, "export"), "$filename.zip").outputStream())).use { zipOS ->
				zipOS.putNextEntry(ZipEntry("$filename.json"))
				recipeFile.inputStream().use { it.copyTo(zipOS) }

				(recipes + refs).map{ it.id }.forEach { recipeId ->
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
		if (getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
				.getBoolean(Constants.PREF_DECIMAL_SEPARATOR_COMMA, DecimalFormatSymbols.getInstance().decimalSeparator == ','))
			','
		else
			'.'

}