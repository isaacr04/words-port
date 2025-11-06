package com.words.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a word list with its associated configuration.
 *
 * @property name The name of the word list (e.g., "English", "Deutsch")
 * @property secretWords Words that can be chosen as the target word
 * @property validWords Words that are accepted as valid guesses (includes secretWords)
 * @property allowedLetters Set of letters that can be used in this word list
 * @property keyboardLayout The keyboard layout for this word list (3 rows of keys)
 * @property availableLengths Word lengths supported by this word list
 */
@Serializable
data class WordList(
    val name: String,
    val secretWords: Set<String>,
    val validWords: Set<String>,
    val allowedLetters: Set<Char>,
    val keyboardLayout: List<List<Key>>,
    val availableLengths: Set<Int>
) {
    init {
        require(name.isNotBlank()) { "Word list name cannot be blank" }
        require(keyboardLayout.size == 3) { "Keyboard layout must have exactly 3 rows" }
        require(availableLengths.isNotEmpty()) { "Available lengths cannot be empty" }
        require(availableLengths.all { it > 0 }) { "Word lengths must be positive" }
    }

    /**
     * Checks if a word is valid (either a secret word or a valid guess word).
     */
    fun contains(word: String): Boolean {
        val normalized = word.uppercase()
        return secretWords.contains(normalized) || validWords.contains(normalized)
    }

    /**
     * Checks if a word is in the secret words set.
     */
    fun isSecretWord(word: String): Boolean {
        return secretWords.contains(word.uppercase())
    }

    /**
     * Gets all words (secret + valid) of a specific length.
     */
    fun getWordsOfLength(length: Int): Set<String> {
        return (secretWords + validWords).filter { it.length == length }.toSet()
    }

    /**
     * Gets only secret words of a specific length.
     */
    fun getSecretWordsOfLength(length: Int): Set<String> {
        return secretWords.filter { it.length == length }.toSet()
    }

    /**
     * Returns the total number of words (secret + valid).
     */
    val totalWords: Int
        get() = (secretWords + validWords).size

    /**
     * Returns the total number of unique words.
     */
    val uniqueWords: Int
        get() = (secretWords + validWords).distinct().size

    companion object {
        /**
         * Creates an empty word list for testing purposes.
         */
        fun empty(name: String = "Empty"): WordList {
            return WordList(
                name = name,
                secretWords = emptySet(),
                validWords = emptySet(),
                allowedLetters = emptySet(),
                keyboardLayout = listOf(
                    listOf(Key.Enter),
                    listOf(Key.Delete),
                    listOf(Key.Enter)
                ),
                availableLengths = setOf(5)
            )
        }
    }
}
