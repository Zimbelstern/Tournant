package eu.zimbelstern.tournant

fun List<IngredientListElement>.scale(factor: Float?): List<IngredientListElement> {
	return if (factor != null) {
		map {
			when (it) {
				is Ingredient -> it.withScaledAmount(factor)
				is IngredientReference -> it.withScaledAmount(factor)
				is IngredientGroup -> IngredientGroup(it.name, it.list.scale(factor))
				else -> it
			}
		}
	}
	else this
}