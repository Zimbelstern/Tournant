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
		"2 min or longer" to listOf(TimeExpression(120, 0..4)),
		"cook for 30 min, then let rest for 10 min" to listOf(TimeExpression(1800, 9..14), TimeExpression(600, 35..40)),
		"20 - 30 min" to listOf(TimeExpression(20*60, 0..10)),
		"4.5 to 5 minutes" to listOf(TimeExpression(270, 0..15)),
		"20 mini muffins" to emptyList(),
		"1'30''" to listOf(TimeExpression(90, 0..5)),
		"1‘30‘‘" to listOf(TimeExpression(90, 0..5)),
		"1'" to listOf(TimeExpression(60, 0..1)),
		"30''" to listOf(TimeExpression(30, 0..3)),
		"30\"" to listOf(TimeExpression(30, 0..2)),
		"30″" to listOf(TimeExpression(30, 0..2))
	)

	@Test
	fun findTimeExpressions_test() {
		stringsUnderTest.forEach { (string, result) ->
			assertEquals(
				result,
				SpannedString(string).toSpanned().findTimeExpressions("to", "h", "min|minutes", "s").toList()
			)
		}
	}

}