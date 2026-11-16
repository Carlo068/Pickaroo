package com.example.pikaroo.common.text

import android.content.Context
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** [UiText] resolution against the real resources, in both languages. */
class UiTextTest {

    private fun contextIn(language: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale(language))
        return base.createConfigurationContext(configuration)
    }

    private val spanish get() = contextIn("es")
    private val english get() = contextIn("en")

    @Test
    fun resResolvesInTheContextLanguage() {
        assertEquals("Cerrar sesión", UiText.Res(R.string.common_logout).asString(spanish))
        assertEquals("Log out", UiText.Res(R.string.common_logout).asString(english))
    }

    @Test
    fun resAcceptsAListOrInlineArguments() {
        val code = 500
        assertEquals(
            UiText.Res.of(R.string.login_error_generic, code).asString(english),
            UiText.Res(R.string.login_error_generic, listOf(code)).asString(english)
        )
        assertEquals(true, UiText.Res.of(R.string.login_error_generic, code).asString(english).contains("500"))
    }

    @Test
    fun nestedUiTextArgumentsAreResolvedInTheSameLanguage() {
        val nested = UiText.Res.of(R.string.products_error, UiText.Res(R.string.common_logout))
        assertEquals("Error: Cerrar sesión", nested.asString(spanish))
        assertEquals("Error: Log out", nested.asString(english))
    }

    @Test
    fun rawIsReturnedAsIs() {
        assertEquals("Calle 5 #12", UiText.Raw("Calle 5 #12").asString(spanish))
    }

    @Test
    fun pluralPicksTheQuantityAndUsesTheCountAsArgument() {
        assertEquals("3 artículos", UiText.Plural(R.plurals.cart_items_count, 3).asString(spanish))
        assertEquals("3 items", UiText.Plural(R.plurals.cart_items_count, 3).asString(english))
    }

    @Test
    fun spanishPluralOfOneIsTheSingular() {
        assertEquals("1 artículo", UiText.Plural(R.plurals.cart_items_count, 1).asString(spanish))
        assertEquals("1 item", UiText.Plural(R.plurals.cart_items_count, 1).asString(english))
    }
}
