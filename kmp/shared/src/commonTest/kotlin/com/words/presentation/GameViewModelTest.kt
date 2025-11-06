package com.words.presentation

import com.words.data.InMemoryStatisticsRepository
import com.words.data.InMemoryWordListRepository
import com.words.domain.game.GameEngine
import com.words.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private lateinit var wordListRepository: InMemoryWordListRepository
    private lateinit var statisticsRepository: InMemoryStatisticsRepository
    private lateinit var viewModel: GameViewModel

    private fun createTestWordList(name: String = "Test"): WordList {
        return WordList(
            name = name,
            secretWords = setOf("HELLO", "WORLD", "TESTS", "GAMES", "WORDS"),
            validWords = setOf("VALID", "GUESS", "WRONG"),
            allowedLetters = ('A'..'Z').toSet(),
            keyboardLayout = listOf(
                listOf(Key.Letter('Q'), Key.Letter('W'), Key.Letter('E'), Key.Letter('R'), Key.Letter('T')),
                listOf(Key.Letter('A'), Key.Letter('S'), Key.Letter('D'), Key.Letter('F'), Key.Letter('G')),
                listOf(Key.Enter, Key.Letter('Z'), Key.Letter('X'), Key.Delete)
            ),
            availableLengths = setOf(5)
        )
    }

    @BeforeTest
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        wordListRepository = InMemoryWordListRepository()
        statisticsRepository = InMemoryStatisticsRepository()
    }

    private fun createViewModel(testScope: CoroutineScope, seed: Long? = null): GameViewModel {
        val gameEngine = if (seed != null) {
            GameEngine(Random(seed))
        } else {
            GameEngine()
        }

        return GameViewModel(
            wordListRepository = wordListRepository,
            statisticsRepository = statisticsRepository,
            gameEngine = gameEngine,
            coroutineScope = testScope
        )
    }

    @Test
    fun testInitialState() = runTest {
        wordListRepository.addWordList(createTestWordList("English"))
        viewModel = createViewModel(this)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.word.isNotEmpty())
        assertEquals(0, state.attempts)
        assertFalse(state.gameOver)
        assertFalse(state.gameWon)
    }

    @Test
    fun testEnterLetter() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.EnterLetter('H'))
        viewModel.processIntent(GameIntent.EnterLetter('E'))
        viewModel.processIntent(GameIntent.EnterLetter('L'))

        val state = viewModel.state.value
        val firstRow = state.getRowLetters(0)

        assertEquals("H", firstRow[0].value)
        assertEquals("E", firstRow[1].value)
        assertEquals("L", firstRow[2].value)
        assertEquals("", firstRow[3].value)
    }

    @Test
    fun testEnterLetterFullRow() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        // Fill the row
        "HELLO".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }

        // Try to enter one more
        viewModel.processIntent(GameIntent.EnterLetter('X'))

        val state = viewModel.state.value
        val firstRow = state.getRowLetters(0)

        // Should still be HELLO, X should not be added
        assertEquals("HELLO", firstRow.joinToString("") { it.value })
    }

    @Test
    fun testBackspace() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.EnterLetter('H'))
        viewModel.processIntent(GameIntent.EnterLetter('E'))
        viewModel.processIntent(GameIntent.EnterLetter('L'))
        viewModel.processIntent(GameIntent.Backspace)

        val state = viewModel.state.value
        val firstRow = state.getRowLetters(0)

        assertEquals("H", firstRow[0].value)
        assertEquals("E", firstRow[1].value)
        assertEquals("", firstRow[2].value)
    }

    @Test
    fun testBackspaceOnEmptyRow() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.Backspace)

        val state = viewModel.state.value
        val firstRow = state.getRowLetters(0)

        assertTrue(firstRow.all { it.isEmpty })
    }

    @Test
    fun testSubmitIncompleteWord() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.EnterLetter('H'))
        viewModel.processIntent(GameIntent.EnterLetter('E'))
        viewModel.processIntent(GameIntent.EnterWord)

        val state = viewModel.state.value

        // Should still be on attempt 0
        assertEquals(0, state.attempts)
    }

    @Test
    fun testSubmitInvalidWord() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        "ZZZZZ".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        val state = viewModel.state.value

        // Should show invalid word animation
        assertTrue(state.showInvalidWordAnimation)
        // Should still be on attempt 0
        assertEquals(0, state.attempts)
    }

    @Test
    fun testSubmitValidWord() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this, seed = 42L)
        advanceUntilIdle()

        // The target word with seed 42 will be deterministic
        // Let's submit a valid word that's not the target
        "VALID".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        val state = viewModel.state.value

        // Should advance to attempt 1
        assertEquals(1, state.attempts)
        // Should have evaluated the first row
        val firstRow = state.getRowLetters(0)
        assertTrue(firstRow.all { it.isEvaluated })
    }

    @Test
    fun testWinGame() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this, seed = 42L)
        advanceUntilIdle()

        val targetWord = viewModel.state.value.word

        // Submit the correct word
        targetWord.forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        advanceUntilIdle()

        val state = viewModel.state.value

        assertTrue(state.gameWon)
        assertTrue(state.gameOver)
        assertEquals(1, state.attempts)
    }

    @Test
    fun testLoseGame() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        // Submit 6 wrong guesses
        repeat(6) {
            "VALID".forEach { char -> viewModel.processIntent(GameIntent.EnterLetter(char)) }
            viewModel.processIntent(GameIntent.EnterWord)
        }

        advanceUntilIdle()

        val state = viewModel.state.value

        assertFalse(state.gameWon)
        assertTrue(state.gameOver)
        assertEquals(6, state.attempts)
    }

    @Test
    fun testKeyboardStateUpdate() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this, seed = 42L)
        advanceUntilIdle()

        // Submit a word
        "VALID".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        val state = viewModel.state.value

        // Keyboard state should be updated with the letters from VALID
        assertTrue(state.keyboardState.isNotEmpty())
        assertTrue(state.keyboardState.containsKey('V'))
        assertTrue(state.keyboardState.containsKey('A'))
    }

    @Test
    fun testStartNewGame() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        val firstWord = viewModel.state.value.word

        // Enter some letters
        "HELLO".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }

        // Start new game
        viewModel.processIntent(GameIntent.StartNewGame)

        val state = viewModel.state.value

        // Word might be different (random)
        assertEquals(0, state.attempts)
        assertFalse(state.gameOver)
        // First row should be empty
        assertTrue(state.getRowLetters(0).all { it.isEmpty })
    }

    @Test
    fun testMoveCursor() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.MoveCursor(2))

        val state = viewModel.state.value
        assertEquals(2, state.selectedLetter.column)
    }

    @Test
    fun testMoveCursorBounds() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        // Try to move beyond bounds
        viewModel.processIntent(GameIntent.MoveCursor(10))

        val state = viewModel.state.value
        // Should be clamped to max column
        assertEquals(4, state.selectedLetter.column)

        // Try to move before bounds
        viewModel.processIntent(GameIntent.MoveCursor(-10))

        val state2 = viewModel.state.value
        // Should be clamped to 0
        assertEquals(0, state2.selectedLetter.column)
    }

    @Test
    fun testShowStatistics() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.ShowStatistics)

        val state = viewModel.state.value
        assertEquals(GamePage.Statistics, state.currentPage)
    }

    @Test
    fun testShowHelp() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.ShowHelp)

        val state = viewModel.state.value
        assertEquals(GamePage.Help, state.currentPage)
    }

    @Test
    fun testReturnToGame() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.ShowHelp)
        viewModel.processIntent(GameIntent.ReturnToGame)

        val state = viewModel.state.value
        assertEquals(GamePage.Game, state.currentPage)
    }

    @Test
    fun testDismissInvalidWordAnimation() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this)
        advanceUntilIdle()

        // Trigger invalid word
        "ZZZZZ".forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        assertTrue(viewModel.state.value.showInvalidWordAnimation)

        viewModel.processIntent(GameIntent.DismissInvalidWordAnimation)

        assertFalse(viewModel.state.value.showInvalidWordAnimation)
    }

    @Test
    fun testStatisticsPersistence() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this, seed = 42L)
        advanceUntilIdle()

        val targetWord = viewModel.state.value.word

        // Win the game
        targetWord.forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        advanceUntilIdle()

        // Check statistics were updated
        val stats = statisticsRepository.loadStatistics("Test", 5)
        assertEquals(1, stats.totalGames)
        assertEquals(1, stats.gamesWon)
    }

    @Test
    fun testSwitchWordList() = runTest {
        wordListRepository.addWordList(createTestWordList("English"))
        wordListRepository.addWordList(createTestWordList("Deutsch"))
        viewModel = createViewModel(this)
        advanceUntilIdle()

        viewModel.processIntent(GameIntent.SwitchWordList("Deutsch"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Deutsch", state.wordList.name)
    }

    @Test
    fun testCannotEnterAfterGameOver() = runTest {
        wordListRepository.addWordList(createTestWordList())
        viewModel = createViewModel(this, seed = 42L)
        advanceUntilIdle()

        val targetWord = viewModel.state.value.word

        // Win the game
        targetWord.forEach { viewModel.processIntent(GameIntent.EnterLetter(it)) }
        viewModel.processIntent(GameIntent.EnterWord)

        advanceUntilIdle()

        // Try to enter more letters
        viewModel.processIntent(GameIntent.EnterLetter('X'))

        val state = viewModel.state.value
        // Second row should still be empty
        assertTrue(state.getRowLetters(1).all { it.isEmpty })
    }
}
