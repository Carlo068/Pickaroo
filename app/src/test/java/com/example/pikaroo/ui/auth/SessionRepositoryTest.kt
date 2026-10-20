package com.example.pikaroo.ui.auth

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.model.RoleAssignment
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.network.MockAuthService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {

    private val storage = InMemoryStringStore()
    private var now = 1_000_000L

    private val auth = MockAuthService(
        api = MockPickarooApi(MockStore(FileSeedSource(), InMemoryStringStore()), latencyMillis = { 0L }),
        nowSeconds = { now }
    )

    private fun newRepository() = SessionRepository(storage)
    private val repository = newRepository()

    private suspend fun login(username: String): LoginResponse =
        auth.login(LoginRequest(username, "demo1234")).also { repository.save(it) }

    private val customerPermissions = setOf(
        Permission.CATALOG_READ, Permission.CART_USE, Permission.ORDER_CREATE,
        Permission.ORDER_READ_OWN, Permission.ORDER_CANCEL_OWN
    )
    private val staffPermissions = setOf(
        Permission.CATALOG_READ, Permission.SHOP_ORDERS_READ,
        Permission.ORDER_STATUS_UPDATE, Permission.PRODUCT_STOCK_UPDATE
    )
    private val ownerPermissions = staffPermissions + setOf(
        Permission.PRODUCT_MANAGE, Permission.SLOTS_MANAGE, Permission.STAFF_MANAGE, Permission.SHOP_STATS_READ
    )
    private val driverPermissions = setOf(
        Permission.CATALOG_READ, Permission.DELIVERY_READ_AVAILABLE, Permission.DELIVERY_ACCEPT,
        Permission.DELIVERY_UPDATE, Permission.LOCATION_WRITE
    )

    private fun assertPermissions(expected: Set<String>) {
        Permission.all.forEach { permission ->
            assertEquals("permission $permission", permission in expected, repository.can(permission))
        }
    }

    @Test
    fun `starts logged out`() {
        assertFalse(repository.isLoggedIn())
        assertNull(repository.session())
        assertEquals("", repository.accessToken())
        assertEquals("", repository.username())
        assertFalse(repository.can(Permission.CATALOG_READ))
        assertTrue(repository.availableModes().isEmpty())
        assertNull(repository.activeMode())
    }

    @Test
    fun `customer has exactly the customer permissions`() = runTest {
        login("customer1")

        assertPermissions(customerPermissions)
    }

    @Test
    fun `staff has exactly the staff permissions`() = runTest {
        login("staff1")

        assertPermissions(staffPermissions)
    }

    @Test
    fun `owner has exactly the owner permissions`() = runTest {
        login("owner2")

        assertPermissions(ownerPermissions)
    }

    @Test
    fun `driver has exactly the driver permissions`() = runTest {
        login("driver1")

        assertPermissions(driverPermissions)
    }

    @Test
    fun `owner who is also a customer has both sets of permissions`() = runTest {
        login("owner1")

        assertPermissions(ownerPermissions + customerPermissions)
    }

    @Test
    fun `saved session is restored by a new instance`() = runTest {
        val response = login("owner1")

        val restored = newRepository().session()!!

        assertEquals(response.access, restored.accessToken)
        assertEquals(response.refresh, restored.refreshToken)
        assertEquals(response.user, restored.user)
        assertEquals(response.roles, restored.roles)
        assertEquals(response.permissions.toSet(), restored.permissions)
        assertTrue(newRepository().isLoggedIn())
    }

    @Test
    fun `user details and accents are kept`() = runTest {
        login("owner1")

        val repository = newRepository()

        assertEquals("user_3", repository.userId())
        assertEquals("owner1", repository.username())
        assertEquals("Dueño Demo Uno", repository.session()!!.user.name)
        assertEquals(RoleAssignment("owner", "shop_1"), repository.roles().first())
    }

    @Test
    fun `clear logs the user out for every instance`() = runTest {
        login("customer1")

        repository.clear()

        assertFalse(repository.isLoggedIn())
        assertFalse(newRepository().isLoggedIn())
        assertTrue(storage.data.isEmpty())
    }

    @Test
    fun `updateTokens replaces only the tokens`() = runTest {
        val response = login("driver1")

        repository.updateTokens("new-access", "new-refresh")

        assertEquals("new-access", repository.accessToken())
        assertEquals("new-refresh", newRepository().refreshToken())
        assertEquals(response.user, repository.session()!!.user)
        assertTrue(repository.can(Permission.DELIVERY_ACCEPT))
    }

    @Test
    fun `updateTokens without a session does nothing`() {
        repository.updateTokens("a", "b")

        assertFalse(repository.isLoggedIn())
        assertTrue(storage.data.isEmpty())
    }

    @Test
    fun `access token expiry follows the token claims`() = runTest {
        login("customer1")

        assertFalse(repository.isAccessTokenExpired(now))
        assertFalse(repository.isAccessTokenExpired(now + 15 * 60 - 1))
        assertTrue(repository.isAccessTokenExpired(now + 15 * 60))
    }

    @Test
    fun `without a session the token counts as expired`() {
        assertTrue(repository.isAccessTokenExpired(now))
    }

    @Test
    fun `modes are derived from the roles in priority order`() = runTest {
        login("customer1")
        assertEquals(listOf(RoleMode.CUSTOMER), repository.availableModes())

        login("staff1")
        assertEquals(listOf(RoleMode.SHOP), repository.availableModes())

        login("driver1")
        assertEquals(listOf(RoleMode.DRIVER), repository.availableModes())

        login("owner1")
        assertEquals(listOf(RoleMode.SHOP, RoleMode.CUSTOMER), repository.availableModes())
    }

    @Test
    fun `active mode defaults to the highest priority mode`() = runTest {
        login("owner1")

        assertEquals(RoleMode.SHOP, repository.activeMode())
    }

    @Test
    fun `active mode can be switched and survives a restart`() = runTest {
        login("owner1")

        repository.setActiveMode(RoleMode.CUSTOMER)

        assertEquals(RoleMode.CUSTOMER, repository.activeMode())
        assertEquals(RoleMode.CUSTOMER, newRepository().activeMode())
    }

    @Test
    fun `a mode without a matching role is rejected`() = runTest {
        login("customer1")

        assertThrows(IllegalArgumentException::class.java) { repository.setActiveMode(RoleMode.DRIVER) }
        assertEquals(RoleMode.CUSTOMER, repository.activeMode())
    }

    @Test
    fun `a new login resets the active mode`() = runTest {
        login("owner1")
        repository.setActiveMode(RoleMode.CUSTOMER)

        login("owner1")

        assertEquals(RoleMode.SHOP, repository.activeMode())
    }

    @Test
    fun `damaged stored data counts as logged out and is cleaned`() = runTest {
        login("customer1")
        storage.put("session_roles", "{not json")

        val restored = newRepository()

        assertFalse(restored.isLoggedIn())
        assertTrue(storage.data.isEmpty())
    }
}
