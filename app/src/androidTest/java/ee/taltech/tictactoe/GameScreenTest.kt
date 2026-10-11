package ee.taltech.tictactoe

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val ROTATION_ANIMATION_WAIT_MS = 600L

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearStoredStatistics() {
        StatisticsStorage(targetContext).reset()
    }

    private fun text(stringId: Int, vararg formatArguments: Any): String =
        targetContext.getString(stringId, *formatArguments)

    private fun cell(row: Int, column: Int) = onView(withTagValue(equalTo(MainActivity.cellTag(row, column))))

    private fun playXWinsTopRow() {
        cell(0, 0).perform(click())
        cell(1, 0).perform(click())
        cell(0, 1).perform(click())
        cell(1, 1).perform(click())
        cell(0, 2).perform(click())
    }

    @Test
    fun newGame_showsXTurn() {
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun xWinsTopRow_showsWinnerText() {
        playXWinsTopRow()

        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_winner, "X"))))
        cell(0, 2).check(matches(withText("X")))
        cell(1, 0).check(matches(withText("O")))
    }

    @Test
    fun tappingOccupiedCell_keepsSymbolAndTurn() {
        cell(1, 1).perform(click())

        cell(1, 1).perform(click())

        cell(1, 1).check(matches(withText("X")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "O"))))
    }
    @Test
    fun newGameMidGame_showsConfirmationDialog_andCancelKeepsBoard() {
        cell(0, 0).perform(click())
        cell(1, 1).perform(click())

        onView(withId(R.id.buttonNewGame)).perform(click())
        onView(withText(R.string.new_game_dialog_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.new_game_dialog_cancel)).inRoot(isDialog()).perform(click())

        cell(0, 0).check(matches(withText("X")))
        cell(1, 1).check(matches(withText("O")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun newGameMidGame_confirmClearsBoard() {
        cell(0, 0).perform(click())

        onView(withId(R.id.buttonNewGame)).perform(click())
        onView(withText(R.string.new_game_dialog_confirm)).inRoot(isDialog()).perform(click())

        cell(0, 0).check(matches(withText("")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun newGameAfterWin_clearsBoardWithoutDialog() {
        playXWinsTopRow()

        onView(withId(R.id.buttonNewGame)).perform(click())

        cell(0, 0).check(matches(withText("")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
    }

    @Test
    fun recreate_keepsBoardTurnAndResult() {
        cell(0, 0).perform(click())
        cell(1, 1).perform(click())

        activityRule.scenario.recreate()

        cell(0, 0).check(matches(withText("X")))
        cell(1, 1).check(matches(withText("O")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))

        cell(0, 1).perform(click())
        cell(2, 0).perform(click())
        cell(0, 2).perform(click())
        activityRule.scenario.recreate()

        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_winner, "X"))))
    }

    @Test
    fun recreate_keepsConfirmationDialogOpen() {
        cell(0, 0).perform(click())
        onView(withId(R.id.buttonNewGame)).perform(click())

        activityRule.scenario.recreate()

        onView(withText(R.string.new_game_dialog_title)).inRoot(isDialog()).check(matches(isDisplayed()))
    }
    @Test
    fun statisticsButton_opensStatisticsScreen() {
        onView(withId(R.id.buttonStatistics)).perform(click())

        onView(withId(R.id.textViewStatsTitle)).check(matches(withText(R.string.stats_title)))
        onView(withId(R.id.textViewGamesPlayed)).check(matches(withText("0")))
        pressBack()
    }

    @Test
    fun finishedGame_isCountedOnStatisticsScreen() {
        playXWinsTopRow()

        onView(withId(R.id.buttonStatistics)).perform(click())

        onView(withId(R.id.textViewGamesPlayed)).check(matches(withText("1")))
        onView(withId(R.id.textViewWinsX)).check(matches(withText("1")))
        onView(withId(R.id.textViewWinsO)).check(matches(withText("0")))
        onView(withId(R.id.textViewDraws)).check(matches(withText("0")))
        pressBack()
    }

    @Test
    fun resetButton_clearsStatistics() {
        playXWinsTopRow()
        onView(withId(R.id.buttonStatistics)).perform(click())

        onView(withId(R.id.buttonResetStatistics)).perform(click())

        onView(withId(R.id.textViewGamesPlayed)).check(matches(withText("0")))
        onView(withId(R.id.textViewWinsX)).check(matches(withText("0")))
        pressBack()
    }
    @Test
    fun backToGameButton_returnsToGameScreen() {
        onView(withId(R.id.buttonStatistics)).perform(click())

        onView(withId(R.id.buttonBackToGame)).perform(click())

        onView(withId(R.id.textViewStatus)).check(matches(isDisplayed()))
    }

    @Test
    fun vsAiMode_aiAnswersAfterShortDelay() {
        onView(withId(R.id.switchVsAi)).perform(click())
        onView(withId(R.id.switchVsAi)).check(matches(isChecked()))
        onView(withId(R.id.buttonNewGame)).perform(click())

        cell(1, 1).perform(click())
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "O"))))

        Thread.sleep(MainActivity.AI_MOVE_DELAY_MS + 300)

        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))
        cell(1, 1).check(matches(withText("X")))
    }
    @Test
    fun undoButton_takesBackLastMove() {
        cell(0, 0).perform(click())
        cell(1, 1).perform(click())

        onView(withId(R.id.buttonUndo)).perform(click())

        cell(1, 1).check(matches(withText("")))
        cell(0, 0).check(matches(withText("X")))
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "O"))))
    }

    @Test
    fun undoButton_vsAi_takesBackBothMoves() {
        onView(withId(R.id.switchVsAi)).perform(click())
        onView(withId(R.id.buttonNewGame)).perform(click())
        cell(1, 1).perform(click())
        Thread.sleep(MainActivity.AI_MOVE_DELAY_MS + 300)
        onView(withId(R.id.textViewStatus)).check(matches(withText(text(R.string.status_turn, "X"))))

        onView(withId(R.id.buttonUndo)).perform(click())

        cell(1, 1).check(matches(withText("")))
        activityRule.scenario.onActivity { activity ->
            val boardTexts = (0 until 3).flatMap { row -> (0 until 3).map { column -> activity.findViewById<android.view.View>(R.id.gridLayoutBoard).findViewWithTag<android.widget.Button>(MainActivity.cellTag(row, column)).text.toString() } }
            assertEquals(List(9) { "" }, boardTexts)
        }
    }

    @Test
    fun rotateBoardButton_rotatesOnlyTheBoard() {
        onView(withId(R.id.buttonRotateBoard)).perform(click())
        Thread.sleep(ROTATION_ANIMATION_WAIT_MS)

        activityRule.scenario.onActivity { activity ->
            assertEquals(90f, activity.findViewById<android.view.View>(R.id.gridLayoutBoard).rotation)
            assertEquals(0f, activity.findViewById<android.view.View>(R.id.textViewStatus).rotation)
        }
    }

    @Test
    fun boardSettingsButton_opensDialog() {
        onView(withId(R.id.buttonBoardSettings)).perform(click())

        onView(withText(R.string.board_settings_rows)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.board_settings_cancel)).inRoot(isDialog()).perform(click())
    }
}
