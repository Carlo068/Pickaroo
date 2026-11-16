package com.example.pikaroo.ui.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockCollections.USERS
import com.example.pikaroo.common.mock.MockCollections.SESSIONS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.navigation.AppNavigation
import com.example.pikaroo.ui.navigation.NavTestTags
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class RegistrationFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private var onboarding = false
    private val username = "test_" + UUID.randomUUID().toString().take(8)
    private var originalUsers = emptySet<String>()
    private var originalSessions = emptySet<String>()

    @Before fun prepare() {
        onboarding = preferences.getBoolean("completed_onboarding")
        preferences.putBoolean("completed_onboarding", true)
        container.sessionRepository.clear()
        runBlocking { container.mockApi.call {
            originalUsers = all(USERS).map { it.get("id").asString }.toSet()
            originalSessions = all(SESSIONS).map { it.get("id").asString }.toSet()
        } }
        compose.setContent { AppNavigation() }
        compose.onNodeWithText("Registrarse").performClick()
    }
    @After fun cleanup() {
        container.sessionRepository.clear()
        runBlocking { container.mockApi.call {
            all(SESSIONS).filter { it.get("id").asString !in originalSessions }.forEach { remove(SESSIONS, it.get("id").asString) }
            all(USERS).filter { it.get("id").asString !in originalUsers }.forEach { remove(USERS, it.get("id").asString) }
        } }
        preferences.putBoolean("completed_onboarding", onboarding)
    }
    private fun field(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(text)
    }
    private fun fill(user: String = username, email: String = "$username@example.test", password: String = "secret1", confirm: String = password) {
        field("register_name", "Ana Demo")
        field("register_username", user)
        field("register_email", email)
        field("register_password", password)
        field("register_confirm", confirm)
    }
    private fun submit() { compose.onNodeWithTag("login_submit").performScrollTo().performClick() }
    private fun error(message: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("auth_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("auth_error").performScrollTo().assertTextEquals(message)
        assertFalse(container.sessionRepository.isLoggedIn())
        compose.onNodeWithTag(NavTestTags.CUSTOMER_HOME).assertDoesNotExist()
    }

    @Test fun emptyFieldsShowRequiredMessage() { submit(); error("Completa todos los campos") }
    @Test fun invalidUsernameShowsMessage() { fill(user = "a b"); submit(); error("El usuario debe tener entre 3 y 64 caracteres, sin espacios") }
    @Test fun invalidEmailShowsMessage() { fill(email = "bad"); submit(); error("Ingresa un correo electrónico válido") }
    @Test fun shortPasswordShowsMessage() { fill(password = "12345"); submit(); error("La contraseña debe tener al menos 6 caracteres") }
    @Test fun mismatchedPasswordsShowMessage() { fill(confirm = "other12"); submit(); error("Las contraseñas no coinciden") }
    @Test fun takenUsernameShowsServerMessage() { fill(user = "customer1"); submit(); error("El nombre de usuario ya está en uso") }
    @Test fun takenEmailShowsServerMessage() { fill(email = "customer1@pickaroo.test"); submit(); error("El correo electrónico ya está en uso") }
    @Test fun serverFailureKeepsFormAndAllowsRetry() {
        fill()
        container.mockApi.failNext { MockErrors.conflict("UNAVAILABLE") }
        submit(); error("No se pudo crear la cuenta. Intenta de nuevo.")
        compose.onNodeWithTag("register_username").performScrollTo().assertTextContains(username)
        submit()
        compose.waitUntil(10_000) { container.sessionRepository.isLoggedIn() }
    }
    @Test fun registeredCustomerCanLogOutAndLogInWithNewCredentials() {
        fill(); submit()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(NavTestTags.CUSTOMER_HOME).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(username, container.sessionRepository.username())
        assertEquals(listOf("customer"), container.sessionRepository.roles().map { it.role })
        assertEquals(RoleMode.CUSTOMER, container.sessionRepository.activeMode())
        compose.onNodeWithText("Usuario").performClick()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("login_username").fetchSemanticsNodes().isNotEmpty() }
        field("login_username", username); field("login_password", "secret1"); submit()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(NavTestTags.CUSTOMER_HOME).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(username, container.sessionRepository.username())
    }
}
