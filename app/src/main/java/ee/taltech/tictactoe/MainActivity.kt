package ee.taltech.tictactoe

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var switchVsAi: Switch
    private lateinit var radioGroupStarter: RadioGroup
    private lateinit var textViewStatus: TextView
    private lateinit var cellButtons: List<Button>

    private var game = TicTacToeGame()
    private var isVsAi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        switchVsAi = findViewById(R.id.switchVsAi)
        radioGroupStarter = findViewById(R.id.radioGroupStarter)
        textViewStatus = findViewById(R.id.textViewStatus)
        cellButtons = listOf(
            findViewById(R.id.buttonCell00),
            findViewById(R.id.buttonCell01),
            findViewById(R.id.buttonCell02),
            findViewById(R.id.buttonCell10),
            findViewById(R.id.buttonCell11),
            findViewById(R.id.buttonCell12),
            findViewById(R.id.buttonCell20),
            findViewById(R.id.buttonCell21),
            findViewById(R.id.buttonCell22),
        )

        renderGame()
    }

    fun buttonCellClicked(view: View) {
        val cellIndex = cellButtons.indexOf(view)
        val row = cellIndex / TicTacToeGame.BOARD_SIZE
        val column = cellIndex % TicTacToeGame.BOARD_SIZE
        val moveWasMade = game.makeMove(row, column)
        if (moveWasMade && isVsAi && game.status == GameStatus.IN_PROGRESS) {
            game.makeRandomMove()
        }
        renderGame()
    }

    fun buttonNewGameClicked(view: View) {
        startNewGame()
    }

    fun buttonStatisticsClicked(view: View) {
    }

    private fun startNewGame() {
        isVsAi = switchVsAi.isChecked
        val startingPlayer = if (radioGroupStarter.checkedRadioButtonId == R.id.radioButtonStarterO) Player.O else Player.X
        game = TicTacToeGame(startingPlayer)
        if (isVsAi && startingPlayer == Player.O) {
            game.makeRandomMove()
        }
        renderGame()
    }

    private fun renderGame() {
        for (cellIndex in cellButtons.indices) {
            val row = cellIndex / TicTacToeGame.BOARD_SIZE
            val column = cellIndex % TicTacToeGame.BOARD_SIZE
            val cellButton = cellButtons[cellIndex]
            cellButton.text = symbolText(game.board[row][column])
            val isWinningCell = Cell(row, column) in game.winningCells
            val backgroundColorId = if (isWinningCell) R.color.cell_winning else R.color.cell_background
            cellButton.setBackgroundColor(ContextCompat.getColor(this, backgroundColorId))
        }
        textViewStatus.text = when (game.status) {
            GameStatus.IN_PROGRESS -> getString(R.string.status_turn, symbolText(game.currentPlayer))
            GameStatus.WON -> getString(R.string.status_winner, symbolText(game.winner))
            GameStatus.DRAW -> getString(R.string.status_draw)
        }
    }

    private fun symbolText(player: Player?): String = when (player) {
        Player.X -> getString(R.string.symbol_x)
        Player.O -> getString(R.string.symbol_o)
        null -> ""
    }
}
