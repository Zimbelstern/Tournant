package eu.zimbelstern.tournant.utils

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import eu.zimbelstern.tournant.data.Cookbook
import eu.zimbelstern.tournant.data.RecipeList
import eu.zimbelstern.tournant.data.Season
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
					?: string?.let { Locale.forLanguageTag(it) }
				@ToJson
				fun toJson(locale: Locale) = locale.toLanguageTag()
			})
			.add(object {
				@FromJson
				fun fromJson(reader: JsonReader): Season? {
					var from: Int? = null
					var until: Int? = null

					reader.beginObject()
					while (reader.hasNext()) {
						when (reader.nextName()) {
							"from" -> from = if (reader.peek() != JsonReader.Token.NULL ) reader.nextInt() else reader.nextNull()
							"until" -> until = if (reader.peek() != JsonReader.Token.NULL ) reader.nextInt() else reader.nextNull()
						}
					}
					reader.endObject()

					return from?.let { fromNotNull -> until?.let { untilNotNull -> Season(fromNotNull, untilNotNull) } }
				}
				@ToJson
				fun toJson(writer: JsonWriter, season: Season?) {
					writer.beginObject()
					writer.name("from").value(season?.from)
					writer.name("until").value(season?.until)
					writer.endObject()
				}
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
				@Suppress("Unused_Parameter") // string will always be null, falling back to app language
				fun fromJson(string: String?) = getAppOrSystemLocale()
				@ToJson
				fun toJson(locale: Locale) = locale.toLanguageTag()
			})
			.build()
			.adapter(RecipeList::class.java)
			.indent("\t")
	}
}