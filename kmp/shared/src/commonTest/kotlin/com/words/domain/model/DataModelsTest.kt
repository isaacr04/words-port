package com.words.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataModelsTest {

    @Test
    fun testCoordCreation() {
        val coord = Coord(2, 3)
        assertEquals(2, coord.column)
        assertEquals(3, coord.row)
    }

    @Test
    fun testCoordOffset() {
        val coord = Coord(2, 3)
        val offsetCol = coord.offsetColumn(1)
        val offsetRow = coord.offsetRow(-1)

        assertEquals(3, offsetCol.column)
        assertEquals(3, offsetCol.row)
        assertEquals(2, offsetRow.column)
        assertEquals(2, offsetRow.row)
    }

    @Test
    fun testLetterEmpty() {
        val letter = Letter.empty()
        assertTrue(letter.isEmpty)
        assertFalse(letter.hasLetter)
        assertFalse(letter.isEvaluated)
        assertEquals(Letter.Format.NotUsed, letter.format)
    }

    @Test
    fun testLetterWithValue() {
        val letter = Letter.withValue('a')
        assertFalse(letter.isEmpty)
        assertTrue(letter.hasLetter)
        assertEquals("A", letter.value)
    }

    @Test
    fun testLetterFormats() {
        val notUsed = Letter(value = "", format = Letter.Format.NotUsed)
        val noMatch = Letter(value = "X", format = Letter.Format.NoMatch)
        val match = Letter(value = "A", format = Letter.Format.Match)
        val exactMatch = Letter(value = "B", format = Letter.Format.ExactMatch)

        assertFalse(notUsed.isEvaluated)
        assertTrue(noMatch.isEvaluated)
        assertTrue(match.isEvaluated)
        assertTrue(exactMatch.isEvaluated)
    }

    @Test
    fun testKeyFromString() {
        val letterKey = Key.fromString("A")
        val enterKey = Key.fromString("SEND")
        val deleteKey = Key.fromString("DEL")

        assertTrue(letterKey is Key.Letter)
        assertEquals('A', (letterKey as Key.Letter).char)
        assertTrue(enterKey is Key.Enter)
        assertTrue(deleteKey is Key.Delete)
    }

    @Test
    fun testKeyFormatConversion() {
        assertEquals(
            KeyFormat.Unused,
            KeyFormat.fromLetterFormat(Letter.Format.NotUsed)
        )
        assertEquals(
            KeyFormat.NoMatch,
            KeyFormat.fromLetterFormat(Letter.Format.NoMatch)
        )
        assertEquals(
            KeyFormat.Match,
            KeyFormat.fromLetterFormat(Letter.Format.Match)
        )
        assertEquals(
            KeyFormat.ExactMatch,
            KeyFormat.fromLetterFormat(Letter.Format.ExactMatch)
        )
    }

    @Test
    fun testGamePageEnum() {
        val pages = GamePage.values()
        assertEquals(5, pages.size)
        assertTrue(pages.contains(GamePage.Game))
        assertTrue(pages.contains(GamePage.GameOver))
        assertTrue(pages.contains(GamePage.Statistics))
        assertTrue(pages.contains(GamePage.Help))
        assertTrue(pages.contains(GamePage.Settings))
    }
}
