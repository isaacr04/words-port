package com.words.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a key on the virtual keyboard.
 */
@Serializable
sealed class Key {
    /**
     * A letter key on the keyboard.
     * @property char The character this key represents (uppercase)
     */
    @Serializable
    data class Letter(val char: Char) : Key() {
        init {
            require(char.isLetter()) { "Key character must be a letter" }
        }

        override fun toString(): String = char.toString()
    }

    /**
     * The Enter/Submit key.
     */
    @Serializable
    data object Enter : Key() {
        override fun toString(): String = "ENTER"
    }

    /**
     * The Delete/Backspace key.
     */
    @Serializable
    data object Delete : Key() {
        override fun toString(): String = "DEL"
    }

    companion object {
        /**
         * Creates a Key from a string representation.
         * Used for parsing keyboard layouts from word list files.
         */
        fun fromString(value: String): Key {
            return when (value.uppercase()) {
                "SEND", "ENTER" -> Enter
                "DEL", "DELETE", "BACKSPACE" -> Delete
                else -> {
                    require(value.length == 1 && value[0].isLetter()) {
                        "Invalid key value: $value"
                    }
                    Letter(value[0].uppercaseChar())
                }
            }
        }
    }
}

/**
 * Formatting state for a keyboard key, used to show which letters have been guessed.
 */
@Serializable
enum class KeyFormat {
    /** Key has not been used yet */
    Unused,

    /** Letter was guessed but is not in the target word */
    NoMatch,

    /** Letter is in the target word but guessed in wrong position */
    Match,

    /** Letter is in the target word and guessed in correct position */
    ExactMatch;

    companion object {
        /**
         * Converts a Letter.Format to the corresponding KeyFormat.
         */
        fun fromLetterFormat(format: Letter.Format): KeyFormat {
            return when (format) {
                Letter.Format.NotUsed -> Unused
                Letter.Format.NoMatch -> NoMatch
                Letter.Format.Match -> Match
                Letter.Format.ExactMatch -> ExactMatch
            }
        }
    }
}
