package utils

import android.view.View
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Description
import org.hamcrest.Matcher

fun clickItemWithFiveStars(): RecyclerViewActions.PositionableRecyclerViewAction = actionOnItem<RecyclerView.ViewHolder>(
	hasDescendant(
		object : BoundedMatcher<View, AppCompatRatingBar>(AppCompatRatingBar::class.java) {
			override fun describeTo(description: Description) {
				description.appendText("(visible) rating of 5 stars")
			}
			override fun matchesSafely(item: AppCompatRatingBar): Boolean {
				return item.isVisible && item.rating == 5f
			}
		}
	),
	clickWithOffset(10, 10)
)

fun clickWithOffset(x: Int, y: Int, longClick: Boolean = false) = GeneralClickAction(
	if (longClick) Tap.LONG else Tap.SINGLE,
	{ view ->
		val screenPos = IntArray(2)
		view.getLocationOnScreen(screenPos)

		floatArrayOf(
			(screenPos[0] + x).toFloat(),
			(screenPos[1] + y).toFloat()
		)
	},
	Press.FINGER,
	0,
	0
)

fun wait(delay: Long) = object : ViewAction {
	override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
	override fun getDescription() = "wait for $delay milliseconds"
	override fun perform(uiController: UiController, v: View?) {
		uiController.loopMainThreadForAtLeast(delay)
	}
}
