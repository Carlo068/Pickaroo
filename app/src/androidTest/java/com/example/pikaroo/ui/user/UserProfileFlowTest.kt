package com.example.pikaroo.ui.user

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.navigation.AppNavigation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserProfileFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private var onboarding = false
    @Before fun prepare() {
        onboarding = preferences.getBoolean("completed_onboarding")
        preferences.putBoolean("completed_onboarding", true)
        container.sessionRepository.clear()
    }
    @After fun cleanup() {
        container.sessionRepository.clear()
        preferences.putBoolean("completed_onboarding", onboarding)
    }
    private fun start(user: String) {
        runBlocking { container.sessionRepository.save(container.authApi.login(LoginRequest(user, "demo1234"))) }
        container.sessionRepository.setActiveMode(RoleMode.CUSTOMER)
        compose.setContent { AppNavigation() }
    }
    private fun waitProfile() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun customerProfileComesFromAuthenticatedUser() {
        start("customer1"); compose.onNodeWithText("Usuario").performClick(); waitProfile()
        compose.onNodeWithTag("profile_name").assertTextEquals("Cliente Demo")
        compose.onNodeWithTag("profile_email").assertTextEquals("customer1@pickaroo.test")
        compose.onNodeWithTag("profile_roles").assertTextEquals("Cliente")
        compose.onNodeWithText("María González").assertDoesNotExist()
    }
    @Test fun ownerProfileShowsDifferentNameEmailAndBothRoles() {
        start("owner1"); compose.onNodeWithText("Usuario").performClick(); waitProfile()
        compose.onNodeWithTag("profile_name").assertTextEquals("Dueño Demo Uno")
        compose.onNodeWithTag("profile_email").assertTextEquals("owner1@pickaroo.test")
        compose.onNodeWithTag("profile_roles").assertTextEquals("Dueño · Cliente")
        compose.onNodeWithText("Cliente Demo").assertDoesNotExist()
    }
    @Test fun failedRequestShowsRetryAndNeverShowsAnotherUsersData() {
        start("customer1")
        container.mockApi.failNext { MockErrors.conflict("UNAVAILABLE") }
        compose.onNodeWithText("Usuario").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("profile_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("profile_name").assertDoesNotExist()
        compose.onNodeWithTag("profile_retry").performClick(); waitProfile()
        compose.onNodeWithTag("profile_name").assertTextEquals("Cliente Demo")
    }
}
