package utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import eu.zimbelstern.tournant.Ingredient
import eu.zimbelstern.tournant.Recipe
import eu.zimbelstern.tournant.test.R
import java.io.ByteArrayOutputStream

class AndroidTestUtils {
	companion object {
		fun buildTestRecipeFromResources() = InstrumentationRegistry.getInstrumentation().context.run {
			Recipe(null,
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
							Ingredient(ingredient[0], ingredient[1], ingredient.drop(2).joinToString(" "), null, null)
						} else {
							Ingredient(ingredient[0], null, ingredient[1], null, null)
						}
					} else {
						Ingredient(null, null, it, null, null)
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