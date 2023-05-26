package eu.zimbelstern.tournant.gourmand

import android.util.Base64
import android.util.Xml
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.RecipeWithIngredients
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

class GourmetXmlWriter {

	private val writer = StringWriter()
	private val serializer = Xml.newSerializer().apply {
		setOutput(writer)
		startDocument("UTF-8", true)
		text("\n")
		setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
		docdecl(" gourmetDoc")
	}

	fun serialize(recipes: List<RecipeWithIngredients>): ByteArray {
		serializer.apply {
			startTag(null, "gourmetDoc")

			for (recipe in recipes) {
				writeRecipe(recipe)
			}

			endTag(null, "gourmetDoc")
			endDocument()
			flush()
		}
		return writer.toString().toByteArray()
	}

	private fun XmlSerializer.writeRecipe(recipe: RecipeWithIngredients) {
		startTag(null, "recipe")
		attribute(null, "id", recipe.recipe.id.toString())

		startTag(null, "title")
		text(recipe.recipe.title)
		endTag(null, "title")

		recipe.recipe.category?.let {
			startTag(null, "category")
			text(it)
			endTag(null, "category")
		}

		recipe.recipe.cuisine?.let {
			startTag(null, "cuisine")
			text(it)
			endTag(null, "cuisine")
		}

		recipe.recipe.source?.let {
			startTag(null, "source")
			text(it)
			endTag(null, "source")
		}

		recipe.recipe.link?.let {
			startTag(null, "link")
			text(it)
			endTag(null, "link")
		}

		recipe.recipe.rating?.let {
			startTag(null, "rating")
			text("$it/5")
			endTag(null, "rating")
		}

		recipe.recipe.preptime?.let {
			startTag(null, "preptime")
			text("$it minutes")
			endTag(null, "preptime")
		}

		recipe.recipe.cooktime?.let {
			startTag(null, "cooktime")
			text("$it minutes")
			endTag(null, "cooktime")
		}

		recipe.recipe.yieldValue?.let {
			startTag(null, "yields")
			text("${recipe.recipe.yieldValue} ${recipe.recipe.yieldUnit}")
			endTag(null, "yields")
		}

		if (recipe.ingredients.isNotEmpty()) {
			startTag(null, "ingredient-list")
			var group: String? = null

			for (ingredient in recipe.ingredients) {
				if (ingredient.group != group) {
					if (group != null)
						endTag(null, "inggroup")
					group = ingredient.group
					if (group != null) {
						startTag(null, "inggroup")
						startTag(null, "groupname")
						text(group)
						endTag(null, "groupname")
					}
				}
				writeIngredient(ingredient)
			}

			if (group != null)
				endTag(null, "inggroup")
			endTag(null, "ingredient-list")
		}

		recipe.recipe.instructions?.let {
			startTag(null, "instructions")
			text(it.replace("<br/>", "\n").replace("<", "&lt;").replace(">", "&gt;"))
			endTag(null, "instructions")
		}

		recipe.recipe.notes?.let {
			startTag(null, "modifications")
			text(it.replace("<br/>", "\n").replace("<", "&lt;").replace(">", "&gt;"))
			endTag(null, "modifications")
		}

		recipe.recipe.image?.let {
			startTag(null, "image")
			attribute(null, "format", "jpg")
			cdsect(Base64.encodeToString(it, Base64.DEFAULT))
			endTag(null, "image")
		}

		endTag(null, "recipe")
	}

	private fun XmlSerializer.writeIngredient(ingredient: Ingredient) {
		val amountString =
			if (ingredient.amountRange != null)
				"${ingredient.amount}-${ingredient.amountRange}"
			else
				"${ingredient.amount}"

		if (ingredient.refId != null) {
			startTag(null, "ingref")
			attribute(null, "refid", ingredient.refId.toString())
			attribute(null, "amount", amountString)
			ingredient.item?.let {
				text(it)
			}
			endTag(null, "ingref")
		} else {
			startTag(null, "ingredient")
			if (ingredient.optional)
				attribute(null, "optional", "yes")
			ingredient.amount?.let {
				startTag(null, "amount")
				text(amountString)
				endTag(null, "amount")
			}
			ingredient.unit?.let {
				startTag(null, "unit")
				text(it)
				endTag(null, "unit")
			}
			ingredient.item?.let {
				startTag(null, "item")
				text(it)
				endTag(null, "item")
			}
			endTag(null, "ingredient")
		}
	}

}