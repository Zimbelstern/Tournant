package eu.zimbelstern.tournant.screenshots

import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
class CaptureMainActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		ActivityScenario.launch(MainActivity::class.java)
	}

	@Test
	fun captureMainView() {
		Screengrab.screenshot("1")
	}

}