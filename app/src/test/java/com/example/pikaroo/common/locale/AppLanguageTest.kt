package com.example.pikaroo.common.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `each language maps to its tags`() {
        assertEquals(emptyList<String>(), AppLanguage.SYSTEM.toLanguageTags())
        assertEquals(listOf("es"), AppLanguage.SPANISH.toLanguageTags())
        assertEquals(listOf("en"), AppLanguage.ENGLISH.toLanguageTags())
    }

    @Test
    fun `tags map back to the language`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(emptyList()))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTags(listOf("es")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags(listOf("en")))
    }

    @Test
    fun `every language survives a round trip`() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromLanguageTags(it.toLanguageTags())) }
    }

    @Test
    fun `unknown tags read as system default`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(listOf("fr")))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(listOf("")))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(listOf("zz-ES")))
    }

    @Test
    fun `regional variants and case read as their language`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTags(listOf("es-MX")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags(listOf("EN-us")))
    }

    @Test
    fun `the first tag decides`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags(listOf("en", "es")))
    }
}
