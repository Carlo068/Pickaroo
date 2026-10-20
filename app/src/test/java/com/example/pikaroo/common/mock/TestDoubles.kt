package com.example.pikaroo.common.mock

import java.io.File

class FileSeedSource(private val dir: File = defaultDir()) : SeedSource {

    override fun read(file: String): String = File(dir, file).readText(Charsets.UTF_8)

    companion object {
        fun defaultDir(): File = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "docs/mock") }
            .first { it.isDirectory }
    }
}

class InMemoryStringStore : StringStore {
    val data = mutableMapOf<String, String>()

    override fun get(key: String): String? = data[key]

    override fun put(key: String, value: String) {
        data[key] = value
    }

    override fun remove(key: String) {
        data.remove(key)
    }
}
