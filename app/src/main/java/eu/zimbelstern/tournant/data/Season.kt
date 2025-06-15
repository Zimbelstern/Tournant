package eu.zimbelstern.tournant.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar

@Parcelize
data class Season(
	val from: Int,
	val until: Int
) : Parcelable {
	fun getIncludedMonths(): Set<Int> =
		(from .. (from + ((until - from + 12) % 12))).map {
			it % 12
		}.toSet()

	fun nowInSeason(): Boolean =
		Calendar.getInstance().get(Calendar.MONTH) in getIncludedMonths()

	companion object {

		fun createOrNull(from: Int?, until: Int?) = from?.let { fromNotNull ->
			until?.let { untilNotNull ->
				Season(fromNotNull, untilNotNull)
			}
		}

	}

}