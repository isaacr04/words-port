package com.words.presentation

import com.words.domain.model.Coord

/**
 * Represents user intents/actions in the game.
 * Following the MVI (Model-View-Intent) pattern.
 */
sealed class GameIntent {

    /**
     * Start a new game with the current word list and settings.
     */
    data object StartNewGame : GameIntent()

    /**
     * Select a specific field/letter in the grid.
     */
    data class SelectField(val coord: Coord) : GameIntent()

    /**
     * Enter a letter at the current position.
     */
    data class EnterLetter(val char: Char) : GameIntent()

    /**
     * Submit the current word for evaluation.
     */
    data object EnterWord : GameIntent()

    /**
     * Delete the letter at the current position (backspace).
     */
    data object Backspace : GameIntent()

    /**
     * Move the cursor left or right.
     * @property step Negative for left, positive for right
     */
    data class MoveCursor(val step: Int) : GameIntent()

    /**
     * Change the word length setting.
     */
    data class SetWordLength(val length: Int) : GameIntent()

    /**
     * Switch to a different word list.
     */
    data class SwitchWordList(val name: String) : GameIntent()

    /**
     * Show the statistics screen.
     */
    data object ShowStatistics : GameIntent()

    /**
     * Show the help screen.
     */
    data object ShowHelp : GameIntent()

    /**
     * Show the settings screen.
     */
    data object ShowSettings : GameIntent()

    /**
     * Return to the game screen.
     */
    data object ReturnToGame : GameIntent()

    /**
     * Dismiss the invalid word animation.
     */
    data object DismissInvalidWordAnimation : GameIntent()

    /**
     * Load game statistics from storage.
     */
    data object LoadStatistics : GameIntent()

    /**
     * Save game statistics to storage.
     */
    data object SaveStatistics : GameIntent()
}
