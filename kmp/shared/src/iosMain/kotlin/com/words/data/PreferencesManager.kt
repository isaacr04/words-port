package com.words.data

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesManager using NSUserDefaults.
 * NSUserDefaults is the iOS equivalent of Android's SharedPreferences.
 */
actual class PreferencesManager {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, default: String): String {
        return userDefaults.stringForKey(key) ?: default
    }

    actual fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
        userDefaults.synchronize()
    }

    actual fun getInt(key: String, default: Int): Int {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.integerForKey(key).toInt()
        } else {
            default
        }
    }

    actual fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), forKey = key)
        userDefaults.synchronize()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.boolForKey(key)
        } else {
            default
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
        userDefaults.synchronize()
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    actual fun clear() {
        val appDomain = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        appDomain?.let {
            userDefaults.removePersistentDomainForName(it)
            userDefaults.synchronize()
        }
    }
}
