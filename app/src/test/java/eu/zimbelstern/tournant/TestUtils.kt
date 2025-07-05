package eu.zimbelstern.tournant

import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.data.Season
import java.util.Date

fun sampleRecipeWithIngredients(n: Int) =
	(1L..n).map { id ->
		Recipe(
			id = id,
			gourmandId = null,
			title = "Sample recipe",
			description = null,
			category = "Category",
			cuisine = "Cuisine",
			keywords = linkedSetOf("Keyword 1", "Keyword 2"),
			season = Season(3, 5),
			source = "Source",
			link = "https://tournant.zimbelstern.eu",
			rating = 4.5f,
			preptime = 60,
			cooktime = 30,
			yieldValue = 2.0,
			yieldUnit = "portions",
			instructions = "Let's start:\n1. Do this, then\n2. do that\n3. and serve it!",
			notes = "Notes",
			image = null,
			thumbnail = null,
			created = Date(0),
			modified = Date(1),
			ingredients = mutableListOf(
				Ingredient(
					amount = 3.0,
					amountRange = null,
					unit = "km",
					item = null,
					group = null,
					optional = false,
					refId = 3
				),
				Ingredient(
					amount = 2.0,
					amountRange = 3.0,
					unit = "mg",
					item = "Second ingredient",
					group = null,
					optional = false,
					refId = null
				),
				Ingredient(
					amount = 0.25,
					amountRange = null,
					unit = "µF",
					item = "Third ingredient",
					group = "Ingredient Group",
					optional = true,
					refId = null
				)
			),
			preparations = mutableListOf(
				Date(0),
				Date(84600),
				Date(84600)
			)
		)
}