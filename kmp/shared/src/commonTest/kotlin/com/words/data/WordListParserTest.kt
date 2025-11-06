package com.words.data

import com.words.domain.model.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WordListParserTest {

    private val parser = WordListParser()

    @Test
    fun testParseKeyboardLayout() {
        val lines = listOf(
            "1Q,W,E,R,T,Y,U,I,O,P",
            "2A,S,D,F,G,H,J,K,L",
            "3SEND,Z,X,C,V,B,N,M,DEL"
        )

        val layout = parser.parseKeyboardLayout(lines)

        assertEquals(3, layout.size)
        assertEquals(10, layout[0].size)
        assertEquals(9, layout[1].size)
        assertEquals(9, layout[2].size)

        // Check first row
        assertTrue(layout[0][0] is Key.Letter)
        assertEquals('Q', (layout[0][0] as Key.Letter).char)

        // Check special keys
        assertTrue(layout[2][0] is Key.Enter)
        assertTrue(layout[2][8] is Key.Delete)
    }

    @Test
    fun testParseAvailableLengths() {
        val line = "4LENGTHS:4,5,6,7,8,9,10,11"
        val lengths = parser.parseAvailableLengths(line)

        assertEquals(8, lengths.size)
        assertTrue(lengths.contains(4))
        assertTrue(lengths.contains(5))
        assertTrue(lengths.contains(11))
    }

    @Test
    fun testParseAvailableLengthsAlternativeFormat() {
        val line = "LENGTHS:5,6,7"
        val lengths = parser.parseAvailableLengths(line)

        assertEquals(3, lengths.size)
        assertTrue(lengths.contains(5))
        assertTrue(lengths.contains(6))
        assertTrue(lengths.contains(7))
    }

    @Test
    fun testParseAvailableLengthsInvalidFormat() {
        assertFailsWith<IllegalArgumentException> {
            parser.parseAvailableLengths("INVALID:5,6,7")
        }
    }

    @Test
    fun testParseCompleteWordList() {
        val content = """
            1Q,W,E,R,T,Y,U,I,O,P
            2A,S,D,F,G,H,J,K,L
            3SEND,Z,X,C,V,B,N,M,DEL
            4LENGTHS:4,5,6
            HELLO
            WORLD
            TESTS
            -----
            VALID
            GUESS
            WORDS
        """.trimIndent()

        val wordList = parser.parse(content, "Test")

        assertEquals("Test", wordList.name)
        assertEquals(3, wordList.secretWords.size)
        assertTrue(wordList.secretWords.contains("HELLO"))
        assertTrue(wordList.secretWords.contains("WORLD"))
        assertTrue(wordList.secretWords.contains("TESTS"))

        assertEquals(3, wordList.validWords.size)
        assertTrue(wordList.validWords.contains("VALID"))
        assertTrue(wordList.validWords.contains("GUESS"))
        assertTrue(wordList.validWords.contains("WORDS"))

        assertEquals(3, wordList.keyboardLayout.size)
        assertEquals(setOf(4, 5, 6), wordList.availableLengths)

        assertTrue(wordList.allowedLetters.isNotEmpty())
        assertTrue(wordList.allowedLetters.contains('Q'))
        assertTrue(wordList.allowedLetters.contains('A'))
        assertTrue(wordList.allowedLetters.contains('Z'))
    }

    @Test
    fun testParseWordListWithoutSeparator() {
        val content = """
            1Q,W,E,R,T
            2A,S,D,F,G
            3SEND,Z,X,DEL
            4LENGTHS:5
            HELLO
            WORLD
        """.trimIndent()

        val wordList = parser.parse(content, "NoSeparator")

        assertEquals(2, wordList.secretWords.size)
        assertTrue(wordList.secretWords.contains("HELLO"))
        assertTrue(wordList.secretWords.contains("WORLD"))
        assertTrue(wordList.validWords.isEmpty())
    }

    @Test
    fun testParseWordListNormalizesCase() {
        val content = """
            1Q,W,E,R,T
            2A,S,D,F,G
            3SEND,Z,X,DEL
            4LENGTHS:5
            hello
            World
            TESTS
        """.trimIndent()

        val wordList = parser.parse(content, "CaseTest")

        // All words should be uppercase
        assertTrue(wordList.secretWords.contains("HELLO"))
        assertTrue(wordList.secretWords.contains("WORLD"))
        assertTrue(wordList.secretWords.contains("TESTS"))
    }

    @Test
    fun testParseWordListWithEmptyLines() {
        val content = """
            1Q,W,E,R,T
            2A,S,D,F,G
            3SEND,Z,X,DEL
            4LENGTHS:5

            HELLO

            WORLD

            -----

            VALID

        """.trimIndent()

        val wordList = parser.parse(content, "EmptyLines")

        assertEquals(2, wordList.secretWords.size)
        assertEquals(1, wordList.validWords.size)
    }

    @Test
    fun testParseInvalidWordList() {
        // Too few lines
        assertFailsWith<IllegalArgumentException> {
            parser.parse("1Q,W,E\n2A,S,D\n3Z,X,C", "Invalid")
        }
    }

    @Test
    fun testWordListContains() {
        val content = """
            1Q,W,E,R,T
            2A,S,D,F,G
            3SEND,Z,X,DEL
            4LENGTHS:5
            HELLO
            -----
            WORLD
        """.trimIndent()

        val wordList = parser.parse(content, "ContainsTest")

        assertTrue(wordList.contains("HELLO"))
        assertTrue(wordList.contains("hello"))
        assertTrue(wordList.contains("WORLD"))
        assertTrue(wordList.contains("world"))
    }
}
