package eu.zimbelstern.tournant.screenshots

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.ui.RecipeEditingActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
class CaptureRecipeEditingActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		ActivityScenario.launch<RecipeEditingActivity>(Intent(ApplicationProvider.getApplicationContext(), RecipeEditingActivity::class.java).apply {
			putExtra("RECIPE_ID", 1L)
		})
	}

	@Test
	fun captureRecipeEditingView() {
		onView(withId(R.id.edit_ingredients_new_group)).perform(swipeUp())
		Screengrab.screenshot("4")
	}

}
