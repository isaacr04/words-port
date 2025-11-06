package com.words.data

import com.words.domain.model.WordList
import com.words.domain.repository.WordListRepository

/**
 * In-memory implementation of WordListRepository for testing.
 * Stores word lists in a mutable map.
 */
class InMemoryWordListRepository : WordListRepository {

    private val wordLists = mutableMapOf<String, WordList>()

    /**
     * Adds a word list to the repository.
     * This is useful for testing.
     */
    fun addWordList(wordList: WordList) {
        wordLists[wordList.name] = wordList
    }

    /**
     * Adds multiple word lists to the repository.
     */
    fun addWordLists(vararg wordLists: WordList) {
        wordLists.forEach { addWordList(it) }
    }

    override suspend fun getAvailableWordLists(): List<String> {
        return wordLists.keys.toList().sorted()
    }

    override suspend fun loadWordList(name: String): WordList? {
        return wordLists[name]
    }

    /**
     * Clears all word lists from the repository.
     */
    fun clear() {
        wordLists.clear()
    }

    /**
     * Gets the count of word lists in the repository.
     */
    fun count(): Int = wordLists.size
}
