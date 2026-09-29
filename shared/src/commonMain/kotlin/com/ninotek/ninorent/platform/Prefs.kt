package com.ninotek.ninorent.platform

/** Key-value store nhỏ, tương đương SharedPreferences (Android) / NSUserDefaults (iOS). */
interface Prefs {
    fun getString(key: String, default: String?): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun contains(key: String): Boolean
    fun edit(): PrefsEditor
}

interface PrefsEditor {
    fun putString(key: String, value: String?): PrefsEditor
    fun putBoolean(key: String, value: Boolean): PrefsEditor
    fun remove(key: String): PrefsEditor
    fun apply()
}

expect fun PlatformContext.openPrefs(name: String): Prefs
