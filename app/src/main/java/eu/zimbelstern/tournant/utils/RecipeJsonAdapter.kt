package eu.zimbelstern.tournant.utils

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import eu.zimbelstern.tournant.data.Cookbook
import eu.zimbelstern.tournant.data.RecipeList
import java.util.Date

object RecipeJsonAdapter {
	val adapter: JsonAdapter<Cookbook> by lazy {
		Moshi.Builder()
			.add(object {
				@FromJson
				fun fromJson(list: List<String>) = LinkedHashSet(list)
				@ToJson
				fun toJson(set: LinkedHashSet<String>) = set.toList()
			})
			.add(object {
				@FromJson
				fun fromJson(string: String?) = Base64.decode(string, Base64.NO_WRAP)
				@ToJson
				fun toJson(bytes: ByteArray?) = Base64.encodeToString(bytes, Base64.NO_WRAP)
			})
			.add(object {
				@FromJson
				fun fromJson(long: Long?) = long?.let { Date(it) }
				@ToJson
				fun toJson(date: Date?) = date?.time
			})
			.build()
			.adapter(Cookbook::class.java)
			.indent("\t")
	}
	val oldAdapter: JsonAdapter<RecipeList> by lazy {
		Moshi.Builder()
			.add(KotlinJsonAdapterFactory())
			.add(object {
				@FromJson
				fun fromJson(string: String?) = Base64.decode(string, Base64.NO_WRAP)
				@ToJson
				fun toJson(bytes: ByteArray?) = Base64.encodeToString(bytes, Base64.NO_WRAP)
			})
			.add(object {
				@FromJson
				fun fromJson(long: Long?) = long?.let { Date(it) }
				@ToJson
				fun toJson(date: Date?) = date?.time
			})
			.build()
			.adapter(RecipeList::class.java)
			.indent("\t")
	}
}