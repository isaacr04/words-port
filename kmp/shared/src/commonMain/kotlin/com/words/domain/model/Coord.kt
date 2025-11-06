package com.words.domain.model

/**
 * Represents a coordinate position in the game grid.
 * @property column The horizontal position (0-based index)
 * @property row The vertical position (0-based index, 0 = top row)
 */
data class Coord(
    val column: Int,
    val row: Int
) {
    init {
        require(column >= 0) { "Column must be non-negative" }
        require(row >= 0) { "Row must be non-negative" }
    }

    /**
     * Creates a new Coord with the column offset by the given delta.
     */
    fun offsetColumn(delta: Int): Coord = copy(column = column + delta)

    /**
     * Creates a new Coord with the row offset by the given delta.
     */
    fun offsetRow(delta: Int): Coord = copy(row = row + delta)

    companion object {
        val ORIGIN = Coord(0, 0)
    }
}
