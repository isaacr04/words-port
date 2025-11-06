package com.words.data

import com.words.domain.model.GameStatistics
import com.words.domain.model.Key
import com.words.domain.model.WordList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {

    private fun createTestWordList(name: String, length: Int = 5): WordList {
        return WordList(
            name = name,
            secretWords = setOf("HELLO", "WORLD", "TESTS"),
            validWords = setOf("VALID", "GUESS"),
            allowedLetters = setOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'),
            keyboardLayout = listOf(
                listOf(Key.Letter('Q'), Key.Letter('W'), Key.Letter('E')),
                listOf(Key.Letter('A'), Key.Letter('S'), Key.Letter('D')),
                listOf(Key.Enter, Key.Letter('Z'), Key.Delete)
            ),
            availableLengths = setOf(length)
        )
    }

    // WordListRepository Tests

    @Test
    fun testInMemoryWordListRepository_addAndLoad() = runTest {
        val repository = InMemoryWordListRepository()
        val wordList = createTestWordList("English")

        repository.addWordList(wordList)

        val loaded = repository.loadWordList("English")
        assertNotNull(loaded)
        assertEquals("English", loaded.name)
        assertEquals(3, loaded.secretWords.size)
    }

    @Test
    fun testInMemoryWordListRepository_getAvailableWordLists() = runTest {
        val repository = InMemoryWordListRepository()
        repository.addWordList(createTestWordList("English"))
        repository.addWordList(createTestWordList("Deutsch"))

        val available = repository.getAvailableWordLists()

        assertEquals(2, available.size)
        assertTrue(available.contains("English"))
        assertTrue(available.contains("Deutsch"))
    }

    @Test
    fun testInMemoryWordListRepository_loadWithLength() = runTest {
        val repository = InMemoryWordListRepository()
        val wordList = createTestWordList("English", length = 5)
        repository.addWordList(wordList)

        val loaded = repository.loadWordList("English", 5)
        assertNotNull(loaded)

        val notFound = repository.loadWordList("English", 6)
        assertNull(notFound)
    }

    @Test
    fun testInMemoryWordListRepository_clear() = runTest {
        val repository = InMemoryWordListRepository()
        repository.addWordList(createTestWordList("English"))
        repository.addWordList(createTestWordList("Deutsch"))

        assertEquals(2, repository.count())

        repository.clear()

        assertEquals(0, repository.count())
    }

    @Test
    fun testInMemoryWordListRepository_nonExistentWordList() = runTest {
        val repository = InMemoryWordListRepository()

        val loaded = repository.loadWordList("NonExistent")
        assertNull(loaded)
    }

    // StatisticsRepository Tests

    @Test
    fun testInMemoryStatisticsRepository_loadEmptyStatistics() = runTest {
        val repository = InMemoryStatisticsRepository()

        val stats = repository.loadStatistics("English", 5)

        assertNotNull(stats)
        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.gamesWon)
    }

    @Test
    fun testInMemoryStatisticsRepository_saveAndLoad() = runTest {
        val repository = InMemoryStatisticsRepository()
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
    }

    @Test
    fun testInMemoryStatisticsRepository_separateKeys() = runTest {
        val repository = InMemoryStatisticsRepository()
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
    fun testInMemoryStatisticsRepository_clearStatistics() = runTest {
        val repository = InMemoryStatisticsRepository()
        val statistics = GameStatistics(totalGames = 10, gamesWon = 7)

        repository.saveStatistics("English", 5, statistics)
        assertTrue(repository.hasStatistics("English", 5))

        repository.clearStatistics("English", 5)
        assertFalse(repository.hasStatistics("English", 5))

        val loaded = repository.loadStatistics("English", 5)
        assertEquals(0, loaded.totalGames)
    }

    @Test
    fun testInMemoryStatisticsRepository_clearAllStatistics() = runTest {
        val repository = InMemoryStatisticsRepository()
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 5))
        repository.saveStatistics("English", 6, GameStatistics(totalGames = 10))
        repository.saveStatistics("Deutsch", 5, GameStatistics(totalGames = 3))

        assertEquals(3, repository.count())

        repository.clearAllStatistics()

        assertEquals(0, repository.count())
    }

    @Test
    fun testInMemoryStatisticsRepository_overwriteStatistics() = runTest {
        val repository = InMemoryStatisticsRepository()
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 5))
        repository.saveStatistics("English", 5, GameStatistics(totalGames = 10))

        val loaded = repository.loadStatistics("English", 5)
        assertEquals(10, loaded.totalGames)
    }

    @Test
    fun testInMemoryStatisticsRepository_count() = runTest {
        val repository = InMemoryStatisticsRepository()

        assertEquals(0, repository.count())

        repository.saveStatistics("English", 5, GameStatistics())
        assertEquals(1, repository.count())

        repository.saveStatistics("English", 6, GameStatistics())
        assertEquals(2, repository.count())

        repository.saveStatistics("Deutsch", 5, GameStatistics())
        assertEquals(3, repository.count())
    }
}
