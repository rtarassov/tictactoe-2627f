package ee.taltech.tictactoe

import android.content.Context
import android.content.SharedPreferences

class StatisticsStorage(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.preference_file_statistics),
        Context.MODE_PRIVATE
    )

    fun load(): Statistics = Statistics(
        gamesPlayed = readInt(R.string.stat_key_games_played),
        winsX = readInt(R.string.stat_key_wins_x),
        winsO = readInt(R.string.stat_key_wins_o),
        draws = readInt(R.string.stat_key_draws),
        humanWinsVsAi = readInt(R.string.stat_key_human_wins_vs_ai),
        aiWinsVsHuman = readInt(R.string.stat_key_ai_wins_vs_human),
        totalDurationMs = sharedPreferences.getLong(key(R.string.stat_key_total_duration_ms), 0L),
    )

    fun recordGame(winner: Player?, isVsAi: Boolean, durationMs: Long) {
        val current = load()
        val updated = current.copy(
            gamesPlayed = current.gamesPlayed + 1,
            winsX = current.winsX + countIf(winner == Player.X),
            winsO = current.winsO + countIf(winner == Player.O),
            draws = current.draws + countIf(winner == null),
            humanWinsVsAi = current.humanWinsVsAi + countIf(isVsAi && winner == Player.X),
            aiWinsVsHuman = current.aiWinsVsHuman + countIf(isVsAi && winner == Player.O),
            totalDurationMs = current.totalDurationMs + durationMs,
        )
        with(sharedPreferences.edit()) {
            putInt(key(R.string.stat_key_games_played), updated.gamesPlayed)
            putInt(key(R.string.stat_key_wins_x), updated.winsX)
            putInt(key(R.string.stat_key_wins_o), updated.winsO)
            putInt(key(R.string.stat_key_draws), updated.draws)
            putInt(key(R.string.stat_key_human_wins_vs_ai), updated.humanWinsVsAi)
            putInt(key(R.string.stat_key_ai_wins_vs_human), updated.aiWinsVsHuman)
            putLong(key(R.string.stat_key_total_duration_ms), updated.totalDurationMs)
            apply()
        }
    }

    fun reset() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }

    private fun readInt(keyStringId: Int): Int = sharedPreferences.getInt(key(keyStringId), 0)

    private fun key(keyStringId: Int): String = context.getString(keyStringId)

    private fun countIf(condition: Boolean): Int = if (condition) 1 else 0
}
