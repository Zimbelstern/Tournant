package eu.zimbelstern.tournant

import org.junit.Assert.assertEquals
import org.junit.Test


class ExtensionsTest {

	/* Just a simple test. */
	@Test
	fun roundToNDigits_test() {
		assertEquals(232f, 231.5f.roundToNDigits(0))
	}

}