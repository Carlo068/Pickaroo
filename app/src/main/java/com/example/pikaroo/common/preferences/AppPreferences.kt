package com.example.pikaroo.common.preferences

import android.content.Context

class AppPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        preferences.getBoolean(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String =
        preferences.getString(key, defaultValue) ?: defaultValue

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}