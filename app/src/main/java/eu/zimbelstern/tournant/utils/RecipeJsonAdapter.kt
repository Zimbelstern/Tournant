package eu.zimbelstern.tournant.utils

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import eu.zimbelstern.tournant.data.RecipeList

class RecipeJsonAdapter : JsonAdapter<RecipeList>() {

	override fun fromJson(p0: JsonReader) = innerAdapter.fromJson(p0)
	override fun toJson(p0: JsonWriter, p1: RecipeList?) = innerAdapter.toJson(p0, p1)

	private val innerAdapter: JsonAdapter<RecipeList> =
		Moshi.Builder()
			.add(object {
				@FromJson
				fun fromJson(string: String?) = Base64.decode(string, Base64.NO_WRAP)
				@ToJson
				fun toJson(bytes: ByteArray?) = Base64.encodeToString(bytes, Base64.NO_WRAP)
			})
			.build()
			.adapter(RecipeList::class.java)
			.indent("\t")

}