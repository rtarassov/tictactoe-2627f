package ee.taltech.tictactoe

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StatsActivity : AppCompatActivity() {

    private lateinit var statisticsStorage: StatisticsStorage
    private lateinit var textViewGamesPlayed: TextView
    private lateinit var textViewWinsX: TextView
    private lateinit var textViewWinsO: TextView
    private lateinit var textViewDraws: TextView
    private lateinit var textViewHumanWinsVsAi: TextView
    private lateinit var textViewAiWinsVsHuman: TextView
    private lateinit var textViewAverageDuration: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statisticsStorage = StatisticsStorage(this)
        textViewGamesPlayed = findViewById(R.id.textViewGamesPlayed)
        textViewWinsX = findViewById(R.id.textViewWinsX)
        textViewWinsO = findViewById(R.id.textViewWinsO)
        textViewDraws = findViewById(R.id.textViewDraws)
        textViewHumanWinsVsAi = findViewById(R.id.textViewHumanWinsVsAi)
        textViewAiWinsVsHuman = findViewById(R.id.textViewAiWinsVsHuman)
        textViewAverageDuration = findViewById(R.id.textViewAverageDuration)
    }

    override fun onResume() {
        super.onResume()
        renderStatistics()
    }

    fun buttonResetStatisticsClicked(view: View) {
        statisticsStorage.reset()
        renderStatistics()
    }

    private fun renderStatistics() {
        val statistics = statisticsStorage.load()
        textViewGamesPlayed.text = countText(statistics.gamesPlayed)
        textViewWinsX.text = countText(statistics.winsX)
        textViewWinsO.text = countText(statistics.winsO)
        textViewDraws.text = countText(statistics.draws)
        textViewHumanWinsVsAi.text = countText(statistics.humanWinsVsAi)
        textViewAiWinsVsHuman.text = countText(statistics.aiWinsVsHuman)

        val averageSeconds = statistics.averageDurationMs / 1000
        val minutes = averageSeconds / 60
        val seconds = averageSeconds % 60
        textViewAverageDuration.text = getString(R.string.stats_duration_format, minutes, seconds)
    }

    private fun countText(count: Int): String = getString(R.string.stats_count_format, count)
}
