package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Cookbook
import eu.zimbelstern.tournant.utils.RecipeJsonAdapter
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File

class JsonAdapterTest {

	@Test
	fun jsonTest() {
		val json = RecipeJsonAdapter.adapter.toJson(Cookbook(sampleRecipeWithIngredients(1)))
		val expected = File("src/test/java/eu/zimbelstern/tournant/JsonAdapterTestOutput").readText()
		assertEquals(expected, json)
	}

}