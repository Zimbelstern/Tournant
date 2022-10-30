package eu.zimbelstern.tournant

import kotlinx.parcelize.Parcelize

@Parcelize
data class IngredientGroup(
	val name: String?,
	val list: List<IngredientListElement>
) : IngredientListElement()