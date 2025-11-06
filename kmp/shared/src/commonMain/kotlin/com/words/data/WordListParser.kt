package com.words.data

import com.words.domain.model.Key
import com.words.domain.model.WordList

/**
 * Parser for word list files.
 *
 * Word list format:
 * - Line 1-3: Keyboard layout (row number, then comma-separated keys)
 * - Line 4: Available word lengths (4LENGTHS:5,6,7,...)
 * - Remaining lines: Words (secret words before separator, valid words after)
 * - Separator: Line starting with "-"
 *
 * Example:
 * ```
 * 1Q,W,E,R,T,Y,U,I,O,P
 * 2A,S,D,F,G,H,J,K,L
 * 3SEND,Z,X,C,V,B,N,M,DEL
 * 4LENGTHS:4,5,6,7,8
 * WORD
 * HELLO
 * -----
 * VALID
 * GUESS
 * ```
 */
class WordListParser {

    /**
     * Parses keyboard layout from the first three lines of a word list file.
     *
     * @param lines The first three lines of the file
     * @return List of three keyboard rows, each containing keys
     */
    fun parseKeyboardLayout(lines: List<String>): List<List<Key>> {
        require(lines.size >= 3) { "Keyboard layout requires at least 3 lines" }

        return lines.take(3).map { line ->
            // Remove the row number prefix (e.g., "1", "2", "3")
            val keysString = line.dropWhile { it.isDigit() }

            // Split by comma and convert to Key objects
            keysString.split(",")
                .filter { it.isNotBlank() }
                .map { keyString -> Key.fromString(keyString.trim()) }
        }
    }

    /**
     * Parses available word lengths from the lengths line.
     *
     * @param line The line starting with "4LENGTHS:" or "LENGTHS:"
     * @return Set of available word lengths
     */
    fun parseAvailableLengths(line: String): Set<Int> {
        // Handle both "4LENGTHS:" and "LENGTHS:" formats
        val prefix = if (line.startsWith("4LENGTHS:")) {
            "4LENGTHS:"
        } else if (line.startsWith("LENGTHS:")) {
            "LENGTHS:"
        } else {
            throw IllegalArgumentException("Invalid lengths format: $line")
        }

        val lengthsString = line.substring(prefix.length)

        return lengthsString.split(",")
            .filter { it.isNotBlank() }
            .map { it.trim().toInt() }
            .toSet()
    }

    /**
     * Parses a complete word list file.
     *
     * @param content The complete file content
     * @param name The name of the word list
     * @return Parsed WordList object
     */
    fun parse(content: String, name: String): WordList {
        val lines = content.lines().filter { it.isNotBlank() }
        require(lines.size >= 5) { "Word list must have at least 5 non-empty lines" }

        // Parse keyboard layout (lines 0-2)
        val keyboardLayout = parseKeyboardLayout(lines.take(3))

        // Parse available lengths (line 3)
        val availableLengths = parseAvailableLengths(lines[3])

        // Find the separator line (line starting with "-")
        val separatorIndex = lines.indexOfFirst { it.startsWith("-") }

        // Parse secret words (between line 4 and separator, or all remaining if no separator)
        val secretWords = if (separatorIndex > 4) {
            lines.subList(4, separatorIndex)
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() && it[0].isLetter() }
                .toSet()
        } else if (separatorIndex == -1 && lines.size > 4) {
            // No separator, all words after line 4 are secret words
            lines.subList(4, lines.size)
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() && it[0].isLetter() }
                .toSet()
        } else {
            emptySet()
        }

        // Parse valid words (after separator)
        val validWords = if (separatorIndex != -1 && separatorIndex < lines.size - 1) {
            lines.subList(separatorIndex + 1, lines.size)
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() && it[0].isLetter() }
                .toSet()
        } else {
            emptySet()
        }

        // Extract allowed letters from keyboard layout
        val allowedLetters = keyboardLayout
            .flatten()
            .filterIsInstance<Key.Letter>()
            .map { it.char }
            .toSet()

        return WordList(
            name = name,
            secretWords = secretWords,
            validWords = validWords,
            allowedLetters = allowedLetters,
            keyboardLayout = keyboardLayout,
            availableLengths = availableLengths
        )
    }
}
