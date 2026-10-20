package com.example.pikaroo.common.mock

import android.content.Context
import com.example.pikaroo.common.preferences.AppPreferences

interface SeedSource {
    fun read(file: String): String
}

class AssetSeedSource(private val context: Context) : SeedSource {
    override fun read(file: String): String =
        context.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

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
