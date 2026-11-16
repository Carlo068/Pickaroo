package com.example.pikaroo.ui.shop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.navigation.AppNavigation
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.shop.view.ShopTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class ShopOrdersFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private var onboarding = false
    private val originals = mutableListOf<com.google.gson.JsonObject>()

    @Before fun prepare() = runBlocking {
        onboarding = preferences.getBoolean("completed_onboarding")
        preferences.putBoolean("completed_onboarding", true)
        container.sessionRepository.clear()
        container.mockApi.call {
            for (id in listOf("order_1001", "order_1004")) {
                val doc = find(ORDERS, id)!!; originals.add(doc.deepCopy())
                doc.addProperty("status", "PLACED"); put(ORDERS, doc)
            }
        }
        container.sessionRepository.save(container.authApi.login(LoginRequest("staff1", "demo1234")))
    }
    @After fun cleanup() = runBlocking {
        container.mockApi.call { originals.forEach { put(ORDERS, it) } }
        container.sessionRepository.clear()
        preferences.putBoolean("completed_onboarding", onboarding)
    }
    private fun waitFor(id: String, status: OrderStatus) {
        val tag = ShopTestTags.order(id, status)
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun staffAdvancesPickupAndCustomerReadsTheSameHistory() {
        compose.setContent { AppNavigation() }
        waitFor("order_1001", OrderStatus.PLACED)
        for (status in listOf(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY)) {
            compose.onNodeWithTag(ShopTestTags.advance("order_1001")).performScrollTo().performClick()
            // With grouping, the row can move off screen; the service is the source of truth.
            compose.waitUntil(10_000) {
                runBlocking { container.shopOrdersRepository.orders().first { it.id == "order_1001" }.status == status }
            }
            compose.onNodeWithTag("shop_orders_list").performScrollToNode(hasTestTag(ShopTestTags.order("order_1001", status)))
            compose.onNodeWithTag(ShopTestTags.order("order_1001", status)).assertIsDisplayed()
        }
        compose.onNodeWithTag(ShopTestTags.advance("order_1001")).performScrollTo().performClick()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithTag(ShopTestTags.order("order_1001", OrderStatus.READY)).assertExists()
        compose.onNodeWithTag(ShopTestTags.advance("order_1001")).performScrollTo().performClick()
        compose.onNodeWithText("Confirmar", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) {
            runBlocking { container.shopOrdersRepository.orders().first { it.id == "order_1001" }.status == OrderStatus.PICKED_UP }
        }
        runBlocking {
            container.sessionRepository.save(container.authApi.login(LoginRequest("customer1", "demo1234")))
            assertEquals(OrderStatus.PICKED_UP, container.ordersRepository.get("order_1001").status)
            assertEquals(OrderStatus.PICKED_UP, container.ordersRepository.events("order_1001").last().status)
        }
    }

    @Test fun pollingDisplaysExternalStatusChanges() {
        compose.setContent { AppNavigation() }
        waitFor("order_1001", OrderStatus.PLACED)
        runBlocking {
            container.mockApi.call {
                val doc = find(ORDERS, "order_1001")!!
                doc.addProperty("status", "ACCEPTED"); put(ORDERS, doc)
            }
        }
        compose.waitUntil(12_000) {
            runCatching {
                compose.onNodeWithTag("shop_orders_list").performScrollToNode(hasTestTag(ShopTestTags.order("order_1001", OrderStatus.ACCEPTED)))
                true
            }.getOrDefault(false)
        }
        waitFor("order_1001", OrderStatus.ACCEPTED)
    }

    @Test fun shopTwoCannotSeeShopOne() {
        runBlocking { container.sessionRepository.save(container.authApi.login(LoginRequest("owner2", "demo1234"))) }
        compose.setContent { AppNavigation() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Pedidos de la sucursal").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(ShopTestTags.order("order_1001", OrderStatus.PLACED)).assertDoesNotExist()
        runBlocking {
            val list = container.shopOrdersRepository.orders()
            org.junit.Assert.assertTrue(list.all { it.shopId == "shop_2" })
        }
    }
}
