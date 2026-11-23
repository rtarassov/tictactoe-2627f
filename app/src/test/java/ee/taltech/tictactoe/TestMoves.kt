package ee.taltech.tictactoe

import kotlin.random.Random

fun playRandomMove(game: TicTacToeGame, random: Random) {
    val cell = game.emptyCells().random(random)
    game.makeMove(cell.row, cell.column)
}
