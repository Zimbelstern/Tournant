package eu.zimbelstern.tournant.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Cookbook(
	val recipes: List<Recipe>
)
