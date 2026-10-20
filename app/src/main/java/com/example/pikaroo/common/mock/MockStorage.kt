package com.example.pikaroo.common.mock

import android.content.Context

interface SeedSource {
    fun read(file: String): String
}

class AssetSeedSource(private val context: Context) : SeedSource {
    override fun read(file: String): String =
        context.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
