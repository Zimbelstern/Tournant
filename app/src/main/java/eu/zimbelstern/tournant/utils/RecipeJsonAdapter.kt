package eu.zimbelstern.tournant.utils

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import eu.zimbelstern.tournant.data.Cookbook
import eu.zimbelstern.tournant.data.RecipeList
import eu.zimbelstern.tournant.getAppOrSystemLocale
import java.util.Date
import java.util.Locale

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
			.add(object {
				@FromJson
				fun fromJson(string: String?) = Locale.getAvailableLocales().find { it.toLanguageTag() == string }
					?: string?.let { Locale(it) }
				@ToJson
				fun toJson(locale: Locale) = locale.toLanguageTag()
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
			.add(object {
				@FromJson
				@Suppress("unused") // string will always be null, falling back to app language
				fun fromJson(string: String?) = getAppOrSystemLocale()
				@ToJson
				fun toJson(locale: Locale) = locale.toLanguageTag()
			})
			.build()
			.adapter(RecipeList::class.java)
			.indent("\t")
	}
}