package com.words.data

import android.content.Context
import com.words.domain.model.WordList
import com.words.domain.repository.WordListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Android-specific word list repository that loads word lists from assets.
 */
class AndroidWordListRepository(private val context: Context) : WordListRepository {

    private val parser = WordListParser()
    private val cache = mutableMapOf<String, WordList>()

    override suspend fun getAvailableWordLists(): List<String> {
        return listOf("English", "Deutsch")
    }

    override suspend fun loadWordList(name: String): WordList? = withContext(Dispatchers.IO) {
        cache[name]?.let { return@withContext it }

        try {
            val content = loadAssetAsString("word-lists/$name.txt") ?: return@withContext null
            val wordList = parser.parse(content, name)
            cache[name] = wordList
            wordList
        } catch (e: Exception) {
            println("Error loading word list $name: ${e.message}")
            null
        }
    }

    override suspend fun loadWordList(name: String, length: Int): WordList? {
        val wordList = loadWordList(name) ?: return null
        return if (wordList.availableLengths.contains(length)) wordList else null
    }

    override fun getDefaultWordListName(): String = "English"

    /**
     * Clears the cache of loaded word lists.
     * Useful for testing.
     */
    fun clearCache() {
        cache.clear()
    }

    private fun loadAssetAsString(path: String): String? {
        return try {
            context.assets.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
