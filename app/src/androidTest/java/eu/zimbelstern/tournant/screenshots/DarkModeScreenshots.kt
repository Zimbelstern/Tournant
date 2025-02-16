package eu.zimbelstern.tournant.screenshots

import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import utils.clickItemWithFiveStars
import utils.wait


@RunWith(AndroidJUnit4::class)
@LargeTest
class DarkModeScreenshots {

	@get:Rule
	var activityScenarioRule = activityScenarioRule<MainActivity>()

	@Test
	fun takeScreenshotsB() {
		onView(withId(R.id.recipe_list_recycler))
			.perform(wait(1000))
		onView(withId(R.id.search))
			.perform(click())
		onView(isRoot())
			.perform(closeSoftKeyboard())
			.perform(captureToBitmap { it.writeToTestStorage("4") })
	}

	@Test
	fun takeScreenshotsC() {
		onView(withId(R.id.recipe_list_recycler))
			.perform(wait(1000))
			.perform(clickItemWithFiveStars())
		onView(withId(R.id.edit))
			.perform(click())
		onView(isRoot())
			.perform(captureToBitmap { it.writeToTestStorage("5") })
	}

	@Test
	fun takeScreenshotsD() {
		onView(withId(R.id.recipe_list_recycler))
			.perform(wait(1000))
		onView(withId(R.id.nav_drawer_button))
			.perform(click())
		onView(withText(R.string.settings))
			.perform(click())
		onView(isRoot())
			.perform(captureToBitmap { it.writeToTestStorage("6") })
	}

}
