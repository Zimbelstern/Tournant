package eu.zimbelstern.tournant.screenshots

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.InstructionsTextAdapter
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.ui.RecipeActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.locale.LocaleTestRule
import utils.AndroidTestUtils
import java.util.Locale

@LargeTest
class CaptureRecipeActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		CleanStatusBar.enableWithDefaults()
		ActivityScenario.launch<RecipeActivity>(Intent(ApplicationProvider.getApplicationContext(), RecipeActivity::class.java).apply {
			putExtra("RECIPE", AndroidTestUtils.buildTestRecipeFromResources())
		})
	}

	@After
	fun tearDown() {
		CleanStatusBar.disable()
	}

	@Test
	fun captureRecipeView() {
		onView(withId(R.id.recipe_detail_ingredients_recycler)).perform(scrollTo())
			.perform(
				*listOf(0, 1, 3, 4).map {
					actionOnItemAtPosition<InstructionsTextAdapter.InstructionTextViewHolder>(it, click())
				}.toTypedArray()
			)

		Screengrab.screenshot("2")

		onView(withId(R.id.recipe_detail_instructions_recycler)).perform(scrollTo(), swipeUp())
			.perform(
				*(0..2).map {
					actionOnItemAtPosition<InstructionsTextAdapter.InstructionTextViewHolder>(it, click())
				}.toTypedArray()
			)

		Screengrab.screenshot("3")
	}

}