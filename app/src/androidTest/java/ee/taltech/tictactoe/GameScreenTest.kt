package ee.taltech.tictactoe

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(stringId: Int, vararg formatArguments: Any): String =
        targetContext.getString(stringId, *formatArguments)

    private fun playXWinsTopRow() {
        onView(withId(R.id.buttonCell00)).perform(click())
        onView(withId(R.id.buttonCell10)).perform(click())
        onView(withId(R.id.buttonCell01)).perform(click())
        onView(withId(R.id.buttonCell11)).perform(click())
        onView(withId(R.id.buttonCell02)).perform(click())
    }

    @Test
    fun newGame_showsXTurn() {
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun xWinsTopRow_showsWinnerText() {
        playXWinsTopRow()

        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_winner, "X"))))
        onView(withId(R.id.buttonCell02)).check(matches(withText("X")))
        onView(withId(R.id.buttonCell10)).check(matches(withText("O")))
    }

    @Test
    fun tappingOccupiedCell_keepsSymbolAndTurn() {
        onView(withId(R.id.buttonCell11)).perform(click())

        onView(withId(R.id.buttonCell11)).perform(click())

        onView(withId(R.id.buttonCell11)).check(matches(withText("X")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "O"))))
    }
    @Test
    fun newGameMidGame_showsConfirmationDialog_andCancelKeepsBoard() {
        onView(withId(R.id.buttonCell00)).perform(click())
        onView(withId(R.id.buttonCell11)).perform(click())

        onView(withId(R.id.buttonNewGame)).perform(click())
        onView(withText(R.string.new_game_dialog_title)).check(matches(isDisplayed()))
        onView(withText(R.string.new_game_dialog_cancel)).perform(click())

        onView(withId(R.id.buttonCell00)).check(matches(withText("X")))
        onView(withId(R.id.buttonCell11)).check(matches(withText("O")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun newGameMidGame_confirmClearsBoard() {
        onView(withId(R.id.buttonCell00)).perform(click())

        onView(withId(R.id.buttonNewGame)).perform(click())
        onView(withText(R.string.new_game_dialog_confirm)).perform(click())

        onView(withId(R.id.buttonCell00)).check(matches(withText("")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun newGameAfterWin_clearsBoardWithoutDialog() {
        playXWinsTopRow()

        onView(withId(R.id.buttonNewGame)).perform(click())

        onView(withId(R.id.buttonCell00)).check(matches(withText("")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun recreate_keepsBoardTurnAndResult() {
        onView(withId(R.id.buttonCell00)).perform(click())
        onView(withId(R.id.buttonCell11)).perform(click())

        activityRule.scenario.recreate()

        onView(withId(R.id.buttonCell00)).check(matches(withText("X")))
        onView(withId(R.id.buttonCell11)).check(matches(withText("O")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))

        onView(withId(R.id.buttonCell01)).perform(click())
        onView(withId(R.id.buttonCell20)).perform(click())
        onView(withId(R.id.buttonCell02)).perform(click())
        activityRule.scenario.recreate()

        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_winner, "X"))))
    }

    @Test
    fun recreate_keepsConfirmationDialogOpen() {
        onView(withId(R.id.buttonCell00)).perform(click())
        onView(withId(R.id.buttonNewGame)).perform(click())

        activityRule.scenario.recreate()

        onView(withText(R.string.new_game_dialog_title)).check(matches(isDisplayed()))
    }
}
