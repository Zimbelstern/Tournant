package eu.zimbelstern.tournant.screenshots

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.ui.MainActivity
import eu.zimbelstern.tournant.ui.RecipeActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
class CaptureMainActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		CleanStatusBar.enableWithDefaults()
		ActivityScenario.launch(MainActivity::class.java)
	}

	@After
	fun tearDown() {
		CleanStatusBar.disable()
	}

	@Test
	fun captureMainView() {
		Screengrab.screenshot("1")
	}

}