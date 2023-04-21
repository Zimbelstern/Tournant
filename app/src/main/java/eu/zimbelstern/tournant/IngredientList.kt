package eu.zimbelstern.tournant

import android.util.Log
import eu.zimbelstern.tournant.data.Ingredient
import eu.zimbelstern.tournant.gourmand.XmlIngredient
import eu.zimbelstern.tournant.gourmand.XmlIngredientGroup
import eu.zimbelstern.tournant.gourmand.XmlIngredientListElement
import eu.zimbelstern.tournant.gourmand.XmlIngredientReference

fun List<XmlIngredientListElement>.scale(factor: Float?): List<XmlIngredientListElement> {
	return if (factor != null) {
		map {
			when (it) {
				is XmlIngredient -> it.withScaledAmount(factor)
				is XmlIngredientReference -> it.withScaledAmount(factor)
				is XmlIngredientGroup -> XmlIngredientGroup(it.name, it.list.scale(factor))
				else -> it
			}
		}
	}
	else this
}

fun List<XmlIngredientListElement>.toIngredientList(startPosition: Int = 0, inGroup: String? = null): MutableList<Ingredient> {
	val ingredients = mutableListOf<Ingredient>()
	for (element in this) {
		when (element) {
			is XmlIngredient -> ingredients.add(element.toIngredient(startPosition + ingredients.size, inGroup))
			is XmlIngredientReference -> ingredients.add(element.toIngredient(startPosition + ingredients.size, inGroup))
			is XmlIngredientGroup -> ingredients.addAll(element.list.toIngredientList(startPosition + ingredients.size, element.name))
		}
	}
	return ingredients
}