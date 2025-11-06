package com.words.data

/**
 * Platform-agnostic key-value storage interface.
 * Platform-specific implementations will be provided in androidMain, iosMain, and desktopMain.
 */
expect class PreferencesManager {

    /**
     * Gets a string value from preferences.
     * @param key The preference key
     * @param default The default value if key doesn't exist
     * @return The stored value or default
     */
    fun getString(key: String, default: String): String

    /**
     * Stores a string value in preferences.
     * @param key The preference key
     * @param value The value to store
     */
    fun putString(key: String, value: String)

    /**
     * Gets an integer value from preferences.
     * @param key The preference key
     * @param default The default value if key doesn't exist
     * @return The stored value or default
     */
    fun getInt(key: String, default: Int): Int

    /**
     * Stores an integer value in preferences.
     * @param key The preference key
     * @param value The value to store
     */
    fun putInt(key: String, value: Int)

    /**
     * Gets a boolean value from preferences.
     * @param key The preference key
     * @param default The default value if key doesn't exist
     * @return The stored value or default
     */
    fun getBoolean(key: String, default: Boolean): Boolean

    /**
     * Stores a boolean value in preferences.
     * @param key The preference key
     * @param value The value to store
     */
    fun putBoolean(key: String, value: Boolean)

    /**
     * Removes a key from preferences.
     * @param key The preference key to remove
     */
    fun remove(key: String)

    /**
     * Clears all preferences.
     */
    fun clear()
}
