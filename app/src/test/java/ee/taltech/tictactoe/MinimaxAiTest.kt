package ee.taltech.tictactoe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class MinimaxAiTest {

    @Test
    fun chooseMove_takesAnImmediateWin() {
        val game = TicTacToeGame.fromMoves(3, 3, 3, Player.X, listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(2, 2)))
        val ai = MinimaxAi.forBoard(game, Random(1))

        val chosenCell = ai.chooseMove(game)

        assertEquals(Cell(1, 2), chosenCell)
    }

    @Test
    fun chooseMove_blocksTheOpponentsThreat() {
        val game = TicTacToeGame.fromMoves(3, 3, 3, Player.X, listOf(Cell(0, 0), Cell(1, 1), Cell(0, 1)))
        val ai = MinimaxAi.forBoard(game, Random(1))

        val chosenCell = ai.chooseMove(game)

        assertEquals(Cell(0, 2), chosenCell)
    }

    @Test
    fun chooseMove_doesNotChangeTheGame() {
        val game = TicTacToeGame.fromMoves(3, 3, 3, Player.X, listOf(Cell(0, 0)))
        val boardBefore = game.toBoardString()

        MinimaxAi.forBoard(game, Random(1)).chooseMove(game)

        assertEquals(boardBefore, game.toBoardString())
        assertEquals(1, game.moveCount)
        assertEquals(Player.O, game.currentPlayer)
    }

    @Test
    fun chooseMove_whenGameOver_returnsNull() {
        val game = TicTacToeGame.fromMoves(3, 3, 3, Player.X, listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1), Cell(0, 2)))

        assertNull(MinimaxAi.forBoard(game, Random(1)).chooseMove(game))
    }

    @Test
    fun minimaxPlayer_neverLosesAgainstRandomOn3x3() {
        val random = Random(2026)
        repeat(40) { gameNumber ->
            val game = TicTacToeGame(startingPlayer = if (gameNumber % 2 == 0) Player.X else Player.O, random = random)
            val ai = MinimaxAi.forBoard(game, random)
            while (game.status == GameStatus.IN_PROGRESS) {
                if (game.currentPlayer == Player.O) {
                    val cell = ai.chooseMove(game)!!
                    game.makeMove(cell.row, cell.column)
                } else {
                    game.makeRandomMove()
                }
            }
            assertNotEquals("game $gameNumber: ${game.toBoardString()}", Player.X, game.winner)
        }
    }

    @Test
    fun chooseMove_onLargeBoard_findsImmediateWin() {
        val game = TicTacToeGame.fromMoves(
            7, 7, 4, Player.X,
            listOf(Cell(0, 0), Cell(3, 3), Cell(0, 1), Cell(3, 4), Cell(0, 2), Cell(3, 5), Cell(3, 2)),
        )

        val chosenCell = MinimaxAi.forBoard(game, Random(1)).chooseMove(game)

        assertEquals(Cell(3, 6), chosenCell)
    }
}
