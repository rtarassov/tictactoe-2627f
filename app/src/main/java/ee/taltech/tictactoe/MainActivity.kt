package ee.taltech.tictactoe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.GridLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // lateinit = väärtus antakse hiljem (onCreate'is findViewById-ga). Muutuja EI OLE null: tüüp on SwitchCompat, mitte SwitchCompat?.
    // Kui seda kasutada enne väärtuse andmist, tuleb UninitializedPropertyAccessException (mitte NullPointerException).
    private lateinit var switchVsAi: SwitchCompat
    private lateinit var radioGroupStarter: RadioGroup
    private lateinit var textViewStatus: TextView
    private lateinit var chronometerGameTime: Chronometer
    private lateinit var buttonUndo: Button
    private lateinit var gridLayoutBoard: GridLayout
    private lateinit var statisticsStorage: StatisticsStorage
    private val cellButtons = mutableListOf<Button>()

    private var game = TicTacToeGame()
    private var isVsAi = false
    private var nextRowCount = TicTacToeGame.DEFAULT_BOARD_SIZE
    private var nextColumnCount = TicTacToeGame.DEFAULT_BOARD_SIZE
    private var nextWinLength = TicTacToeGame.DEFAULT_BOARD_SIZE
    private var boardRotationDegrees = 0f
    private var gameStartElapsedRealtime = 0L
    private var finishedGameDurationMs = 0L
    private var isNewGameDialogShowing = false
    private var newGameDialog: AlertDialog? = null
    private var boardSettingsDialog: AlertDialog? = null
    // Handler, mis jooksutab koodi hiljem samal UI (main) lõimel. Vajalik AI käigu 400 ms viivituse jaoks.
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
        buttonUndo = findViewById(R.id.buttonUndo)
        gridLayoutBoard = findViewById(R.id.gridLayoutBoard)
        statisticsStorage = StatisticsStorage(this)

        // savedInstanceState on null, kui äpp käivitati esimest korda. Pööramise järel on seal onSaveInstanceState'is salvestatud seis.
        if (savedInstanceState != null) {
            restoreGame(savedInstanceState)
        } else {
            startNewGame()
        }
    }

    // Kutsutakse enne, kui Activity hävitatakse (nt pööramisel). Bundle'isse panna ainult väikesed andmed.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ROW_COUNT, game.rowCount)
        outState.putInt(KEY_COLUMN_COUNT, game.columnCount)
        outState.putInt(KEY_WIN_LENGTH, game.winLength)
        outState.putString(KEY_STARTING_PLAYER, game.startingPlayer.name)
        // Laua asemel salvestame käikude ajaloo (lahtrite indeksid). Taastamisel mängitakse käigud uuesti läbi,
        // nii arvutatakse võitja ise uuesti ja undo ajalugu jääb alles.
        outState.putIntArray(KEY_MOVES, game.moveHistory.map { cell -> cellIndexOf(cell) }.toIntArray())
        outState.putBoolean(KEY_IS_VS_AI, isVsAi)
        outState.putInt(KEY_NEXT_ROW_COUNT, nextRowCount)
        outState.putInt(KEY_NEXT_COLUMN_COUNT, nextColumnCount)
        outState.putInt(KEY_NEXT_WIN_LENGTH, nextWinLength)
        outState.putFloat(KEY_BOARD_ROTATION, boardRotationDegrees)
        outState.putLong(KEY_GAME_START, gameStartElapsedRealtime)
        outState.putLong(KEY_FINISHED_DURATION, finishedGameDurationMs)
        outState.putBoolean(KEY_DIALOG_SHOWING, isNewGameDialogShowing)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dialoogid peavad koos Activity'ga sulgema, muidu tuleb "window leaked" viga.
        newGameDialog?.dismiss()
        boardSettingsDialog?.dismiss()
        // Tühistab ootel AI käigu, et see ei jookseks juba hävitatud Activity peal. Uus Activity planeerib selle restoreGame'is uuesti.
        aiMoveHandler.removeCallbacksAndMessages(null)
    }

    // XML-i android:onClick kutsub selle välja. Nõuded: avalik meetod selles Activity's, täpselt üks View parameeter,
    // nimi täht-tähelt sama mis XML-is. Vea korral crash alles nupuvajutusel, mitte kompileerimisel.
    fun buttonCellClicked(view: View) {
        // Kui AI "mõtleb" (400 ms ootel), ignoreerime puudutusi, muidu saaks inimene teha AI eest käigu.
        if (isAiTurn()) return

        val cell = cellOf(cellButtons.indexOf(view))
        val moveWasMade = game.makeMove(cell.row, cell.column)
        if (!moveWasMade) return

        // onMoveMade kutsutakse ALATI pärast õnnestunud käiku, ka siis kui mäng just lõppes: seal salvestatakse statistika.
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

    fun buttonUndoClicked(view: View) {
        // Lõppenud mängus undo ei tööta: tulemus on juba statistikas ja muidu saaks sama mängu kaks korda kirja panna.
        if (game.status != GameStatus.IN_PROGRESS || game.moveCount == 0) return

        aiMoveHandler.removeCallbacksAndMessages(null)
        game.undoMove()
        // AI vastu: kui pärast tagasivõtmist on AI kord, võtame tagasi ka inimese käigu. Muidu teeks AI 400 ms pärast
        // lihtsalt uue käigu ja undo näiks nagu ei teeks midagi. Inimene tahab tagasi võtta OMA käigu.
        if (isVsAi && game.currentPlayer == Player.O) {
            game.undoMove()
        }
        if (game.moveCount == 0) {
            gameStartElapsedRealtime = 0L
        }
        renderGame()
        if (isAiTurn()) {
            scheduleAiMove()
        }
    }

    fun buttonRotateBoardClicked(view: View) {
        // Pöörame ainult laua vaadet, mitte Activity't, seega Activity't ei looda uuesti.
        // rotation(siht) animeerib täpse sihtnurgani; rotationBy(90f) lisaks 90 poolelioleva animatsiooni nurgale.
        // Puudutused lähevad ikka õigesse lahtrisse, sest Android arvestab vaate pöörde ise ümber.
        boardRotationDegrees += QUARTER_TURN_DEGREES
        gridLayoutBoard.animate().rotation(boardRotationDegrees)
    }

    fun buttonBoardSettingsClicked(view: View) {
        showBoardSettingsDialog()
    }

    fun buttonStatisticsClicked(view: View) {
        // Explicit intent: ütleme täpselt, millist klassi avada. StatsActivity läheb back stackis MainActivity peale.
        startActivity(Intent(this, StatsActivity::class.java))
    }

    private fun startNewGame() {
        aiMoveHandler.removeCallbacksAndMessages(null)
        isVsAi = switchVsAi.isChecked
        val startingPlayer = if (radioGroupStarter.checkedRadioButtonId == R.id.radioButtonStarterO) Player.O else Player.X
        game = TicTacToeGame(nextRowCount, nextColumnCount, nextWinLength, startingPlayer)
        gameStartElapsedRealtime = 0L
        finishedGameDurationMs = 0L
        buildBoard()
        renderGame()

        if (isAiTurn()) {
            scheduleAiMove()
        }
    }

    private fun isAiTurn(): Boolean =
        isVsAi && game.status == GameStatus.IN_PROGRESS && game.currentPlayer == Player.O

    // postDelayed = "jooksuta see 400 ms pärast samal UI lõimel". Vahepeal on lõim vaba ja ekraanile joonistatakse X.
    // Thread.sleep(400) blokeeriks UI lõime: X ja O ilmuksid koos ja äpp oleks külmunud (~5 s pärast ANR).
    private fun scheduleAiMove() {
        aiMoveHandler.postDelayed({ makeAiMove() }, AI_MOVE_DELAY_MS)
    }

    private fun makeAiMove() {
        val bestCell = MinimaxAi.forBoard(game).chooseMove(game)
        if (bestCell != null) {
            game.makeMove(bestCell.row, bestCell.column)
        }
        onMoveMade()
    }

    private fun onMoveMade() {
        // elapsedRealtime = millisekundid alates telefoni käivitamisest, MITTE kellaaeg. Ei hüppa, kui kasutaja muudab
        // kellaaega või ajavööndit, seega sobib kestuse mõõtmiseks. Chronometer ootab ka just seda kella.
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
        nextRowCount = savedInstanceState.getInt(KEY_NEXT_ROW_COUNT, TicTacToeGame.DEFAULT_BOARD_SIZE)
        nextColumnCount = savedInstanceState.getInt(KEY_NEXT_COLUMN_COUNT, TicTacToeGame.DEFAULT_BOARD_SIZE)
        nextWinLength = savedInstanceState.getInt(KEY_NEXT_WIN_LENGTH, TicTacToeGame.DEFAULT_BOARD_SIZE)
        isVsAi = savedInstanceState.getBoolean(KEY_IS_VS_AI)
        boardRotationDegrees = savedInstanceState.getFloat(KEY_BOARD_ROTATION)
        gameStartElapsedRealtime = savedInstanceState.getLong(KEY_GAME_START)
        finishedGameDurationMs = savedInstanceState.getLong(KEY_FINISHED_DURATION)

        val rowCount = savedInstanceState.getInt(KEY_ROW_COUNT, TicTacToeGame.DEFAULT_BOARD_SIZE)
        val columnCount = savedInstanceState.getInt(KEY_COLUMN_COUNT, TicTacToeGame.DEFAULT_BOARD_SIZE)
        val winLength = savedInstanceState.getInt(KEY_WIN_LENGTH, TicTacToeGame.DEFAULT_BOARD_SIZE)
        val startingPlayer = Player.valueOf(savedInstanceState.getString(KEY_STARTING_PLAYER, Player.X.name))
        val moveIndexes = savedInstanceState.getIntArray(KEY_MOVES) ?: IntArray(0)
        val moves = moveIndexes.map { index -> Cell(index / columnCount, index % columnCount) }
        // Mängib salvestatud käigud uuesti läbi: laud, kelle käik, võitja ja võidulahtrid tulevad ise õigesti tagasi.
        game = TicTacToeGame.fromMoves(rowCount, columnCount, winLength, startingPlayer, moves)

        buildBoard()
        gridLayoutBoard.rotation = boardRotationDegrees
        renderGame()

        // Kui pöörati ajal, mil AI "mõtles", planeerime AI käigu uuesti (vana tühistati onDestroy's).
        if (isAiTurn()) {
            scheduleAiMove()
        }
        // Kui dialoog oli enne pööramist lahti (väärtus true), näitame seda uuesti.
        if (savedInstanceState.getBoolean(KEY_DIALOG_SHOWING)) {
            showNewGameDialog()
        }
    }

    private fun buildBoard() {
        gridLayoutBoard.removeAllViews()
        cellButtons.clear()
        gridLayoutBoard.rowCount = game.rowCount
        gridLayoutBoard.columnCount = game.columnCount

        val boardLayoutParams = gridLayoutBoard.layoutParams as ConstraintLayout.LayoutParams
        boardLayoutParams.dimensionRatio = "${game.columnCount}:${game.rowCount}"
        gridLayoutBoard.layoutParams = boardLayoutParams

        val gap = resources.getDimensionPixelSize(R.dimen.board_gap)
        val largestSide = maxOf(game.rowCount, game.columnCount)
        val textSizePx = resources.getDimension(R.dimen.cell_text_size) * TicTacToeGame.DEFAULT_BOARD_SIZE / largestSide
        // Kaks tsüklit: välimine käib read läbi, sisemine veerud. Nii tehakse lahtrid rea kaupa vasakult paremale.
        for (row in 0 until game.rowCount) {
            for (column in 0 until game.columnCount) {
                // Teeb cell_button.xml mallist uue nupu-objekti. "false" = ära lisa veel lauale, lisame allpool ise.
                val cellButton = layoutInflater.inflate(R.layout.cell_button, gridLayoutBoard, false) as Button
                // Silt "cell_rida_veerg", mille järgi Espresso testid õige lahtri üles leiavad.
                cellButton.tag = cellTag(row, column)
                // Mida suurem laud, seda väiksem X/O tekst, et sümbol lahtrisse ära mahuks.
                cellButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                // Ütleb GridLayoutile, millisesse ritta ja veergu nupp läheb. Kaal 1f = kõik read ja veerud saavad võrdse osa ruumist.
                val cellLayoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(column, 1f)
                )
                // 0 + kaal tähendab: "ära kasuta oma suurust, võta oma võrdne osa ruumist" (nagu 0dp + layout_weight).
                cellLayoutParams.width = 0
                cellLayoutParams.height = 0
                // Vahe ainult lahtri üla- ja vasakusse serva, mitte esimeses reas/veerus. Vahedest paistab läbi
                // GridLayouti taustavärv ja need ongi laua jooned. Välisservas vahet pole, seega raami ei teki.
                cellLayoutParams.topMargin = if (row == 0) 0 else gap
                cellLayoutParams.leftMargin = if (column == 0) 0 else gap
                // Paneb nupu lauale nende asukoha- ja suurusereeglitega.
                gridLayoutBoard.addView(cellButton, cellLayoutParams)
                // Jätame nupu listi meelde. Listi järjekord = lahtrite järjekord, mille järgi
                // cellOf(indeks) arvutab hiljem rea (indeks / veerud) ja veeru (indeks % veerud).
                cellButtons.add(cellButton)
                // Näide 3 veeruga laual (number = indeks listis):
                //   rida 0:  0  1  2
                //   rida 1:  3  4  5
                //   rida 2:  6  7  8
                // Indeks 7: rida = 7 / 3 = 2 (mitu täisrida on enne), veerg = 7 % 3 = 1 (palju üle jääb).
            }
        }
    }

    private fun showNewGameDialog() {
        isNewGameDialogShowing = true
        newGameDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_game_dialog_title)
            .setMessage(R.string.new_game_dialog_message)
            .setPositiveButton(R.string.new_game_dialog_confirm) { _, _ -> startNewGame() }
            .setNegativeButton(R.string.new_game_dialog_cancel, null)
            // Käib iga sulgemise peale (New game, Cancel või väljaspool dialoogi vajutus), et lipp ei jääks valeks.
            .setOnDismissListener { isNewGameDialogShowing = false }
            .show()
    }

    private fun showBoardSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_board_settings, null)
        val numberPickerRows = dialogView.findViewById<NumberPicker>(R.id.numberPickerRows)
        val numberPickerColumns = dialogView.findViewById<NumberPicker>(R.id.numberPickerColumns)
        val numberPickerWinLength = dialogView.findViewById<NumberPicker>(R.id.numberPickerWinLength)

        setupNumberPicker(numberPickerRows, TicTacToeGame.MIN_BOARD_SIZE, TicTacToeGame.MAX_BOARD_SIZE, nextRowCount)
        setupNumberPicker(numberPickerColumns, TicTacToeGame.MIN_BOARD_SIZE, TicTacToeGame.MAX_BOARD_SIZE, nextColumnCount)
        setupNumberPicker(numberPickerWinLength, TicTacToeGame.MIN_WIN_LENGTH, TicTacToeGame.MAX_BOARD_SIZE, nextWinLength)

        boardSettingsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.board_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.board_settings_apply) { _, _ ->
                nextRowCount = numberPickerRows.value
                nextColumnCount = numberPickerColumns.value
                nextWinLength = numberPickerWinLength.value.coerceAtMost(maxOf(nextRowCount, nextColumnCount))
                if (game.moveCount == 0) {
                    startNewGame()
                }
            }
            .setNegativeButton(R.string.board_settings_cancel, null)
            .show()
    }

    private fun setupNumberPicker(numberPicker: NumberPicker, minimum: Int, maximum: Int, currentValue: Int) {
        numberPicker.minValue = minimum
        numberPicker.maxValue = maximum
        numberPicker.value = currentValue
        numberPicker.wrapSelectorWheel = false
    }

    private fun renderGame() {
        for (cellIndex in cellButtons.indices) {
            val cell = cellOf(cellIndex)
            val cellButton = cellButtons[cellIndex]
            cellButton.text = symbolText(game.board[cell.row][cell.column])
            val isWinningCell = cell in game.winningCells
            val backgroundColorId = if (isWinningCell) R.color.cell_winning else R.color.cell_background
            cellButton.setBackgroundColor(ContextCompat.getColor(this, backgroundColorId))
        }
        textViewStatus.text = when (game.status) {
            GameStatus.IN_PROGRESS -> getString(R.string.status_turn, symbolText(game.currentPlayer))
            GameStatus.WON -> getString(R.string.status_winner, symbolText(game.winner))
            GameStatus.DRAW -> getString(R.string.status_draw)
        }
        buttonUndo.isEnabled = game.status == GameStatus.IN_PROGRESS && game.moveCount > 0
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

    private fun cellOf(cellIndex: Int): Cell = Cell(cellIndex / game.columnCount, cellIndex % game.columnCount)

    private fun cellIndexOf(cell: Cell): Int = cell.row * game.columnCount + cell.column

    private fun symbolText(player: Player?): String = when (player) {
        Player.X -> getString(R.string.symbol_x)
        Player.O -> getString(R.string.symbol_o)
        null -> ""
    }

    companion object {
        const val AI_MOVE_DELAY_MS = 400L
        private const val QUARTER_TURN_DEGREES = 90f
        private const val KEY_ROW_COUNT = "rowCount"
        private const val KEY_COLUMN_COUNT = "columnCount"
        private const val KEY_WIN_LENGTH = "winLength"
        private const val KEY_STARTING_PLAYER = "startingPlayer"
        private const val KEY_MOVES = "moves"
        private const val KEY_IS_VS_AI = "isVsAi"
        private const val KEY_NEXT_ROW_COUNT = "nextRowCount"
        private const val KEY_NEXT_COLUMN_COUNT = "nextColumnCount"
        private const val KEY_NEXT_WIN_LENGTH = "nextWinLength"
        private const val KEY_BOARD_ROTATION = "boardRotationDegrees"
        private const val KEY_GAME_START = "gameStartElapsedRealtime"
        private const val KEY_FINISHED_DURATION = "finishedGameDurationMs"
        private const val KEY_DIALOG_SHOWING = "isNewGameDialogShowing"

        fun cellTag(row: Int, column: Int): String = "cell_${row}_$column"
    }
}
