package com.words.domain.repository

import com.words.domain.model.GameStatistics

/**
 * Repository interface for loading and saving game statistics.
 * Statistics are stored per word list and word length combination.
 */
interface StatisticsRepository {

    /**
     * Loads statistics for a specific word list and word length.
     *
     * @param wordListName The name of the word list
     * @param wordLength The length of words
     * @return The saved statistics, or a new GameStatistics() if none exist
     */
    suspend fun loadStatistics(wordListName: String, wordLength: Int): GameStatistics

    /**
     * Saves statistics for a specific word list and word length.
     *
     * @param wordListName The name of the word list
     * @param wordLength The length of words
     * @param statistics The statistics to save
     */
    suspend fun saveStatistics(
        wordListName: String,
        wordLength: Int,
        statistics: GameStatistics
    )

    /**
     * Clears all statistics for a specific word list and word length.
     *
     * @param wordListName The name of the word list
     * @param wordLength The length of words
     */
    suspend fun clearStatistics(wordListName: String, wordLength: Int)

    /**
     * Clears all statistics for all word lists and word lengths.
     */
    suspend fun clearAllStatistics()
}
