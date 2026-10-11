package ee.taltech.tictactoe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var switchVsAi: SwitchCompat
    private lateinit var radioGroupStarter: RadioGroup
    private lateinit var textViewStatus: TextView
    private lateinit var chronometerGameTime: Chronometer
    private lateinit var cellButtons: List<Button>
    private lateinit var statisticsStorage: StatisticsStorage

    private var game = TicTacToeGame()
    private var isVsAi = false
    private var gameStartElapsedRealtime = 0L
    private var finishedGameDurationMs = 0L
    private var isNewGameDialogShowing = false
    private var newGameDialog: AlertDialog? = null
    private val aiMoveHandler = Handler(Looper.getMainLooper())

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
        chronometerGameTime = findViewById(R.id.chronometerGameTime)
        statisticsStorage = StatisticsStorage(this)
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

        if (savedInstanceState != null) {
            restoreGame(savedInstanceState)
        } else {
            startNewGame()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_BOARD, game.toBoardString())
        outState.putString(KEY_CURRENT_PLAYER, game.currentPlayer.name)
        outState.putBoolean(KEY_IS_VS_AI, isVsAi)
        outState.putLong(KEY_GAME_START, gameStartElapsedRealtime)
        outState.putLong(KEY_FINISHED_DURATION, finishedGameDurationMs)
        outState.putBoolean(KEY_DIALOG_SHOWING, isNewGameDialogShowing)
    }

    override fun onDestroy() {
        super.onDestroy()
        newGameDialog?.dismiss()
        aiMoveHandler.removeCallbacksAndMessages(null)
    }

    fun buttonCellClicked(view: View) {
        if (isAiTurn()) return

        val cellIndex = cellButtons.indexOf(view)
        val row = cellIndex / TicTacToeGame.BOARD_SIZE
        val column = cellIndex % TicTacToeGame.BOARD_SIZE
        val moveWasMade = game.makeMove(row, column)
        if (!moveWasMade) return

        onMoveMade()
        if (isAiTurn()) {
            scheduleAiMove()
        }
    }

    fun buttonNewGameClicked(view: View) {
        if (game.status == GameStatus.IN_PROGRESS && game.moveCount > 0) {
            showNewGameDialog()
        } else {
            startNewGame()
        }
    }

    fun buttonStatisticsClicked(view: View) {
        startActivity(Intent(this, StatsActivity::class.java))
    }

    private fun startNewGame() {
        aiMoveHandler.removeCallbacksAndMessages(null)
        isVsAi = switchVsAi.isChecked
        val startingPlayer = if (radioGroupStarter.checkedRadioButtonId == R.id.radioButtonStarterO) Player.O else Player.X
        game = TicTacToeGame(startingPlayer)
        gameStartElapsedRealtime = 0L
        finishedGameDurationMs = 0L
        renderGame()

        if (isAiTurn()) {
            scheduleAiMove()
        }
    }

    private fun isAiTurn(): Boolean =
        isVsAi && game.status == GameStatus.IN_PROGRESS && game.currentPlayer == Player.O

    private fun scheduleAiMove() {
        aiMoveHandler.postDelayed({ makeAiMove() }, AI_MOVE_DELAY_MS)
    }

    private fun makeAiMove() {
        game.makeRandomMove()
        onMoveMade()
    }

    private fun onMoveMade() {
        if (gameStartElapsedRealtime == 0L) {
            gameStartElapsedRealtime = SystemClock.elapsedRealtime()
        }
        if (game.status != GameStatus.IN_PROGRESS) {
            finishedGameDurationMs = SystemClock.elapsedRealtime() - gameStartElapsedRealtime
            statisticsStorage.recordGame(game.winner, isVsAi, finishedGameDurationMs)
        }
        renderGame()
    }

    private fun restoreGame(savedInstanceState: Bundle) {
        val boardString = savedInstanceState.getString(KEY_BOARD, EMPTY_BOARD)
        val currentPlayer = Player.valueOf(savedInstanceState.getString(KEY_CURRENT_PLAYER, Player.X.name))
        game = TicTacToeGame.fromBoardString(boardString, currentPlayer)
        isVsAi = savedInstanceState.getBoolean(KEY_IS_VS_AI)
        gameStartElapsedRealtime = savedInstanceState.getLong(KEY_GAME_START)
        finishedGameDurationMs = savedInstanceState.getLong(KEY_FINISHED_DURATION)
        renderGame()

        if (isAiTurn()) {
            scheduleAiMove()
        }
        if (savedInstanceState.getBoolean(KEY_DIALOG_SHOWING)) {
            showNewGameDialog()
        }
    }

    private fun showNewGameDialog() {
        isNewGameDialogShowing = true
        newGameDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_game_dialog_title)
            .setMessage(R.string.new_game_dialog_message)
            .setPositiveButton(R.string.new_game_dialog_confirm) { _, _ -> startNewGame() }
            .setNegativeButton(R.string.new_game_dialog_cancel, null)
            .setOnDismissListener { isNewGameDialogShowing = false }
            .show()
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
        renderGameTime()
    }

    private fun renderGameTime() {
        if (gameStartElapsedRealtime == 0L) {
            chronometerGameTime.base = SystemClock.elapsedRealtime()
            chronometerGameTime.stop()
        } else if (game.status == GameStatus.IN_PROGRESS) {
            chronometerGameTime.base = gameStartElapsedRealtime
            chronometerGameTime.start()
        } else {
            chronometerGameTime.base = SystemClock.elapsedRealtime() - finishedGameDurationMs
            chronometerGameTime.stop()
        }
    }

    private fun symbolText(player: Player?): String = when (player) {
        Player.X -> getString(R.string.symbol_x)
        Player.O -> getString(R.string.symbol_o)
        null -> ""
    }

    companion object {
        const val AI_MOVE_DELAY_MS = 400L
        private const val EMPTY_BOARD = "........."
        private const val KEY_BOARD = "board"
        private const val KEY_CURRENT_PLAYER = "currentPlayer"
        private const val KEY_IS_VS_AI = "isVsAi"
        private const val KEY_GAME_START = "gameStartElapsedRealtime"
        private const val KEY_FINISHED_DURATION = "finishedGameDurationMs"
        private const val KEY_DIALOG_SHOWING = "isNewGameDialogShowing"
    }
}
