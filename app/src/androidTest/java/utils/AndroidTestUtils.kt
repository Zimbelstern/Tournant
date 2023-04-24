package utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import eu.zimbelstern.tournant.test.R
import java.io.ByteArrayOutputStream

class AndroidTestUtils {
	companion object {
		fun buildTestRecipeFromResources() = InstrumentationRegistry.getInstrumentation().context.run {
			RecipeWithIngredients(
				Recipe(
					null,
					getString(R.string.sample_title),
					null,
					getString(R.string.sample_category),
					getString(R.string.sample_cuisine),
					getString(R.string.sample_source),
					null,
					5f,
					25,
					20,
					getString(R.string.sample_yields).split(" ")[0].toFloat(),
					getString(R.string.sample_yields).split(" ")[1],
					getString(R.string.sample_instructions),
					getString(R.string.sample_modifications),
					ByteArrayOutputStream().also {
						BitmapFactory.decodeResource(resources, R.drawable.image)
							.compress(Bitmap.CompressFormat.JPEG, 75, it)
					}.toByteArray()
				),
				resources.getStringArray(R.array.sample_ingredients).mapIndexed { i, it ->
					if (it[0].isDigit()) {
						val ingredient = it.split(" ")
						if (ingredient.size > 2) {
							Ingredient(i, ingredient[0].toFloat(), null, ingredient[1], ingredient.drop(2).joinToString(" "), null, false)
						} else {
							Ingredient(i, ingredient[0].toFloat(), null, null, ingredient[1], null, false)
						}
					} else {
						Ingredient(i, null, null, null, it, null, false)
					}
				}.toMutableList()
			)
		}
	}
}