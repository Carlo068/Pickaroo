package com.example.pikaroo.ui.auth

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.mock.toApiError
import com.example.pikaroo.ui.auth.data.JwtPayload
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LogoutRequest
import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.model.RegisterRequest
import com.example.pikaroo.ui.auth.model.RoleAssignment
import com.example.pikaroo.ui.auth.network.MockAuthService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class MockAuthServiceTest {

    private val storage = InMemoryStringStore()
    private var now = 1_000_000L
    private var tokenCounter = 0

    private fun newService() = MockAuthService(
        api = MockPickarooApi(MockStore(FileSeedSource(), storage), latencyMillis = { 0L }),
        nowSeconds = { now },
        newToken = { "t${++tokenCounter}" }
    )

    private val service = newService()

    private suspend fun expectError(status: Int, code: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: HttpException) {
            assertEquals(status, e.code())
            assertEquals(code, e.toApiError()!!.code)
            return
        }
        throw AssertionError("Expected HTTP $status $code")
    }

    private suspend fun login(username: String = "customer1", password: String = "demo1234") =
        service.login(LoginRequest(username, password))

    @Test
    fun `login returns tokens, user, roles and permissions`() = runTest {
        val response = login()

        assertEquals("user_1", response.user.id)
        assertEquals("customer1", response.user.username)
        assertEquals("Cliente Demo", response.user.name)
        assertEquals(listOf(RoleAssignment("customer")), response.roles)
        assertTrue("cart:use" in response.permissions)
        assertFalse("shop:orders:read" in response.permissions)
        assertTrue(response.refresh.startsWith("rt_"))
    }

    @Test
    fun `multi role users get the union of permissions and shop scoped roles`() = runTest {
        val response = login("owner1")

        assertEquals(listOf(RoleAssignment("owner", "shop_1"), RoleAssignment("customer")), response.roles)
        assertTrue("product:manage" in response.permissions)
        assertTrue("cart:use" in response.permissions)
        assertEquals(response.permissions.size, response.permissions.toSet().size)
    }

    @Test
    fun `driver gets delivery permissions only`() = runTest {
        val response = login("driver1")

        assertTrue("delivery:accept" in response.permissions)
        assertTrue("location:write" in response.permissions)
        assertFalse("order:create" in response.permissions)
    }

    @Test
    fun `login ignores case and surrounding spaces in the username`() = runTest {
        assertEquals("user_1", login("  CUSTOMER1 ").user.id)
    }

    @Test
    fun `wrong password and unknown user fail the same way`() = runTest {
        expectError(401, "INVALID_CREDENTIALS") { login(password = "nope") }
        expectError(401, "INVALID_CREDENTIALS") { login(username = "ghost") }
    }

    @Test
    fun `blank credentials are a bad request`() = runTest {
        expectError(400, "MISSING_FIELDS") { login(username = " ") }
        expectError(400, "MISSING_FIELDS") { login(password = "") }
    }

    @Test
    fun `access token has the shape and claims of a short lived jwt`() = runTest {
        val access = login("owner1").access

        val payload = JwtPayload.decode(access)!!
        assertEquals(3, access.split('.').size)
        assertTrue(access.endsWith(".mock"))
        assertEquals("user_3", payload.subject)
        assertEquals(listOf("owner", "customer"), payload.roles)
        assertEquals(now, payload.issuedAt)
        assertEquals(15 * 60L, payload.expiresAt - payload.issuedAt)
    }

    @Test
    fun `me returns the user behind a valid token`() = runTest {
        val me = service.me("Bearer ${login("staff1").access}")

        assertEquals("user_2", me.user.id)
        assertEquals(listOf(RoleAssignment("staff", "shop_1")), me.roles)
        assertTrue("order:status:update" in me.permissions)
    }

    @Test
    fun `me rejects expired, malformed and missing tokens`() = runTest {
        val access = login().access

        now += 15 * 60
        expectError(401, "TOKEN_EXPIRED") { service.me("Bearer $access") }
        expectError(401, "INVALID_TOKEN") { service.me("Bearer garbage") }
        expectError(401, "INVALID_TOKEN") { service.me("Bearer ${access.substringBeforeLast('.')}.forged") }
        expectError(401, "UNAUTHORIZED") { service.me(access) }
    }

    @Test
    fun `me works right before the token expires`() = runTest {
        val access = login().access

        now += 15 * 60 - 1

        assertEquals("user_1", service.me("Bearer $access").user.id)
    }

    @Test
    fun `refresh rotates both tokens and revokes the old refresh`() = runTest {
        val first = login()
        now += 60

        val second = service.refresh(RefreshRequest(first.refresh))

        assertNotEquals(first.access, second.access)
        assertNotEquals(first.refresh, second.refresh)
        assertEquals(now, JwtPayload.decode(second.access)!!.issuedAt)
        expectError(401, "INVALID_REFRESH_TOKEN") { service.refresh(RefreshRequest(first.refresh)) }
        service.refresh(RefreshRequest(second.refresh))
    }

    @Test
    fun `refresh fails for unknown and expired tokens`() = runTest {
        val session = login()

        expectError(401, "INVALID_REFRESH_TOKEN") { service.refresh(RefreshRequest("rt_unknown")) }

        now += 7 * 24 * 60 * 60
        expectError(401, "REFRESH_EXPIRED") { service.refresh(RefreshRequest(session.refresh)) }
    }

    @Test
    fun `logout revokes the refresh token`() = runTest {
        val session = login()

        service.logout("Bearer ${session.access}", LogoutRequest(session.refresh))

        expectError(401, "INVALID_REFRESH_TOKEN") { service.refresh(RefreshRequest(session.refresh)) }
    }

    @Test
    fun `logout needs a valid access token`() = runTest {
        val session = login()
        now += 15 * 60

        expectError(401, "TOKEN_EXPIRED") { service.logout("Bearer ${session.access}", LogoutRequest(session.refresh)) }
        service.refresh(RefreshRequest(session.refresh))
    }

    @Test
    fun `logout cannot revoke another user's refresh token`() = runTest {
        val customer = login()
        val driver = login("driver1")

        expectError(403, "FORBIDDEN") { service.logout("Bearer ${driver.access}", LogoutRequest(customer.refresh)) }
        service.refresh(RefreshRequest(customer.refresh))
    }

    @Test
    fun `logout with an unknown refresh token does nothing`() = runTest {
        val session = login()

        service.logout("Bearer ${session.access}", LogoutRequest("rt_unknown"))

        service.refresh(RefreshRequest(session.refresh))
    }

    @Test
    fun `register creates a customer and starts a session`() = runTest {
        val response = service.register(RegisterRequest("Ana Pérez", "ana", "ana@pickaroo.test", "secret1"))

        assertEquals("user_6", response.user.id)
        assertEquals("Ana Pérez", response.user.name)
        assertEquals(listOf(RoleAssignment("customer")), response.roles)
        assertTrue("order:create" in response.permissions)
        assertEquals("user_6", service.me("Bearer ${response.access}").user.id)
    }

    @Test
    fun `registered users can log in later, even after a restart`() = runTest {
        service.register(RegisterRequest("Ana Pérez", "ana", "ana@pickaroo.test", "secret1"))

        val afterRestart = newService().login(LoginRequest("ANA", "secret1"))

        assertEquals("Ana Pérez", afterRestart.user.name)
        assertEquals(listOf("customer"), JwtPayload.decode(afterRestart.access)!!.roles)
    }

    @Test
    fun `register rejects taken usernames and emails`() = runTest {
        expectError(409, "USERNAME_TAKEN") {
            service.register(RegisterRequest("Otro", "Customer1", "otro@pickaroo.test", "secret1"))
        }
        expectError(409, "EMAIL_TAKEN") {
            service.register(RegisterRequest("Otro", "otro", "CUSTOMER1@pickaroo.test", "secret1"))
        }
    }

    @Test
    fun `register validates every field`() = runTest {
        expectError(400, "INVALID_NAME") { service.register(RegisterRequest(" ", "ana", "ana@pickaroo.test", "secret1")) }
        expectError(400, "INVALID_USERNAME") { service.register(RegisterRequest("Ana", "an", "ana@pickaroo.test", "secret1")) }
        expectError(400, "INVALID_USERNAME") { service.register(RegisterRequest("Ana", "a na", "ana@pickaroo.test", "secret1")) }
        expectError(400, "INVALID_EMAIL") { service.register(RegisterRequest("Ana", "ana", "not-an-email", "secret1")) }
        expectError(400, "WEAK_PASSWORD") { service.register(RegisterRequest("Ana", "ana", "ana@pickaroo.test", "12345")) }
    }

    @Test
    fun `failed registrations do not create users`() = runTest {
        runCatching { service.register(RegisterRequest("Ana", "ana", "bad", "secret1")) }

        expectError(401, "INVALID_CREDENTIALS") { login("ana", "secret1") }
    }
}
