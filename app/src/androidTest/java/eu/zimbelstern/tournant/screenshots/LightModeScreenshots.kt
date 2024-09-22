package eu.zimbelstern.tournant.screenshots

import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import eu.zimbelstern.tournant.R
import eu.zimbelstern.tournant.ui.MainActivity
import eu.zimbelstern.tournant.ui.adapter.InstructionsTextAdapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import utils.clickItemWithFiveStars
import utils.wait


@RunWith(AndroidJUnit4::class)
@LargeTest
class LightModeScreenshots {

	@get:Rule
	var activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun takeScreenshotsA() {
        onView(withId(R.id.recipe_list_recycler))
			.perform(wait(1000))
        onView(isRoot())
			.perform(captureToBitmap { it.writeToTestStorage("1") })

	    onView(withId(R.id.recipe_list_recycler))
			.perform(clickItemWithFiveStars())
	    onView(withId(R.id.recipe_detail_ingredients))
			.perform(scrollTo())
	    onView(isRoot())
			.perform(captureToBitmap { it.writeToTestStorage("2") })

	    onView(withId(R.id.recipe_detail_instructions_recycler))
			.perform(scrollTo())
		    .perform(
			    *(0..4).map {
				    actionOnItemAtPosition<InstructionsTextAdapter.InstructionTextViewHolder>(it, click())
			    }.toTypedArray()
		    )
	    onView(withId(R.id.recipe_detail_notes))
			.perform(scrollTo())
	    onView(isRoot()).perform(captureToBitmap { it.writeToTestStorage("3") })
    }
}
