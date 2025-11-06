package com.words.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single letter in the game grid.
 * @property value The letter character (empty string for unused cells)
 * @property format The visual formatting state of the letter
 * @property selected Whether this letter is currently selected by the user
 * @property incorrect Whether this letter has been marked as incorrect (for animation)
 */
@Serializable
data class Letter(
    val value: String = "",
    val format: Format = Format.NotUsed,
    val selected: Boolean = false,
    val incorrect: Boolean = false
) {
    /**
     * Visual formatting states for letters in the game.
     */
    @Serializable
    enum class Format {
        /** Cell has not been used yet (gray/default) */
        NotUsed,

        /** Letter was guessed but is not in the target word (dark gray) */
        NoMatch,

        /** Letter is in the target word but in the wrong position (yellow) */
        Match,

        /** Letter is in the target word and in the correct position (green) */
        ExactMatch
    }

    /**
     * Returns true if this letter has been evaluated (i.e., submitted as part of a guess).
     */
    val isEvaluated: Boolean
        get() = format != Format.NotUsed

    /**
     * Returns true if this cell is empty.
     */
    val isEmpty: Boolean
        get() = value.isEmpty()

    /**
     * Returns true if this cell contains a letter.
     */
    val hasLetter: Boolean
        get() = value.isNotEmpty()

    companion object {
        /** Creates an empty, unused letter cell. */
        fun empty(): Letter = Letter()

        /** Creates a letter with the given character value. */
        fun withValue(char: Char): Letter = Letter(value = char.toString().uppercase())

        /** Creates a letter with the given string value. */
        fun withValue(value: String): Letter = Letter(value = value.uppercase())
    }
}
