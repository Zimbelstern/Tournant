package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.gourmand.GourmetXmlParser
import eu.zimbelstern.tournant.gourmand.GourmetXmlWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportImportTest {

	@Test
	fun exportImportTest() {
		val testRecipes = sampleRecipeWithIngredients(2)
		assertEquals(testRecipes, exportImportWithChar(testRecipes, '.'))
		assertEquals(testRecipes, exportImportWithChar(testRecipes, ','))
	}

	private fun exportImportWithChar(testRecipes: List<Recipe>, char: Char) : List<Recipe> {
		val xmlByteArray = GourmetXmlWriter(char).serialize(testRecipes)
		println(xmlByteArray.decodeToString())
		return GourmetXmlParser(char).parse(xmlByteArray.inputStream()).mapIndexed { i, it ->
			it.copy(
				// Copy data IDs and timestamps as they are not expected to be the same
				id = it.gourmandId!!.toLong(),
				gourmandId = testRecipes[i].gourmandId,
				keywords = testRecipes[i].keywords,
				season = testRecipes[i].season,
				created = testRecipes[i].created,
				modified = testRecipes[i].modified,
				// Replace <br/> with \n because it will be displayed the same
				instructions = it.instructions?.replace("<br/>", "\n"),
				notes = it.notes?.replace("<br/>", "\n"),
				preparations = testRecipes[i].preparations,
				ingredients = testRecipes[i].ingredients.onEach { if (it.refId != null) it.unit = null }
			)
		}
	}

}