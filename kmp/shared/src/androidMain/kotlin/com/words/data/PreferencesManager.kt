package com.words.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of PreferencesManager using SharedPreferences.
 *
 * Note: This requires a Context instance to be provided.
 * In a real application, this would typically be provided via dependency injection.
 */
actual class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    actual fun getString(key: String, default: String): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    actual fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual fun getInt(key: String, default: Int): Int {
        return sharedPreferences.getInt(key, default)
    }

    actual fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    actual fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    actual fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "words_preferences"
    }
}
