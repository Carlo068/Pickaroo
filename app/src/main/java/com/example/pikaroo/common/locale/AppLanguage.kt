package com.example.pikaroo.common.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** The language the user chose for the app; [SYSTEM] follows the phone. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en");

    /** The language tags to hand to the system; empty means "no app language, follow the phone". */
    fun toLanguageTags(): List<String> = listOfNotNull(tag)

    companion object {
        /** The first tag decides, by language only ("es-MX" is Spanish); anything unknown is [SYSTEM]. */
        fun fromLanguageTags(tags: List<String>): AppLanguage {
            val language = tags.firstOrNull()?.substringBefore('-')?.lowercase() ?: return SYSTEM
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
        }
    }
}

/** Reads and writes the app language. An interface so ViewModels and tests can use a fake. */
interface AppLanguageStore {
    fun current(): AppLanguage

    /** Applies the language at once; the system recreates the running activities. */
    fun set(language: AppLanguage)
}

/** Backed by AppCompat's per-app locales (system setting on Android 13+, AppCompat storage below). */
class AppCompatLanguageStore : AppLanguageStore {

    override fun current(): AppLanguage {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return AppLanguage.fromLanguageTags(tags.split(',').filter { it.isNotBlank() })
    }

    override fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.toLanguageTags().joinToString(","))
        )
    }
}
