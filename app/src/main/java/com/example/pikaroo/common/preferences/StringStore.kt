package com.example.pikaroo.common.preferences

import android.content.Context

interface StringStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

class PreferencesStringStore(context: Context) : StringStore {
    private val preferences = AppPreferences(context)

    override fun get(key: String): String? = preferences.getString(key).ifEmpty { null }

    override fun put(key: String, value: String) = preferences.putString(key, value)

    override fun remove(key: String) = preferences.remove(key)
}
