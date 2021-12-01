package eu.zimbelstern.tournant

import android.util.Base64
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

class GourmetXmlParser {

	@Throws(XmlPullParserException::class, IOException::class)
	fun parse(inputStream: InputStream): List<Recipe> {
		inputStream.use {
			val parser: XmlPullParser = Xml.newPullParser()
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
			parser.setInput(it, null)
			parser.nextTag()
			return readGourmetDoc(parser)
		}
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readGourmetDoc(parser: XmlPullParser): List<Recipe> {
		val recipes = mutableListOf<Recipe>()
		parser.require(XmlPullParser.START_TAG, null, "gourmetDoc")
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			if (parser.name == "recipe") {
				recipes.add(readRecipe(parser))
			} else {
				skip(parser)
			}
		}
		return recipes
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readRecipe(parser: XmlPullParser): Recipe {
		parser.require(XmlPullParser.START_TAG, null, "recipe")
		val id = parser.getAttributeValue(null, "id").toInt()
		var title: String? = null
		var category: String? = null
		var cuisine: String? = null
		var source: String? = null
		var link: String? = null
		var rating: Float? = null
		var preptime: String? = null
		var cooktime: String? = null
		var yields: String? = null
		var ingredientList: List<Ingredient>? = null
		var instructions: String? = null
		var modifications: String? = null
		var image: ByteArray? = null
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			when (parser.name) {
				"title" -> title = readStringField(parser)
				"category" -> category = readStringField(parser)
				"cuisine" -> cuisine = readStringField(parser)
				"rating" -> rating = readStars(parser)
				"source" -> source = readStringField(parser)
				"link" -> link = readStringField(parser)
				"preptime" -> preptime = readStringField(parser)
				"cooktime" -> cooktime = readStringField(parser)
				"yields" -> yields = readStringField(parser)
				"ingredient-list" -> ingredientList = readIngredientList(parser)
				"instructions" -> instructions = readStringField(parser)
				"modifications" -> modifications = readStringField(parser)
				"image" -> image = readImageField(parser)
				else -> skip(parser)
			}
		}
		return Recipe(id, title, category, cuisine, source, link, rating, preptime, cooktime, yields, ingredientList, instructions, modifications, image)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredientList(parser: XmlPullParser): List<Ingredient> {
		val ingredients = mutableListOf<Ingredient>()
		parser.require(XmlPullParser.START_TAG, null, "ingredient-list")
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			if (parser.name == "ingredient") {
				ingredients.add(readIngredient(parser))
			} else {
				skip(parser)
			}
		}
		return ingredients
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredient(parser: XmlPullParser): Ingredient {
		parser.require(XmlPullParser.START_TAG, null, "ingredient")
		var amount: String? = null
		var unit: String? = null
		var item: String? = null
		var key: String? = null
		val optional = parser.getAttributeValue(null, "optional") == "yes"
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			when (parser.name) {
				"amount" -> amount = readStringField(parser)
				"unit" -> unit = readStringField(parser)
				"item" -> item = readStringField(parser)
				"key" -> key = readStringField(parser)
				else -> skip(parser)
			}
		}
		return Ingredient(amount, unit, item, key, optional)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readImageField(parser: XmlPullParser): ByteArray {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val image = readBase64Cdata(parser)
		parser.require(XmlPullParser.END_TAG, null, tag)
		return image
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readBase64Cdata(parser: XmlPullParser): ByteArray {
		var cdataString = ""
		if (parser.next() == XmlPullParser.TEXT) {
			cdataString = parser.text
			parser.nextTag()
		}
		return Base64.decode(cdataString, Base64.DEFAULT)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readStars(parser: XmlPullParser): Float? {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val stars = readText(parser).substringBefore("/5 ", "").toFloatOrNull()
		parser.require(XmlPullParser.END_TAG, null, tag)
		return stars
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readStringField(parser: XmlPullParser): String {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val title = readText(parser)
		parser.require(XmlPullParser.END_TAG, null, tag)
		return title
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readText(parser: XmlPullParser): String {
		var result = ""
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.text
			parser.nextTag()
		}
		return result
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun skip(parser: XmlPullParser) {
		if (parser.eventType != XmlPullParser.START_TAG) {
			throw  IllegalStateException()
		}
		var depth = 1
		while (depth != 0) {
			when (parser.next()) {
				XmlPullParser.END_TAG -> depth--
				XmlPullParser.START_TAG -> depth++
			}
		}
	}

}