package eu.zimbelstern.tournant

import android.text.SpannedString
import androidx.core.text.toSpanned
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExtensionsTest {

	/* Just a simple test. */
	@Test
	fun roundToNDigits_test() {
		assertEquals(232.0, 231.5.roundToNDigits(0), 0.0)
	}

	private val stringsUnderTest = listOf(
		"2 min" to listOf(2.0 to 0..4),
		"cook for 30 min, then let rest for 10 min" to listOf(30.0 to 9..14, 10.0 to 35..40),
		"20 - 30 min" to listOf(20.0 to 0..10),
		"4.5 to 5 minutes" to listOf(4.5 to 0..15),
		"20 mini muffins" to emptyList()
	)

	@Test
	fun findDurationsByRegex_test() {
		stringsUnderTest.forEach { (string, result) ->
			assertEquals(
				result,
				SpannedString(string).toSpanned().findDurationsByRegex("to", "min|minutes").toList()
			)
		}
	}

}