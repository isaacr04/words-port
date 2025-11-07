package com.words.presentation

import com.words.domain.game.GameEngine
import com.words.domain.model.*
import com.words.domain.repository.StatisticsRepository
import com.words.domain.repository.WordListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Words game following the MVI pattern.
 *
 * @property wordListRepository Repository for loading word lists
 * @property statisticsRepository Repository for game statistics
 * @property gameEngine Core game logic engine
 * @property coroutineScope Coroutine scope for launching async operations
 */
class GameViewModel(
    private val wordListRepository: WordListRepository,
    private val statisticsRepository: StatisticsRepository,
    private val gameEngine: GameEngine = GameEngine(),
    private val coroutineScope: CoroutineScope
) {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        // Load initial word list and statistics
        coroutineScope.launch {
            loadInitialState()
        }
    }

    /**
     * Processes user intents and updates state accordingly.
     */
    fun processIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.StartNewGame -> startNewGame()
            is GameIntent.EnterLetter -> enterLetter(intent.char)
            is GameIntent.EnterWord -> submitWord()
            is GameIntent.Backspace -> handleBackspace()
            is GameIntent.MoveCursor -> moveCursor(intent.step)
            is GameIntent.SelectField -> selectField(intent.coord)
            is GameIntent.SetWordLength -> setWordLength(intent.length)
            is GameIntent.SwitchWordList -> switchWordList(intent.name)
            is GameIntent.ShowStatistics -> showStatistics()
            is GameIntent.ShowHelp -> showHelp()
            is GameIntent.ShowSettings -> showSettings()
            is GameIntent.ReturnToGame -> returnToGame()
            is GameIntent.DismissInvalidWordAnimation -> dismissInvalidWordAnimation()
            is GameIntent.LoadStatistics -> loadStatistics()
            is GameIntent.SaveStatistics -> saveStatistics()
            is GameIntent.ClearStatistics -> clearStatistics()
        }
    }

    /**
     * Loads initial state with default word list.
     */
    private suspend fun loadInitialState() {
        val availableLists = wordListRepository.getAvailableWordLists()
        val defaultName = wordListRepository.getDefaultWordListName()
        val wordList = wordListRepository.loadWordList(defaultName) ?: WordList.empty()
        val statistics = statisticsRepository.loadStatistics(wordList.name, 5)

        _state.update { currentState ->
            currentState.copy(
                wordList = wordList,
                availableWordLists = availableLists,
                statistics = statistics,
                numberOfLetters = 5
            )
        }

        // Start first game
        startNewGame()
    }

    /**
     * Starts a new game with the current word list and settings.
     */
    private fun startNewGame() {
        val currentState = state.value
        val wordList = currentState.wordList

        if (wordList.name.isEmpty()) {
            // Word list not loaded yet
            return
        }

        val newWord = try {
            gameEngine.pickRandomWord(wordList, currentState.numberOfLetters)
        } catch (e: Exception) {
            // No words available for this length
            return
        }

        _state.update { current ->
            current.copy(
                word = newWord,
                attempts = 0,
                selectedLetter = Coord(0, 0),
                letters = GameState.createEmptyGrid(current.numberOfLetters),
                currentPage = GamePage.Game,
                gameWon = false,
                gameOver = false,
                keyboardState = emptyMap(),
                showInvalidWordAnimation = false
            )
        }
    }

    /**
     * Enters a letter at the current selected position.
     */
    private fun enterLetter(char: Char) {
        val currentState = state.value

        if (currentState.gameOver || currentState.currentPage != GamePage.Game) {
            return
        }

        val currentRow = currentState.currentRow
        if (currentRow >= GameEngine.MAX_ATTEMPTS) {
            return
        }

        // Check if the letter is allowed in the current word list
        if (!currentState.wordList.allowedLetters.contains(char.uppercaseChar())) {
            return
        }

        // Find the first empty cell in the current row
        val rowLetters = currentState.getRowLetters(currentRow)
        val emptyIndex = rowLetters.indexOfFirst { it.isEmpty }

        if (emptyIndex == -1) {
            // Row is full
            return
        }

        // Update the letter at the found position
        val coord = Coord(emptyIndex, currentRow)
        val newLetter = Letter.withValue(char)

        _state.update { current ->
            current.copy(
                letters = current.letters + (coord to newLetter),
                selectedLetter = coord
            )
        }
    }

    /**
     * Handles backspace - removes the last letter in the current row.
     */
    private fun handleBackspace() {
        val currentState = state.value

        if (currentState.gameOver || currentState.currentPage != GamePage.Game) {
            return
        }

        val currentRow = currentState.currentRow
        val rowLetters = currentState.getRowLetters(currentRow)

        // Find the last non-empty cell in the current row
        val lastFilledIndex = rowLetters.indexOfLast { it.hasLetter }

        if (lastFilledIndex == -1) {
            // Row is already empty
            return
        }

        // Clear the letter at the found position
        val coord = Coord(lastFilledIndex, currentRow)
        val emptyLetter = Letter.empty()

        _state.update { current ->
            current.copy(
                letters = current.letters + (coord to emptyLetter),
                selectedLetter = coord,
                showInvalidWordAnimation = false
            )
        }
    }

    /**
     * Submits the current word for evaluation.
     */
    private fun submitWord() {
        val currentState = state.value

        if (currentState.gameOver || currentState.currentPage != GamePage.Game) {
            return
        }

        val enteredWord = currentState.getCurrentWord()

        // Validate word length
        if (enteredWord.length != currentState.numberOfLetters) {
            return
        }

        // Validate word exists in dictionary
        if (!currentState.wordList.contains(enteredWord)) {
            // Show invalid word animation
            _state.update { it.copy(showInvalidWordAnimation = true) }
            return
        }

        // Evaluate the guess
        val evaluatedLetters = gameEngine.evaluateGuess(currentState.word, enteredWord)
        val currentRow = currentState.currentRow

        // Update letters in the grid with evaluated colors
        val updatedLetters = currentState.letters.toMutableMap()
        evaluatedLetters.forEachIndexed { index, letter ->
            val coord = Coord(index, currentRow)
            updatedLetters[coord] = letter
        }

        // Update keyboard state
        val newKeyboardState = updateKeyboardState(currentState.keyboardState, evaluatedLetters)

        // Check win/loss conditions
        val isWin = gameEngine.isCorrectGuess(currentState.word, enteredWord)
        val newAttempts = currentState.attempts + 1
        val isGameOver = isWin || newAttempts >= GameEngine.MAX_ATTEMPTS

        _state.update { current ->
            current.copy(
                letters = updatedLetters,
                attempts = newAttempts,
                gameWon = isWin,
                gameOver = isGameOver,
                keyboardState = newKeyboardState,
                selectedLetter = if (isGameOver) current.selectedLetter else Coord(0, newAttempts),
                showInvalidWordAnimation = false
            )
        }

        // Update statistics if game is over
        if (isGameOver) {
            coroutineScope.launch {
                updateStatistics(isWin, newAttempts)
            }

            // Show game over page after a delay
            _state.update { it.copy(currentPage = GamePage.GameOver) }
        }
    }

    /**
     * Updates keyboard state based on evaluated letters.
     */
    private fun updateKeyboardState(
        currentState: Map<Char, KeyFormat>,
        evaluatedLetters: List<Letter>
    ): Map<Char, KeyFormat> {
        val newState = currentState.toMutableMap()

        evaluatedLetters.forEach { letter ->
            if (letter.value.isNotEmpty()) {
                val char = letter.value[0]
                val newFormat = KeyFormat.fromLetterFormat(letter.format)
                val currentFormat = newState[char]

                // Only update if the new format is "better" than the current
                // Priority: ExactMatch > Match > NoMatch > Unused
                val shouldUpdate = when {
                    currentFormat == null -> true
                    newFormat == KeyFormat.ExactMatch -> true
                    newFormat == KeyFormat.Match && currentFormat != KeyFormat.ExactMatch -> true
                    newFormat == KeyFormat.NoMatch &&
                            currentFormat != KeyFormat.ExactMatch &&
                            currentFormat != KeyFormat.Match -> true
                    else -> false
                }

                if (shouldUpdate) {
                    newState[char] = newFormat
                }
            }
        }

        return newState
    }

    /**
     * Updates statistics after a game ends.
     */
    private suspend fun updateStatistics(won: Boolean, attempts: Int) {
        val currentState = state.value
        val updatedStats = currentState.statistics.updateWithOutcome(won, attempts)

        statisticsRepository.saveStatistics(
            currentState.wordList.name,
            currentState.numberOfLetters,
            updatedStats
        )

        _state.update { it.copy(statistics = updatedStats) }
    }

    /**
     * Moves the cursor by a given step.
     */
    private fun moveCursor(step: Int) {
        val currentState = state.value

        if (currentState.gameOver) {
            return
        }

        val currentRow = currentState.currentRow
        val newColumn = (currentState.selectedLetter.column + step)
            .coerceIn(0, currentState.numberOfLetters - 1)

        _state.update { it.copy(selectedLetter = Coord(newColumn, currentRow)) }
    }

    /**
     * Selects a specific field in the grid.
     */
    private fun selectField(coord: Coord) {
        val currentState = state.value

        if (currentState.gameOver) {
            return
        }

        // Only allow selecting in the current row
        if (coord.row != currentState.currentRow) {
            return
        }

        _state.update { it.copy(selectedLetter = coord) }
    }

    /**
     * Sets the word length and starts a new game.
     */
    private fun setWordLength(length: Int) {
        val currentState = state.value

        if (!currentState.wordList.availableLengths.contains(length)) {
            return
        }

        coroutineScope.launch {
            val statistics = statisticsRepository.loadStatistics(currentState.wordList.name, length)

            _state.update { it.copy(numberOfLetters = length, statistics = statistics) }
            startNewGame()
        }
    }

    /**
     * Switches to a different word list.
     */
    private fun switchWordList(name: String) {
        coroutineScope.launch {
            val wordList = wordListRepository.loadWordList(name) ?: return@launch
            val statistics = statisticsRepository.loadStatistics(name, state.value.numberOfLetters)

            _state.update { it.copy(wordList = wordList, statistics = statistics) }
            startNewGame()
        }
    }

    /**
     * Shows the statistics screen.
     */
    private fun showStatistics() {
        _state.update { it.copy(currentPage = GamePage.Statistics) }
    }

    /**
     * Shows the help screen.
     */
    private fun showHelp() {
        _state.update { it.copy(currentPage = GamePage.Help) }
    }

    /**
     * Shows the settings screen.
     */
    private fun showSettings() {
        _state.update { it.copy(currentPage = GamePage.Settings) }
    }

    /**
     * Returns to the game screen.
     */
    private fun returnToGame() {
        _state.update { it.copy(currentPage = GamePage.Game) }
    }

    /**
     * Dismisses the invalid word animation.
     */
    private fun dismissInvalidWordAnimation() {
        _state.update { it.copy(showInvalidWordAnimation = false) }
    }

    /**
     * Loads statistics from repository.
     */
    private fun loadStatistics() {
        coroutineScope.launch {
            val currentState = state.value
            val statistics = statisticsRepository.loadStatistics(
                currentState.wordList.name,
                currentState.numberOfLetters
            )
            _state.update { it.copy(statistics = statistics) }
        }
    }

    /**
     * Saves current statistics to repository.
     */
    private fun saveStatistics() {
        coroutineScope.launch {
            val currentState = state.value
            statisticsRepository.saveStatistics(
                currentState.wordList.name,
                currentState.numberOfLetters,
                currentState.statistics
            )
        }
    }

    /**
     * Clears all statistics.
     */
    private fun clearStatistics() {
        _state.update { it.copy(statistics = GameStatistics()) }
        saveStatistics()
    }
}
