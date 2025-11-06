package com.words.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the different pages/screens in the game.
 */
@Serializable
enum class GamePage {
    /** Main game screen with the letter grid and keyboard */
    Game,

    /** Game over screen showing results and statistics */
    GameOver,

    /** Statistics screen showing win/loss record and streaks */
    Statistics,

    /** Help screen explaining how to play */
    Help,

    /** Settings screen for configuring game options */
    Settings
}
