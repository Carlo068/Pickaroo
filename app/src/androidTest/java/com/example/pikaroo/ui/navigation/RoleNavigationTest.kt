package com.example.pikaroo.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.model.RoleMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RoleNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private var onboardingWasCompleted = false

    @Before
    fun setUp() {
        onboardingWasCompleted = preferences.getBoolean(ONBOARDING_KEY)
        preferences.putBoolean(ONBOARDING_KEY, true)
        container.sessionRepository.clear()
    }

    @After
    fun tearDown() {
        container.sessionRepository.clear()
        preferences.putBoolean(ONBOARDING_KEY, onboardingWasCompleted)
    }

    private fun startSessionAs(username: String) = runBlocking {
        container.sessionRepository.save(container.authApi.login(LoginRequest(username, "demo1234")))
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun assertOnlyHome(tag: String) {
        listOf(NavTestTags.CUSTOMER_HOME, NavTestTags.SHOP_HOME, NavTestTags.DRIVER_HOME).forEach {
            if (it == tag) composeRule.onNodeWithTag(it).assertIsDisplayed()
            else composeRule.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    private fun relaunchAs(username: String, expectedHome: String) {
        startSessionAs(username)
        composeRule.setContent { AppNavigation() }

        waitForTag(expectedHome)
        assertOnlyHome(expectedHome)
    }

    private fun loginAs(username: String, expectedHome: String) {
        composeRule.setContent { AppNavigation() }
        waitForTag("login_submit")

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(username)
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("demo1234")
        composeRule.onNodeWithTag("login_submit").performClick()

        waitForTag(expectedHome)
        assertOnlyHome(expectedHome)
    }

    @Test
    fun customerRelaunchLandsOnCustomerHome() = relaunchAs("customer1", NavTestTags.CUSTOMER_HOME)

    @Test
    fun staffRelaunchLandsOnShopHome() = relaunchAs("staff1", NavTestTags.SHOP_HOME)

    @Test
    fun ownerRelaunchLandsOnShopHome() = relaunchAs("owner1", NavTestTags.SHOP_HOME)

    @Test
    fun driverRelaunchLandsOnDriverHome() = relaunchAs("driver1", NavTestTags.DRIVER_HOME)

    @Test
    fun customerLoginLandsOnCustomerHome() = loginAs("customer1", NavTestTags.CUSTOMER_HOME)

    @Test
    fun staffLoginLandsOnShopHome() = loginAs("staff1", NavTestTags.SHOP_HOME)

    @Test
    fun ownerLoginLandsOnShopHome() = loginAs("owner1", NavTestTags.SHOP_HOME)

    @Test
    fun driverLoginLandsOnDriverHome() = loginAs("driver1", NavTestTags.DRIVER_HOME)

    @Test
    fun withoutASessionTheAppStartsOnLogin() {
        composeRule.setContent { AppNavigation() }

        waitForTag("login_submit")
        assertOnlyHome("none")
    }

    @Test
    fun ownerCanSwitchToCustomerModeAndBack() {
        relaunchAs("owner1", NavTestTags.SHOP_HOME)

        composeRule.onNodeWithTag(NavTestTags.switchTo(RoleMode.CUSTOMER)).performClick()
        waitForTag(NavTestTags.CUSTOMER_HOME)
        assertOnlyHome(NavTestTags.CUSTOMER_HOME)
        assertEquals(RoleMode.CUSTOMER, container.sessionRepository.activeMode())

        composeRule.onNodeWithText("Usuario").performClick()
        composeRule.onNodeWithTag(NavTestTags.switchTo(RoleMode.SHOP)).performScrollTo().performClick()
        waitForTag(NavTestTags.SHOP_HOME)
        assertOnlyHome(NavTestTags.SHOP_HOME)
        assertEquals(RoleMode.SHOP, container.sessionRepository.activeMode())
    }

    @Test
    fun chosenModeIsKeptWhenTheAppRelaunches() {
        startSessionAs("owner1")
        container.sessionRepository.setActiveMode(RoleMode.CUSTOMER)

        composeRule.setContent { AppNavigation() }

        waitForTag(NavTestTags.CUSTOMER_HOME)
        assertOnlyHome(NavTestTags.CUSTOMER_HOME)
    }

    @Test
    fun singleRoleUsersGetNoSwitchButtons() {
        relaunchAs("driver1", NavTestTags.DRIVER_HOME)

        RoleMode.entries.forEach { composeRule.onNodeWithTag(NavTestTags.switchTo(it)).assertDoesNotExist() }
    }

    @Test
    fun logoutFromDriverHomeReturnsToLoginAndClearsTheSession() {
        relaunchAs("driver1", NavTestTags.DRIVER_HOME)

        composeRule.onNodeWithText("Cerrar sesión").performClick()

        waitForTag("login_submit")
        assertOnlyHome("none")
        composeRule.waitUntil(5_000) { !container.sessionRepository.isLoggedIn() }
        assertFalse(container.sessionRepository.isLoggedIn())
    }

    @Test
    fun guardShowsContentOnlyWithThePermission() {
        startSessionAs("driver1")
        var denied = 0
        composeRule.setContent {
            RequirePermission(container.sessionRepository, Permission.DELIVERY_ACCEPT, onDenied = { denied++ }) {
                Text("allowed")
            }
            RequirePermission(container.sessionRepository, Permission.CART_USE, onDenied = { denied++ }) {
                Text("forbidden")
            }
        }

        composeRule.onNodeWithText("allowed").assertIsDisplayed()
        composeRule.onNodeWithText("forbidden").assertDoesNotExist()
        composeRule.waitUntil(5_000) { denied == 1 }
        assertTrue(denied == 1)
    }

    private companion object {
        const val ONBOARDING_KEY = "completed_onboarding"
    }
}
