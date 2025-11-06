package com.words.data

import com.words.domain.model.GameStatistics
import com.words.domain.repository.StatisticsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Implementation of StatisticsRepository that persists statistics using PreferencesManager.
 * Statistics are serialized to JSON and stored as strings.
 */
class PreferencesStatisticsRepository(
    private val preferencesManager: PreferencesManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
) : StatisticsRepository {

    companion object {
        private const val PREFIX = "stats_"
    }

    /**
     * Creates a preference key from word list name and word length.
     */
    private fun createKey(wordListName: String, wordLength: Int): String {
        return "$PREFIX${wordListName}_$wordLength"
    }

    override suspend fun loadStatistics(wordListName: String, wordLength: Int): GameStatistics {
        val key = createKey(wordListName, wordLength)
        val jsonString = preferencesManager.getString(key, "")

        return if (jsonString.isEmpty()) {
            GameStatistics()
        } else {
            try {
                json.decodeFromString<GameStatistics>(jsonString)
            } catch (e: Exception) {
                println("Error deserializing statistics for $wordListName/$wordLength: ${e.message}")
                GameStatistics()
            }
        }
    }

    override suspend fun saveStatistics(
        wordListName: String,
        wordLength: Int,
        statistics: GameStatistics
    ) {
        val key = createKey(wordListName, wordLength)
        try {
            val jsonString = json.encodeToString(statistics)
            preferencesManager.putString(key, jsonString)
        } catch (e: Exception) {
            println("Error serializing statistics for $wordListName/$wordLength: ${e.message}")
        }
    }

    override suspend fun clearStatistics(wordListName: String, wordLength: Int) {
        val key = createKey(wordListName, wordLength)
        preferencesManager.remove(key)
    }

    override suspend fun clearAllStatistics() {
        // Note: This is a simple implementation that only clears known prefixed keys
        // A more complete implementation would iterate over all keys
        preferencesManager.clear()
    }
}
