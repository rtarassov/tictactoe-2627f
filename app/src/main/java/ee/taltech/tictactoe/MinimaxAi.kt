package ee.taltech.tictactoe

import kotlin.random.Random

class MinimaxAi(
    private val maxDepth: Int,
    private val random: Random = Random.Default,
) {

    fun chooseMove(game: TicTacToeGame): Cell? {
        if (game.status != GameStatus.IN_PROGRESS) return null

        val aiPlayer = game.currentPlayer
        var bestScore = Int.MIN_VALUE
        var bestCell: Cell? = null
        for (cell in game.emptyCells().shuffled(random)) {
            game.makeMove(cell.row, cell.column)
            val score = minimax(game, aiPlayer, depth = 1, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
            game.undoMove()
            if (score > bestScore) {
                bestScore = score
                bestCell = cell
            }
        }
        return bestCell
    }

    private fun minimax(game: TicTacToeGame, aiPlayer: Player, depth: Int, alpha: Int, beta: Int): Int {
        if (game.status == GameStatus.WON) {
            return if (game.winner == aiPlayer) WIN_SCORE - depth else depth - WIN_SCORE
        }
        if (game.status == GameStatus.DRAW || depth >= maxDepth) {
            return DRAW_SCORE
        }

        val isAiTurn = game.currentPlayer == aiPlayer
        var currentAlpha = alpha
        var currentBeta = beta
        var bestScore = if (isAiTurn) Int.MIN_VALUE else Int.MAX_VALUE
        for (cell in game.emptyCells()) {
            game.makeMove(cell.row, cell.column)
            val score = minimax(game, aiPlayer, depth + 1, currentAlpha, currentBeta)
            game.undoMove()

            if (isAiTurn) {
                bestScore = maxOf(bestScore, score)
                currentAlpha = maxOf(currentAlpha, bestScore)
            } else {
                bestScore = minOf(bestScore, score)
                currentBeta = minOf(currentBeta, bestScore)
            }
            if (currentBeta <= currentAlpha) {
                break
            }
        }
        return bestScore
    }

    companion object {
        private const val WIN_SCORE = 1000
        private const val DRAW_SCORE = 0
        private const val FULL_SEARCH_CELL_LIMIT = 9
        private const val MEDIUM_BOARD_CELL_LIMIT = 16
        private const val MEDIUM_BOARD_DEPTH = 6
        private const val LARGE_BOARD_DEPTH = 4

        fun forBoard(game: TicTacToeGame, random: Random = Random.Default): MinimaxAi {
            val depth = when {
                game.cellCount <= FULL_SEARCH_CELL_LIMIT -> game.cellCount
                game.cellCount <= MEDIUM_BOARD_CELL_LIMIT -> MEDIUM_BOARD_DEPTH
                else -> LARGE_BOARD_DEPTH
            }
            return MinimaxAi(depth, random)
        }
    }
}
