package eu.zimbelstern.tournant.data

sealed class IngredientLine {
	data class IngredientGroupTitle(var title: String?) : IngredientLine()
	data class IngredientItem(val ingredient: Ingredient, val isChecked: Boolean = false) : IngredientLine()

	fun toStringForCooks(optionalString: String) =
		when (this) {
			is IngredientGroupTitle -> title ?: ""
			is IngredientItem -> buildString {
				append(ingredient.toStringForCooks(optionalString))
				if (isChecked) {
					append(" ✓")
				}
			}
		}
}