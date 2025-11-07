package com.words.presentation

import com.words.domain.model.*
import kotlinx.serialization.Serializable

/**
 * Represents the complete state of the game.
 *
 * @property word The target word to guess (kept in state for validation)
 * @property numberOfLetters Length of words in current game
 * @property attempts Current attempt number (0-5, where 0 is first attempt)
 * @property selectedLetter Currently selected letter coordinate
 * @property letters Map of all letters in the grid
 * @property wordList Current word list being used
 * @property currentPage Current screen/page being displayed
 * @property gameWon Whether the game has been won
 * @property gameOver Whether the game is over (won or lost)
 * @property keyboardState State of keyboard keys (for color feedback)
 * @property statistics Game statistics
 * @property availableWordLists Names of available word lists
 * @property showInvalidWordAnimation Whether to show invalid word shake animation
 */
@Serializable
data class GameState(
    val word: String = "",
    val numberOfLetters: Int = 5,
    val attempts: Int = 0,
    val selectedLetter: Coord = Coord(0, 0),
    val letters: Map<Coord, Letter> = emptyMap(),
    val wordList: WordList = WordList.empty(),
    val currentPage: GamePage = GamePage.Game,
    val gameWon: Boolean = false,
    val gameOver: Boolean = false,
    val keyboardState: Map<Char, KeyFormat> = emptyMap(),
    val statistics: GameStatistics = GameStatistics(),
    val availableWordLists: List<String> = emptyList(),
    val showInvalidWordAnimation: Boolean = false
) {
    init {
        require(numberOfLetters > 0) { "Number of letters must be positive" }
        require(attempts >= 0) { "Attempts cannot be negative" }
        require(attempts <= 6) { "Attempts cannot exceed 6" }
    }

    /**
     * Gets the current row being edited.
     */
    val currentRow: Int
        get() = attempts

    /**
     * Checks if the game is still in progress.
     */
    val isGameInProgress: Boolean
        get() = !gameOver && currentPage == GamePage.Game

    /**
     * Compatibility property for screen code - word list name.
     */
    val wordListName: String
        get() = wordList.name

    /**
     * Compatibility property for screen code - word length.
     */
    val wordLength: Int
        get() = numberOfLetters

    /**
     * Compatibility property for screen code - available word lengths.
     */
    val availableWordLengths: List<Int>
        get() = listOf(4, 5, 6, 7, 8)

    /**
     * Compatibility property for screen code - whether game was won.
     */
    val won: Boolean
        get() = gameWon

    /**
     * Compatibility property for screen code - total games played.
     */
    val gamesPlayed: Int
        get() = statistics.totalGames

    /**
     * Compatibility property for screen code - longest win streak.
     */
    val maxStreak: Int
        get() = statistics.longestStreak

    /**
     * Compatibility property for screen code - guess distribution.
     */
    val guessDistribution: List<Int>
        get() = statistics.winDistribution

    /**
     * Compatibility property for screen code - game grid (letters) as 2D list.
     */
    val grid: List<List<Letter>>
        get() = (0 until 6).map { row ->
            (0 until numberOfLetters).map { col ->
                letters[Coord(col, row)] ?: Letter.empty()
            }
        }

    /**
     * Compatibility property for screen code - keyboard keys layout.
     */
    val keys: List<List<Key>>
        get() = wordList.keyboardLayout

    /**
     * Gets all letters in the current row.
     */
    fun getRowLetters(row: Int): List<Letter> {
        return (0 until numberOfLetters).map { col ->
            letters[Coord(col, row)] ?: Letter.empty()
        }
    }

    /**
     * Gets the word entered in the current row.
     */
    fun getCurrentWord(): String {
        return getRowLetters(currentRow)
            .map { it.value }
            .joinToString("")
    }

    /**
     * Checks if the current row is full.
     */
    fun isCurrentRowFull(): Boolean {
        return getRowLetters(currentRow).all { it.hasLetter }
    }

    /**
     * Checks if the current row is empty.
     */
    fun isCurrentRowEmpty(): Boolean {
        return getRowLetters(currentRow).all { it.isEmpty }
    }

    /**
     * Gets the number of letters entered in the current row.
     */
    fun getCurrentRowLength(): Int {
        return getRowLetters(currentRow).count { it.hasLetter }
    }

    companion object {
        /**
         * Creates an empty grid of letters.
         */
        fun createEmptyGrid(wordLength: Int, maxAttempts: Int = 6): Map<Coord, Letter> {
            return buildMap {
                for (row in 0 until maxAttempts) {
                    for (col in 0 until wordLength) {
                        put(Coord(col, row), Letter.empty())
                    }
                }
            }
        }

        /**
         * Creates initial game state with a word list.
         */
        fun initial(wordList: WordList, wordLength: Int = 5): GameState {
            return GameState(
                word = "",
                numberOfLetters = wordLength,
                attempts = 0,
                selectedLetter = Coord(0, 0),
                letters = createEmptyGrid(wordLength),
                wordList = wordList,
                currentPage = GamePage.Game,
                gameWon = false,
                gameOver = false,
                keyboardState = emptyMap(),
                statistics = GameStatistics(),
                availableWordLists = emptyList(),
                showInvalidWordAnimation = false
            )
        }
    }
}
