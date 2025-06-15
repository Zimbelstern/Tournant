package eu.zimbelstern.tournant.gourmand

import android.util.Base64
import android.util.Xml
import androidx.core.text.parseAsHtml
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.data.Recipe
import eu.zimbelstern.tournant.extractFractionsToDouble
import eu.zimbelstern.tournant.withFractionsToDouble
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

class GourmetXmlParser(private val separator: Char) {

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
		return recipes.toList()
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readRecipe(parser: XmlPullParser): Recipe {
		parser.require(XmlPullParser.START_TAG, null, "recipe")
		val gourmandId = parser.getAttributeValue(null, "id").toInt()
		var title = ""
		var category: String? = null
		var cuisine: String? = null
		var source: String? = null
		var link: String? = null
		var rating: Float? = null
		var preptime: Int? = null
		var cooktime: Int? = null
		var yieldValue: Double? = null
		var yieldUnit: String? = null
		var ingredientList: MutableList<Ingredient> = mutableListOf()
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
				"preptime" -> preptime = readTime(parser)
				"cooktime" -> cooktime = readTime(parser)
				"yields" -> {
					val yield = readYield(parser)
					yieldValue = yield.first
					yieldUnit = yield.second
				}
				"ingredient-list" -> ingredientList = readIngredientList(parser)
				"instructions" -> instructions = readStringField(parser)
				"modifications" -> modifications = readStringField(parser)
				"image" -> image = readImageField(parser)
				else -> skip(parser)
			}
		}
		return Recipe(
			gourmandId = gourmandId,
			title = title,
			category = category,
			cuisine = cuisine,
			keywords = linkedSetOf(),
			source = source,
			link = link,
			rating = rating,
			preptime = preptime,
			cooktime = cooktime,
			yieldValue = yieldValue,
			yieldUnit = yieldUnit,
			instructions = instructions,
			notes = modifications,
			image = image,
			ingredients = ingredientList,
			preparations = mutableListOf()
		)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredientList(parser: XmlPullParser): MutableList<Ingredient> {
		val ingredients = mutableListOf<Ingredient>()
		parser.require(XmlPullParser.START_TAG, null, "ingredient-list")
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			when (parser.name) {
				"ingredient" -> ingredients.add(readIngredient(parser))
				"ingref" -> ingredients.add(readIngredientReference(parser))
				"inggroup" -> ingredients.addAll(readIngredientGroup(parser))
				else -> skip(parser)
			}
		}
		return ingredients
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredientGroup(parser: XmlPullParser): List<Ingredient> {
		parser.require(XmlPullParser.START_TAG, null, "inggroup")
		var name: String? = null
		val ingredients = mutableListOf<Ingredient>()
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			when (parser.name) {
				"groupname" -> name = readStringField(parser)
				"ingredient" -> ingredients.add(readIngredient(parser))
				"ingref" -> ingredients.add(readIngredientReference(parser))
				"inggroup" -> ingredients.addAll(readIngredientGroup(parser))
				else -> skip(parser)
			}
		}
		ingredients.forEach {
			it.group = name
		}
		return ingredients.toList()
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredient(parser: XmlPullParser): Ingredient {
		parser.require(XmlPullParser.START_TAG, null, "ingredient")
		var amountString: String? = null
		var unit: String? = null
		var item = ""
		val optional = parser.getAttributeValue(null, "optional") == "yes"
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.eventType != XmlPullParser.START_TAG) {
				continue
			}
			when (parser.name) {
				"amount" -> amountString = readStringField(parser)
				"unit" -> unit = readStringField(parser)
				"item" -> item = readStringField(parser)
				else -> skip(parser)
			}
		}
		val amount = amountString?.split("-")?.get(0)?.withFractionsToDouble(separator)
		val amountRange = if (amountString?.contains("-") == true)
			amountString.split("-")[1].withFractionsToDouble(separator)
		else null

		return Ingredient(amount, amountRange, unit, item, null, null, optional)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readIngredientReference(parser: XmlPullParser): Ingredient {
		parser.require(XmlPullParser.START_TAG, null, "ingref")
		val amountString = parser.getAttributeValue(null, "amount")
		val refId = parser.getAttributeValue(null, "refid").toLong()

		val amount = amountString?.split("-")?.get(0)?.withFractionsToDouble(separator)
		val amountRange = if (amountString?.contains("-") == true)
			amountString.split("-")[1].withFractionsToDouble(separator)
		else null

		readStringField(parser)
		parser.require(XmlPullParser.END_TAG, null, "ingref")

		return Ingredient(amount, amountRange, null, null, refId, null, false)
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
		val stars = readText(parser).substringBefore("/5", "").toFloatOrNull()
		parser.require(XmlPullParser.END_TAG, null, tag)
		return stars
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readTime(parser: XmlPullParser): Int? {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val timeString = readText(parser)
		parser.require(XmlPullParser.END_TAG, null, tag)
		return try {
			when {
				timeString.contains("hours") -> {
					if (timeString.contains("minutes")) {
						timeString.split(" ")[0].withFractionsToDouble(separator)!!.times(60)
							.plus(timeString.split(" ")[2].withFractionsToDouble(separator)!!)
							.toInt()
					} else {
						timeString.split(" ")[0].withFractionsToDouble(separator)?.times(60)?.toInt()
					}
				}
				timeString.contains("minutes") -> timeString.split(" ")[0].withFractionsToDouble(separator)?.toInt()
				else -> timeString.withFractionsToDouble(separator)?.toInt()
			}
		} catch (_: Exception) {
			null
		}
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readYield(parser: XmlPullParser): Pair<Double?, String?> {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val yieldString = readText(parser)
		parser.require(XmlPullParser.END_TAG, null, tag)
		return yieldString.extractFractionsToDouble(separator)
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readStringField(parser: XmlPullParser): String {
		val tag = parser.name
		parser.require(XmlPullParser.START_TAG, null, tag)
		val title = readText(parser).replace("\n", "&lt;br/>").parseAsHtml().toString()
		parser.require(XmlPullParser.END_TAG, null, tag)
		return title
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun readText(parser: XmlPullParser): String {
		var result = ""
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.text.trim()
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