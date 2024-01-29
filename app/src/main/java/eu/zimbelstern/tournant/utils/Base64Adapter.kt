package eu.zimbelstern.tournant.utils

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class Base64Adapter {
	@FromJson
	fun fromJson(string: String?): ByteArray {
		return Base64.decode(string, Base64.NO_WRAP)
	}

	@ToJson
	fun toJson(bytes: ByteArray?): String {
		return Base64.encodeToString(bytes, Base64.NO_WRAP)
	}
}