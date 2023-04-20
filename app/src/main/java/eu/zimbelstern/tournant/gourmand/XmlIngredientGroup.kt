package eu.zimbelstern.tournant.gourmand

import kotlinx.parcelize.Parcelize

@Parcelize
data class XmlIngredientGroup(
	val name: String?,
	val list: List<XmlIngredientListElement>
) : XmlIngredientListElement()