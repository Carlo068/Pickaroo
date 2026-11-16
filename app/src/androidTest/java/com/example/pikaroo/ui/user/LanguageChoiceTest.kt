package com.example.pikaroo.ui.user

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.MainActivity
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.locale.AppCompatLanguageStore
import com.example.pikaroo.common.locale.AppLanguage
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.user.view.UserTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The language setting on the Usuario screen. Runs in the real [MainActivity] because choosing a
 * language recreates the activity. The runner's Spanish pin only applies while the language is
 * "System default", so every test puts it back there in @After.
 */
class LanguageChoiceTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private val languages = AppCompatLanguageStore()
    private var onboardingWasCompleted = false
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        onboardingWasCompleted = preferences.getBoolean(ONBOARDING_KEY)
        preferences.putBoolean(ONBOARDING_KEY, true)
        container.sessionRepository.clear()
        runBlocking { container.sessionRepository.save(container.authApi.login(LoginRequest("customer1", "demo1234"))) }
        container.sessionRepository.setActiveMode(RoleMode.CUSTOMER)
        runOnMain { languages.set(AppLanguage.SYSTEM) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        // Close first so no activity is recreated mid-teardown, then clear the app language.
        scenario?.close()
        runOnMain { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
        container.sessionRepository.clear()
        preferences.putBoolean(ONBOARDING_KEY, onboardingWasCompleted)
    }

    private fun runOnMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openLanguageDialog() {
        composeRule.onNodeWithText("Usuario").performClick()
        waitForTag(UserTestTags.LANGUAGE_ROW)
        composeRule.onNodeWithTag(UserTestTags.LANGUAGE_ROW).performScrollTo().performClick()
        waitForTag(UserTestTags.LANGUAGE_DIALOG)
    }

    private fun choose(language: AppLanguage) {
        composeRule.onNodeWithTag(UserTestTags.languageOption(language)).performClick()
    }

    @Test
    fun theDialogListsTheThreeOptionsWithTheCurrentOneSelected() {
        openLanguageDialog()

        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.SYSTEM)).assertIsSelected()
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.SPANISH)).assertIsNotSelected()
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.ENGLISH)).assertIsNotSelected()
        // Read the labels from the options: the language row behind the dialog shows the current one too.
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.SYSTEM)).assertTextEquals("Predeterminado del sistema")
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.SPANISH)).assertTextEquals("Español")
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.ENGLISH)).assertTextEquals("English")
    }

    @Test
    fun choosingEnglishShowsEnglishOnTheSameScreenAndKeepsTheSession() {
        openLanguageDialog()
        choose(AppLanguage.ENGLISH)

        waitForText("Log out")
        // Same screen (the setting row is on the Usuario screen), nothing lost.
        waitForTag(UserTestTags.LANGUAGE_ROW)
        composeRule.onNodeWithText("Account").assertExists()
        assertTrue(container.sessionRepository.isLoggedIn())
        assertEquals(RoleMode.CUSTOMER, container.sessionRepository.activeMode())
        assertEquals(AppLanguage.ENGLISH, languages.current())

        composeRule.onNodeWithTag(UserTestTags.LANGUAGE_ROW).performScrollTo().performClick()
        waitForTag(UserTestTags.LANGUAGE_DIALOG)
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.ENGLISH)).assertIsSelected()
        composeRule.onNodeWithTag(UserTestTags.languageOption(AppLanguage.SYSTEM)).assertTextEquals("System default")
    }

    @Test
    fun choosingSpanishGoesBackToSpanish() {
        openLanguageDialog()
        choose(AppLanguage.ENGLISH)
        waitForText("Log out")

        composeRule.onNodeWithTag(UserTestTags.LANGUAGE_ROW).performScrollTo().performClick()
        waitForTag(UserTestTags.LANGUAGE_DIALOG)
        choose(AppLanguage.SPANISH)

        waitForText("Cerrar sesión")
        waitForTag(UserTestTags.LANGUAGE_ROW)
        assertEquals(AppLanguage.SPANISH, languages.current())
        assertTrue(container.sessionRepository.isLoggedIn())
    }

    @Test
    fun choosingSystemDefaultClearsTheAppLanguage() {
        openLanguageDialog()
        choose(AppLanguage.ENGLISH)
        waitForText("Log out")
        assertEquals(AppLanguage.ENGLISH, languages.current())

        composeRule.onNodeWithTag(UserTestTags.LANGUAGE_ROW).performScrollTo().performClick()
        waitForTag(UserTestTags.LANGUAGE_DIALOG)
        choose(AppLanguage.SYSTEM)

        // Back to following the phone; the runner pins the test process to Spanish again.
        waitForText("Cerrar sesión")
        assertEquals(AppLanguage.SYSTEM, languages.current())
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
        assertTrue(container.sessionRepository.isLoggedIn())
    }

    private companion object {
        const val ONBOARDING_KEY = "completed_onboarding"
    }
}
