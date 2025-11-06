package com.words.data

import com.words.domain.model.WordList
import com.words.domain.repository.WordListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop implementation of WordListRepository that loads word lists from resources.
 * Word list files should be placed in src/commonMain/resources/word-lists/
 */
class ResourceWordListRepository : WordListRepository {

    private val parser = WordListParser()
    private val cache = mutableMapOf<String, WordList>()

    /**
     * Gets available word lists by scanning the resources directory.
     * For now, returns a hardcoded list of known word lists.
     */
    override suspend fun getAvailableWordLists(): List<String> {
        return listOf("English", "Deutsch")
    }

    /**
     * Loads a word list from resources.
     * Files are expected to be at word-lists/{name}.txt
     */
    override suspend fun loadWordList(name: String): WordList? = withContext(Dispatchers.IO) {
        // Check cache first
        cache[name]?.let { return@withContext it }

        try {
            val resourcePath = "word-lists/$name.txt"
            val content = loadResourceAsString(resourcePath) ?: return@withContext null

            val wordList = parser.parse(content, name)
            cache[name] = wordList
            wordList
        } catch (e: Exception) {
            println("Error loading word list $name: ${e.message}")
            null
        }
    }

    /**
     * Loads a resource file as a string.
     */
    private fun loadResourceAsString(path: String): String? {
        return try {
            val classLoader = this::class.java.classLoader
            val inputStream = classLoader?.getResourceAsStream(path)
                ?: return null

            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("Error reading resource $path: ${e.message}")
            null
        }
    }

    /**
     * Clears the word list cache.
     */
    fun clearCache() {
        cache.clear()
    }
}
