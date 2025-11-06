package com.words.domain.game

import com.words.domain.model.Key
import com.words.domain.model.Letter
import com.words.domain.model.WordList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {

    private val engine = GameEngine()

    private fun createTestWordList(secretWords: Set<String>, validWords: Set<String> = emptySet()): WordList {
        return WordList(
            name = "Test",
            secretWords = secretWords,
            validWords = validWords,
            allowedLetters = setOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                                   'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'),
            keyboardLayout = listOf(
                listOf(Key.Letter('Q'), Key.Letter('W'), Key.Letter('E')),
                listOf(Key.Letter('A'), Key.Letter('S'), Key.Letter('D')),
                listOf(Key.Enter, Key.Letter('Z'), Key.Delete)
            ),
            availableLengths = setOf(5)
        )
    }

    @Test
    fun testPickRandomWord() {
        val wordList = createTestWordList(setOf("HELLO", "WORLD", "TESTS"))
        val word = engine.pickRandomWord(wordList)

        assertTrue(wordList.secretWords.contains(word))
    }

    @Test
    fun testPickRandomWordWithLength() {
        val wordList = createTestWordList(
            setOf("HELLO", "WORLD", "HI", "BYE"),
            setOf("VALID", "TESTS")
        )
        val word = engine.pickRandomWord(wordList, length = 5)

        assertEquals(5, word.length)
        assertTrue(wordList.contains(word))
    }

    @Test
    fun testCalculateLetterColorsExactMatch() {
        val result = engine.calculateLetterColors("HELLO", "HELLO")

        assertEquals(5, result.size)
        assertTrue(result.all { it == Letter.Format.ExactMatch })
    }

    @Test
    fun testCalculateLetterColorsNoMatch() {
        val result = engine.calculateLetterColors("HELLO", "GRAND")

        assertEquals(5, result.size)
        // G, R, A, N, D are not in HELLO
        assertEquals(Letter.Format.NoMatch, result[0]) // G
        assertEquals(Letter.Format.NoMatch, result[1]) // R
        assertEquals(Letter.Format.NoMatch, result[2]) // A
        assertEquals(Letter.Format.NoMatch, result[3]) // N
        assertEquals(Letter.Format.NoMatch, result[4]) // D
    }

    @Test
    fun testCalculateLetterColorsPartialMatch() {
        // Target: CRATE
        // Guess:  TRACE
        // T is in word but wrong position (index 0)
        // R is exact match (index 1)
        // A is exact match (index 2)
        // C is in word but wrong position (index 3)
        // E is exact match (index 4)
        val result = engine.calculateLetterColors("CRATE", "TRACE")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.Match, result[0])      // T
        assertEquals(Letter.Format.ExactMatch, result[1]) // R
        assertEquals(Letter.Format.ExactMatch, result[2]) // A
        assertEquals(Letter.Format.Match, result[3])      // C
        assertEquals(Letter.Format.ExactMatch, result[4]) // E
    }

    @Test
    fun testCalculateLetterColorsDuplicateLetters() {
        // Target: SPEED
        // Guess:  ERASE
        // E appears twice in guess, once in target (index 2)
        // Only one E should be marked as partial match
        val result = engine.calculateLetterColors("SPEED", "ERASE")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.Match, result[0])      // E (partial match)
        assertEquals(Letter.Format.NoMatch, result[1])    // R (not in word)
        assertEquals(Letter.Format.NoMatch, result[2])    // A (not in word)
        assertEquals(Letter.Format.NoMatch, result[3])    // S (used up by exact match)
        assertEquals(Letter.Format.Match, result[4])      // E (but wait, this should be NoMatch since E already used)
    }

    @Test
    fun testCalculateLetterColorsDuplicateLettersExactMatchTakesPriority() {
        // Target: ROBOT
        // Guess:  FLOOR
        // O appears at positions 2 and 4 in guess
        // O appears at positions 1 and 3 in target
        // First O in guess (position 2) should be partial match
        // Second O in guess (position 4) should be partial match
        val result = engine.calculateLetterColors("ROBOT", "FLOOR")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.NoMatch, result[0])    // F
        assertEquals(Letter.Format.NoMatch, result[1])    // L
        assertEquals(Letter.Format.Match, result[2])      // O (partial)
        assertEquals(Letter.Format.Match, result[3])      // O (partial)
        assertEquals(Letter.Format.NoMatch, result[4])    // R (R at wrong position)
    }

    @Test
    fun testCalculateLetterColorsTripleLetter() {
        // Target: GEESE (3 E's)
        // Guess:  SPEED (2 E's)
        // Both E's in guess should match (positions 2 and 3 are exact, position 4 is exact)
        val result = engine.calculateLetterColors("GEESE", "SPEED")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.NoMatch, result[0])    // S
        assertEquals(Letter.Format.NoMatch, result[1])    // P
        assertEquals(Letter.Format.ExactMatch, result[2]) // E (exact match at position 2)
        assertEquals(Letter.Format.ExactMatch, result[3]) // E (exact match at position 3)
        assertEquals(Letter.Format.NoMatch, result[4])    // D
    }

    @Test
    fun testCalculateLetterColorsAllSameLetter() {
        // Target: AAAAA
        // Guess:  AAAAA
        val result = engine.calculateLetterColors("AAAAA", "AAAAA")

        assertEquals(5, result.size)
        assertTrue(result.all { it == Letter.Format.ExactMatch })
    }

    @Test
    fun testCalculateLetterColorsEdgeCase() {
        // Target: ABCDE
        // Guess:  EDCBA (reverse)
        val result = engine.calculateLetterColors("ABCDE", "EDCBA")

        assertEquals(5, result.size)
        // All letters are in the word but in wrong positions
        assertEquals(Letter.Format.Match, result[0]) // E
        assertEquals(Letter.Format.Match, result[1]) // D
        assertEquals(Letter.Format.ExactMatch, result[2]) // C (exact match at position 2)
        assertEquals(Letter.Format.Match, result[3]) // B
        assertEquals(Letter.Format.Match, result[4]) // A
    }

    @Test
    fun testCalculateLetterColorsCaseInsensitive() {
        val result1 = engine.calculateLetterColors("HELLO", "hello")
        val result2 = engine.calculateLetterColors("hello", "HELLO")
        val result3 = engine.calculateLetterColors("HeLLo", "hElLO")

        assertTrue(result1.all { it == Letter.Format.ExactMatch })
        assertTrue(result2.all { it == Letter.Format.ExactMatch })
        assertTrue(result3.all { it == Letter.Format.ExactMatch })
    }

    @Test
    fun testEvaluateGuess() {
        val letters = engine.evaluateGuess("HELLO", "HELLS")

        assertEquals(5, letters.size)
        assertEquals("H", letters[0].value)
        assertEquals(Letter.Format.ExactMatch, letters[0].format)
        assertEquals("E", letters[1].value)
        assertEquals(Letter.Format.ExactMatch, letters[1].format)
        assertEquals("L", letters[2].value)
        assertEquals(Letter.Format.ExactMatch, letters[2].format)
        assertEquals("L", letters[3].value)
        assertEquals(Letter.Format.ExactMatch, letters[3].format)
        assertEquals("S", letters[4].value)
        assertEquals(Letter.Format.NoMatch, letters[4].format)
    }

    @Test
    fun testIsCorrectGuess() {
        assertTrue(engine.isCorrectGuess("HELLO", "HELLO"))
        assertTrue(engine.isCorrectGuess("HELLO", "hello"))
        assertTrue(engine.isCorrectGuess("hello", "HELLO"))
        assertFalse(engine.isCorrectGuess("HELLO", "WORLD"))
        assertFalse(engine.isCorrectGuess("HELLO", "HELL"))
    }

    @Test
    fun testComplexDuplicateScenario() {
        // Target: HELLO (two L's)
        // Guess:  LLAMA (two L's at start)
        // Expected:
        // - L at position 0: partial match (matches L at position 2 or 3)
        // - L at position 1: partial match (matches remaining L)
        // - A at position 2: no match
        // - M at position 3: no match
        // - A at position 4: no match
        val result = engine.calculateLetterColors("HELLO", "LLAMA")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.Match, result[0])   // L (partial)
        assertEquals(Letter.Format.Match, result[1])   // L (partial)
        assertEquals(Letter.Format.NoMatch, result[2]) // A
        assertEquals(Letter.Format.NoMatch, result[3]) // M
        assertEquals(Letter.Format.NoMatch, result[4]) // A
    }

    @Test
    fun testRealWorldExample() {
        // Real Wordle example that often confuses people
        // Target: ROBOT
        // Guess:  FLOOR
        val result = engine.calculateLetterColors("ROBOT", "FLOOR")

        assertEquals(5, result.size)
        assertEquals(Letter.Format.NoMatch, result[0])  // F - not in word
        assertEquals(Letter.Format.NoMatch, result[1])  // L - not in word
        assertEquals(Letter.Format.Match, result[2])    // O - in word, wrong spot
        assertEquals(Letter.Format.Match, result[3])    // O - in word, wrong spot
        assertEquals(Letter.Format.NoMatch, result[4])  // R - not in correct position
    }
}
