package ee.taltech.tictactoe

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsStorageTest {

    private val storage = StatisticsStorage(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun clearStoredStatistics() {
        storage.reset()
    }

    @Test
    fun freshStorage_loadsZeros() {
        assertEquals(Statistics(), storage.load())
    }

    @Test
    fun recordGame_updatesCountersAndTotalDuration() {
        storage.recordGame(winner = Player.X, isVsAi = true, durationMs = 5000L)
        storage.recordGame(winner = null, isVsAi = false, durationMs = 3000L)

        val expected = Statistics(
            gamesPlayed = 2,
            winsX = 1,
            winsO = 0,
            draws = 1,
            humanWinsVsAi = 1,
            aiWinsVsHuman = 0,
            totalDurationMs = 8000L,
        )
        assertEquals(expected, storage.load())
        assertEquals(4000L, storage.load().averageDurationMs)
    }

    @Test
    fun recordGame_aiWin_countsForOAndAi() {
        storage.recordGame(winner = Player.O, isVsAi = true, durationMs = 1000L)

        val statistics = storage.load()
        assertEquals(1, statistics.winsO)
        assertEquals(1, statistics.aiWinsVsHuman)
        assertEquals(0, statistics.humanWinsVsAi)
    }

    @Test
    fun reset_clearsEverything() {
        storage.recordGame(winner = Player.X, isVsAi = false, durationMs = 2000L)

        storage.reset()

        assertEquals(Statistics(), storage.load())
    }
}
