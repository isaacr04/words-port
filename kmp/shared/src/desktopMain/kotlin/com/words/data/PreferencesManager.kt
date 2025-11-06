package com.words.data

import java.util.prefs.Preferences

/**
 * Desktop implementation of PreferencesManager using Java Preferences API.
 * Stores preferences in the system's user preferences store.
 */
actual class PreferencesManager {

    private val prefs: Preferences = Preferences.userNodeForPackage(PreferencesManager::class.java)

    actual fun getString(key: String, default: String): String {
        return prefs.get(key, default)
    }

    actual fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    actual fun getInt(key: String, default: Int): Int {
        return prefs.getInt(key, default)
    }

    actual fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
        prefs.flush()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        prefs.flush()
    }

    actual fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    actual fun clear() {
        prefs.clear()
        prefs.flush()
    }
}
