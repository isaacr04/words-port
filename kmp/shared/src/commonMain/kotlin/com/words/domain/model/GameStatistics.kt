package com.words.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents game statistics for tracking wins, losses, and streaks.
 *
 * @property totalGames Total number of games played
 * @property gamesWon Total number of games won
 * @property currentStreak Current consecutive win streak
 * @property longestStreak Longest consecutive win streak ever achieved
 * @property lastStreak The streak count before the current streak was broken
 * @property winDistribution Distribution of wins by number of attempts (index 0 = 1 attempt, etc.)
 */
@Serializable
data class GameStatistics(
    val totalGames: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStreak: Int = 0,
    val winDistribution: List<Int> = List(6) { 0 }
) {
    init {
        require(totalGames >= 0) { "Total games cannot be negative" }
        require(gamesWon >= 0) { "Games won cannot be negative" }
        require(gamesWon <= totalGames) { "Games won cannot exceed total games" }
        require(currentStreak >= 0) { "Current streak cannot be negative" }
        require(longestStreak >= 0) { "Longest streak cannot be negative" }
        require(winDistribution.size == 6) { "Win distribution must have 6 entries" }
        require(winDistribution.all { it >= 0 }) { "Win distribution entries cannot be negative" }
    }

    /**
     * Updates statistics with the outcome of a game.
     *
     * @param won Whether the game was won
     * @param attempts Number of attempts used (1-6)
     * @return Updated GameStatistics
     */
    fun updateWithOutcome(won: Boolean, attempts: Int): GameStatistics {
        require(attempts in 1..6) { "Attempts must be between 1 and 6" }

        return if (won) {
            val newCurrentStreak = currentStreak + 1
            val newWinDistribution = winDistribution.toMutableList().apply {
                this[attempts - 1] = this[attempts - 1] + 1
            }

            copy(
                totalGames = totalGames + 1,
                gamesWon = gamesWon + 1,
                currentStreak = newCurrentStreak,
                longestStreak = maxOf(longestStreak, newCurrentStreak),
                winDistribution = newWinDistribution
            )
        } else {
            copy(
                totalGames = totalGames + 1,
                lastStreak = currentStreak,
                currentStreak = 0
            )
        }
    }

    /**
     * Calculates the win percentage.
     */
    val winPercentage: Double
        get() = if (totalGames > 0) (gamesWon.toDouble() / totalGames) * 100 else 0.0

    /**
     * Gets the number of games lost.
     */
    val gamesLost: Int
        get() = totalGames - gamesWon

    /**
     * Gets the most common winning attempt count.
     */
    val mostCommonWinAttempt: Int?
        get() {
            val maxWins = winDistribution.maxOrNull() ?: return null
            return if (maxWins > 0) {
                winDistribution.indexOf(maxWins) + 1
            } else {
                null
            }
        }
}
