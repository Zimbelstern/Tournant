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
		val testRecipes = sampleRecipeWithIngredients(2)
		assertEquals(testRecipes, exportImportWithChar(testRecipes, '.').mapIndexed { index, recipeWithIngredients ->
			recipeWithIngredients.apply {
				recipe.created = testRecipes[index].recipe.created
				recipe.modified = testRecipes[index].recipe.modified
			}
		})
		assertEquals(testRecipes, exportImportWithChar(testRecipes, ',').mapIndexed { index, recipeWithIngredients ->
			recipeWithIngredients.apply {
				recipe.created = testRecipes[index].recipe.created
				recipe.modified = testRecipes[index].recipe.modified
			}
		})
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

	private fun sampleRecipeWithIngredients(n: Int) = mutableListOf<RecipeWithIngredients>().apply {
		for (i in 1..n) {
			add(RecipeWithIngredients(
				recipe = Recipe(
					gourmandId = null,
					title = "Sample recipe",
					description = null,
					category = "Category",
					cuisine = "Cuisine",
					source = "Source",
					link = "https://tournant.zimbelstern.eu",
					rating = 4.5f,
					preptime = 60,
					cooktime = 30,
					yieldValue = 2.0,
					yieldUnit = "portions",
					instructions = "Let's start:\n1. Do this, then\n2. do that\n3. and serve it!",
					notes = "Notes"
				),
				ingredients = mutableListOf(
					Ingredient(0, 2.0, null, "mg", "Ingredient 1", null, false),
					Ingredient(1, 3.0, null, "km", "Ingredient 2", null, true),
					Ingredient(2, 0.25, null, "µF", "Ingredient 3", "Ingredient Group", true)
				),
			))
		}
	}.toList()

}