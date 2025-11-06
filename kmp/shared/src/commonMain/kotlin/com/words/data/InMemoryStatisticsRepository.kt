package com.words.data

import com.words.domain.model.GameStatistics
import com.words.domain.repository.StatisticsRepository

/**
 * In-memory implementation of StatisticsRepository for testing.
 * Stores statistics in a mutable map with composite keys.
 */
class InMemoryStatisticsRepository : StatisticsRepository {

    private val statistics = mutableMapOf<String, GameStatistics>()

    /**
     * Creates a composite key from word list name and word length.
     */
    private fun createKey(wordListName: String, wordLength: Int): String {
        return "${wordListName}_$wordLength"
    }

    override suspend fun loadStatistics(wordListName: String, wordLength: Int): GameStatistics {
        val key = createKey(wordListName, wordLength)
        return statistics[key] ?: GameStatistics()
    }

    override suspend fun saveStatistics(
        wordListName: String,
        wordLength: Int,
        statistics: GameStatistics
    ) {
        val key = createKey(wordListName, wordLength)
        this.statistics[key] = statistics
    }

    override suspend fun clearStatistics(wordListName: String, wordLength: Int) {
        val key = createKey(wordListName, wordLength)
        statistics.remove(key)
    }

    override suspend fun clearAllStatistics() {
        statistics.clear()
    }

    /**
     * Gets the count of stored statistics entries.
     */
    fun count(): Int = statistics.size

    /**
     * Checks if statistics exist for a specific word list and length.
     */
    fun hasStatistics(wordListName: String, wordLength: Int): Boolean {
        val key = createKey(wordListName, wordLength)
        return statistics.containsKey(key)
    }
}
