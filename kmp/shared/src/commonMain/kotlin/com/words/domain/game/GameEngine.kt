package com.words.domain.game

import com.words.domain.model.Letter
import com.words.domain.model.WordList
import kotlin.random.Random

/**
 * Core game engine responsible for game logic and word evaluation.
 */
class GameEngine(
    private val random: Random = Random.Default
) {

    /**
     * Picks a random word from the word list.
     *
     * @param wordList The word list to pick from
     * @param length The desired word length (optional, picks any length if not specified)
     * @return A random word from the secret words, or from all words if no secret words
     */
    fun pickRandomWord(wordList: WordList, length: Int? = null): String {
        val pool = if (length != null) {
            val secretWords = wordList.getSecretWordsOfLength(length)
            if (secretWords.isNotEmpty()) {
                secretWords
            } else {
                // Fallback to all words if no secret words of this length
                wordList.getWordsOfLength(length)
            }
        } else {
            if (wordList.secretWords.isNotEmpty()) {
                wordList.secretWords
            } else {
                wordList.validWords
            }
        }

        require(pool.isNotEmpty()) { "No words available to pick from" }

        return pool.random(random)
    }

    /**
     * Calculates the color formatting for each letter in a guessed word.
     *
     * Algorithm:
     * 1. First pass: Mark exact matches (correct letter in correct position)
     * 2. Count leftover letters from target word (excluding exact matches)
     * 3. Second pass: Mark partial matches (correct letter in wrong position)
     *    using leftover letter counts
     *
     * @param targetWord The word to guess
     * @param guessedWord The word that was guessed
     * @return List of Letter.Format values for each position
     */
    fun calculateLetterColors(targetWord: String, guessedWord: String): List<Letter.Format> {
        require(targetWord.length == guessedWord.length) {
            "Target and guessed words must have the same length"
        }

        val target = targetWord.uppercase()
        val guess = guessedWord.uppercase()
        val length = target.length

        // Initialize result with NoMatch for all positions
        val result = MutableList(length) { Letter.Format.NoMatch }

        // Track which positions have exact matches
        val exactMatches = mutableSetOf<Int>()

        // Count leftover letters in target (excluding exact matches)
        val leftoverLetters = mutableMapOf<Char, Int>()

        // First pass: Find exact matches
        for (index in 0 until length) {
            val targetChar = target[index]
            val guessChar = guess[index]

            if (targetChar == guessChar) {
                result[index] = Letter.Format.ExactMatch
                exactMatches.add(index)
            } else {
                // Count letters that aren't exact matches
                leftoverLetters[targetChar] = leftoverLetters.getOrDefault(targetChar, 0) + 1
            }
        }

        // Second pass: Find partial matches using leftover letters
        for (index in 0 until length) {
            if (index !in exactMatches) {
                val guessChar = guess[index]
                val count = leftoverLetters[guessChar] ?: 0

                if (count > 0) {
                    result[index] = Letter.Format.Match
                    leftoverLetters[guessChar] = count - 1
                }
                // else: remains NoMatch (already set)
            }
        }

        return result
    }

    /**
     * Evaluates a guessed word and returns formatted letters.
     *
     * @param targetWord The word to guess
     * @param guessedWord The word that was guessed
     * @return List of Letter objects with appropriate formatting
     */
    fun evaluateGuess(targetWord: String, guessedWord: String): List<Letter> {
        val colors = calculateLetterColors(targetWord, guessedWord)
        val guess = guessedWord.uppercase()

        return guess.mapIndexed { index, char ->
            Letter(
                value = char.toString(),
                format = colors[index],
                selected = false,
                incorrect = false
            )
        }
    }

    /**
     * Checks if the guessed word matches the target word.
     */
    fun isCorrectGuess(targetWord: String, guessedWord: String): Boolean {
        return targetWord.uppercase() == guessedWord.uppercase()
    }

    companion object {
        /** Maximum number of attempts allowed in the game */
        const val MAX_ATTEMPTS = 6

        /** Default word length */
        const val DEFAULT_WORD_LENGTH = 5
    }
}
