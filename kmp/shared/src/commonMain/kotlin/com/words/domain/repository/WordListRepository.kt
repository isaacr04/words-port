package com.words.domain.repository

import com.words.domain.model.WordList

/**
 * Repository interface for loading and managing word lists.
 */
interface WordListRepository {

    /**
     * Gets the list of available word list names.
     * @return List of word list names (e.g., "English", "Deutsch")
     */
    suspend fun getAvailableWordLists(): List<String>

    /**
     * Loads a word list by name.
     * @param name The name of the word list to load
     * @return The loaded WordList, or null if not found
     */
    suspend fun loadWordList(name: String): WordList?

    /**
     * Loads a word list by name and filters it for a specific length.
     * This is a convenience method that loads the full word list and
     * checks if the requested length is supported.
     *
     * @param name The name of the word list to load
     * @param length The desired word length
     * @return The loaded WordList if it supports the requested length, null otherwise
     */
    suspend fun loadWordList(name: String, length: Int): WordList? {
        val wordList = loadWordList(name) ?: return null
        return if (wordList.availableLengths.contains(length)) {
            wordList
        } else {
            null
        }
    }

    /**
     * Gets the default word list name.
     * @return The name of the default word list (typically "English")
     */
    fun getDefaultWordListName(): String = "English"
}
