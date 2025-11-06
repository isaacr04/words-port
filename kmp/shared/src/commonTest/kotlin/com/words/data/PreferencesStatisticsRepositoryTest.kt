package com.words.data

import com.words.domain.model.GameStatistics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Tests for PreferencesStatisticsRepository using in-memory PreferencesManager.
 */
class PreferencesStatisticsRepositoryTest {

    private lateinit var preferencesManager: TestPreferencesManager
    private lateinit var repository: PreferencesStatisticsRepository

    @BeforeTest
    fun setup() {
        preferencesManager = TestPreferencesManager()
        repository = PreferencesStatisticsRepository(preferencesManager)
    }

    @Test
    fun testLoadEmptyStatistics() = runTest {
        val stats = repository.loadStatistics("English", 5)

        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.gamesWon)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun testSaveAndLoadStatistics() = runTest {
        val statistics = GameStatistics(
            totalGames = 10,
            gamesWon = 7,
            currentStreak = 3,
            longestStreak = 5,
            lastStreak = 4,
            winDistribution = listOf(1, 2, 2, 1, 1, 0)
        )

        repository.saveStatistics("English", 5, statistics)
        val loaded = repository.loadStatistics("English", 5)

        assertEquals(10, loaded.totalGames)
        assertEquals(7, loaded.gamesWon)
        assertEquals(3, loaded.currentStreak)
        assertEquals(5, loaded.longestStreak)
        assertEquals(4, loaded.lastStreak)
        assertEquals(listOf(1, 2, 2, 1, 1, 0), loaded.winDistribution)
    }

    @Test
    fun testSeparateKeys() = runTest {
        val stats1 = GameStatistics(totalGames = 5, gamesWon = 3)
        val stats2 = GameStatistics(totalGames = 10, gamesWon = 7)

        repository.saveStatistics("English", 5, stats1)
        repository.saveStatistics("English", 6, stats2)

        val loaded1 = repository.loadStatistics("English", 5)
        val loaded2 = repository.loadStatistics("English", 6)

        assertEquals(5, loaded1.totalGames)
        assertEquals(10, loaded2.totalGames)
    }

    @Test
    fun testClearStatistics() = runTest {
        val statistics = GameStatistics(totalGames = 10, gamesWon = 7)

        repository.saveStatistics("English", 5, statistics)
        repository.clearStatistics("English", 5)

        val loaded = repository.loadStatistics("English", 5)
        assertEquals(0, loaded.totalGames)
    }

    @Test
    fun testClearAllStatistics() = runTest {
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 5))
        repository.saveStatistics("English", 6, GameStatistics(totalGames = 10))
        repository.saveStatistics("Deutsch", 5, GameStatistics(totalGames = 3))

        repository.clearAllStatistics()

        assertEquals(0, repository.loadStatistics("English", 5).totalGames)
        assertEquals(0, repository.loadStatistics("English", 6).totalGames)
        assertEquals(0, repository.loadStatistics("Deutsch", 5).totalGames)
    }

    @Test
    fun testOverwriteStatistics() = runTest {
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 5))
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 10))

        val loaded = repository.loadStatistics("English", 5)
        assertEquals(10, loaded.totalGames)
    }

    @Test
    fun testWinDistribution() = runTest {
        val statistics = GameStatistics(
            totalGames = 6,
            gamesWon = 6,
            winDistribution = listOf(1, 2, 1, 1, 1, 0)
        )

        repository.saveStatistics("English", 5, statistics)
        val loaded = repository.loadStatistics("English", 5)

        assertEquals(listOf(1, 2, 1, 1, 1, 0), loaded.winDistribution)
    }
}

/**
 * Test implementation of PreferencesManager for unit testing.
 */
private class TestPreferencesManager : PreferencesManager {
    private val storage = mutableMapOf<String, Any>()

    override fun getString(key: String, default: String): String {
        return storage[key] as? String ?: default
    }

    override fun putString(key: String, value: String) {
        storage[key] = value
    }

    override fun getInt(key: String, default: Int): Int {
        return storage[key] as? Int ?: default
    }

    override fun putInt(key: String, value: Int) {
        storage[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return storage[key] as? Boolean ?: default
    }

    override fun putBoolean(key: String, value: Boolean) {
        storage[key] = value
    }

    override fun remove(key: String) {
        storage.remove(key)
    }

    override fun clear() {
        storage.clear()
    }
}
