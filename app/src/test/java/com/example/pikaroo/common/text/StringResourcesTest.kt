package com.example.pikaroo.common.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** English (the fallback) and Spanish must define exactly the same texts. */
class StringResourcesTest {

    private class Resources(val strings: Map<String, String>, val plurals: Map<String, Set<String>>)

    private val resDir: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main/res") }
        .first { it.isDirectory }

    private fun read(folder: String): Resources {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "$folder/strings.xml"))
        fun elements(tag: String) = document.getElementsByTagName(tag).let { list ->
            (0 until list.length).map { list.item(it) as Element }
        }
        return Resources(
            strings = elements("string").associate { it.getAttribute("name") to it.textContent },
            plurals = elements("plurals").associate { plural ->
                val items = plural.getElementsByTagName("item")
                plural.getAttribute("name") to (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }.toSet()
            }
        )
    }

    private val english = read("values")
    private val spanish = read("values-es")

    @Test
    fun `english and spanish define the same string keys`() {
        assertEquals("keys only in English", emptySet<String>(), english.strings.keys - spanish.strings.keys)
        assertEquals("keys only in Spanish", emptySet<String>(), spanish.strings.keys - english.strings.keys)
    }

    @Test
    fun `english and spanish define the same plural keys and quantities`() {
        assertEquals("plurals only in English", emptySet<String>(), english.plurals.keys - spanish.plurals.keys)
        assertEquals("plurals only in Spanish", emptySet<String>(), spanish.plurals.keys - english.plurals.keys)
        english.plurals.forEach { (key, quantities) ->
            assertTrue("$key needs 'other' in English", "other" in quantities)
            assertTrue("$key needs 'other' in Spanish", "other" in spanish.plurals.getValue(key))
        }
    }

    @Test
    fun `both languages use the same format arguments in every string`() {
        val argument = Regex("%\\d+\\$[a-z]")
        english.strings.forEach { (key, value) ->
            val other = spanish.strings[key] ?: return@forEach
            assertEquals("format arguments of $key", argument.findAll(value).map { it.value }.toSet(), argument.findAll(other).map { it.value }.toSet())
        }
    }

    @Test
    fun `no string is blank`() {
        (english.strings + spanish.strings).forEach { (key, value) -> assertTrue("$key is blank", value.isNotBlank()) }
    }
}
