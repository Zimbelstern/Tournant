package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.RecipeWithIngredients
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
		val testRecipes = generateSampleRecipeWithIngredients(2)
		assertEquals(testRecipes, exportImportWithChar(testRecipes, '.'))
		assertEquals(testRecipes, exportImportWithChar(testRecipes, ','))
	}

	private fun exportImportWithChar(testRecipes: List<RecipeWithIngredients>, char: Char) : List<RecipeWithIngredients> {
		val xmlByteArray = GourmetXmlWriter(char).serialize(testRecipes)
		return GourmetXmlParser(char).parse(xmlByteArray.inputStream()).mapIndexed { i, it ->
			it.copy(recipe = it.recipe.copy(
				// Copy gourmandId from the original one as the IDs are not expected to be the same
				gourmandId = testRecipes[i].recipe.gourmandId,
				// Replace <br/> with \n because it will be displayed the same
				instructions = it.recipe.instructions?.replace("<br/>", "\n"),
				notes = it.recipe.notes?.replace("<br/>", "\n"),
			))
		}
	}

	private fun generateSampleRecipeWithIngredients(n: Int) = mutableListOf<RecipeWithIngredients>().apply {
		for (i in 1..n) {
			add(RecipeWithIngredients(
				Recipe(
					null,
					"Sample recipe",
					null,
					"Category",
					"Cuisine",
					"Source",
					"https://tournant.zimbelstern.eu",
					4.5f,
					60,
					30,
					2f,
					"portions",
					"Let's start:\n1. Do this, then\n2. do that\n3. and serve it!",
					"Notes"
				),
				mutableListOf(
					Ingredient(0, 2f, null, "mg", "Ingredient 1", null, false),
					Ingredient(1, 3f, null, "km", "Ingredient 2", null, true),
					Ingredient(2, .25f, null, "µF", "Ingredient 3", "Ingredient Group", true)
				)
			))
		}
	}.toList()

}