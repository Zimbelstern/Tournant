package utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import eu.zimbelstern.tournant.gourmand.XmlIngredient
import eu.zimbelstern.tournant.gourmand.XmlRecipe
import eu.zimbelstern.tournant.test.R
import java.io.ByteArrayOutputStream

class AndroidTestUtils {
	companion object {
		fun buildTestRecipeFromResources() = InstrumentationRegistry.getInstrumentation().context.run {
			XmlRecipe(null,
				getString(R.string.sample_title),
				getString(R.string.sample_category),
				getString(R.string.sample_cuisine),
				getString(R.string.sample_source),
				null,
				5f,
				getString(R.string.sample_preptime),
				getString(R.string.sample_cooktime),
				getString(R.string.sample_yields),
				resources.getStringArray(R.array.sample_ingredients).map {
					if (it[0].isDigit()) {
						val ingredient = it.split(" ")
						if (ingredient.size > 2) {
							XmlIngredient(ingredient[0], ingredient[1], ingredient.drop(2).joinToString(" "), null, null)
						} else {
							XmlIngredient(ingredient[0], null, ingredient[1], null, null)
						}
					} else {
						XmlIngredient(null, null, it, null, null)
					}
				},
				getString(R.string.sample_instructions),
				getString(R.string.sample_modifications),
				ByteArrayOutputStream().also {
					BitmapFactory.decodeResource(resources, R.drawable.image)
						.compress(Bitmap.CompressFormat.JPEG, 75, it)
				}.toByteArray()
			)
		}
	}
}