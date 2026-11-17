package com.example.pikaroo

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Proves that [PikarooTestRunner] pins the app to Spanish, so the other tests can look for Spanish text. */
@RunWith(AndroidJUnit4::class)
class SpanishPinTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appContextResourcesAreSpanish() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Cerrar sesión", context.getString(R.string.common_logout))
    }

    @Test
    fun composeTextIsSpanish() {
        composeRule.setContent { Text(stringResource(R.string.login_welcome)) }
        composeRule.onNodeWithText("Bienvenido").assertIsDisplayed()
    }
}
