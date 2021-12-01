package eu.zimbelstern.tournant

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient (
	val amount: String?,
	val unit: String?,
	val item: String?,
	val key: String?,
	val optional: Boolean?
) : Parcelable
