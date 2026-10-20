package com.example.pikaroo.ui.auth

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.auth.data.JwtPayload
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LogoutRequest
import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.model.RefreshResponse
import com.example.pikaroo.ui.auth.network.AuthApi
import com.example.pikaroo.ui.auth.network.MockAuthService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class AuthorizedCallsTest {

    private var now = 1_000_000L
    private var tokenCounter = 0

    private val mockApi = MockPickarooApi(MockStore(FileSeedSource(), InMemoryStringStore()), latencyMillis = { 0L })
    private val realAuth = MockAuthService(mockApi, nowSeconds = { now }, newToken = { "t${++tokenCounter}" })

    private class CountingAuthApi(private val delegate: AuthApi) : AuthApi by delegate {
        var refreshCalls = 0
        override suspend fun refresh(body: RefreshRequest): RefreshResponse {
            refreshCalls++
            return delegate.refresh(body)
        }
    }

    private val auth = CountingAuthApi(realAuth)
    private val session = SessionRepository(InMemoryStringStore())
    private val calls = AuthorizedCalls(session, auth, nowSeconds = { now })

    private suspend fun login() {
        session.save(auth.login(LoginRequest("customer1", "demo1234")))
    }

    @Test
    fun `a valid token is used as it is`() = runTest {
        login()
        val original = session.accessToken()

        val bearer = calls.call { it }

        assertEquals("Bearer $original", bearer)
        assertEquals(0, auth.refreshCalls)
    }

    @Test
    fun `an expired token is refreshed before the call`() = runTest {
        login()
        val original = session.accessToken()
        now += 15 * 60

        val bearer = calls.call { it }

        assertEquals(1, auth.refreshCalls)
        assertNotEquals("Bearer $original", bearer)
        assertEquals("Bearer ${session.accessToken()}", bearer)
        assertFalse(JwtPayload.decode(session.accessToken())!!.isExpired(now))
    }

    @Test
    fun `a 401 from the server refreshes once and retries`() = runTest {
        login()
        val used = mutableListOf<String>()

        val result = calls.call { bearer ->
            used += bearer
            if (used.size == 1) throw MockErrors.unauthorized("TOKEN_EXPIRED")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, used.size)
        assertNotEquals(used[0], used[1])
        assertEquals(1, auth.refreshCalls)
    }

    @Test
    fun `a second 401 is passed on and the session is kept`() = runTest {
        login()
        var attempts = 0

        try {
            calls.call<Unit> { attempts++; throw MockErrors.unauthorized() }
            throw AssertionError("Expected a 401")
        } catch (e: HttpException) {
            assertEquals(401, e.code())
        }

        assertEquals(2, attempts)
        assertEquals(1, auth.refreshCalls)
        assertTrue(session.isLoggedIn())
    }

    @Test
    fun `other errors are passed on without refreshing`() = runTest {
        login()

        try {
            calls.call<Unit> { throw MockErrors.forbidden() }
            throw AssertionError("Expected a 403")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
        }

        assertEquals(0, auth.refreshCalls)
        assertTrue(session.isLoggedIn())
    }

    @Test
    fun `a refresh that is rejected logs the user out and announces it`() = runTest {
        login()
        auth.logout("Bearer ${session.accessToken()}", LogoutRequest(session.refreshToken()))
        now += 15 * 60
        val announcements = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            calls.sessionExpired.collect { announcements += it }
        }

        try {
            calls.call { it }
            throw AssertionError("Expected the session to expire")
        } catch (_: SessionExpiredException) {
        }

        assertFalse(session.isLoggedIn())
        assertEquals(1, announcements.size)
    }

    @Test
    fun `an expired refresh token also logs the user out`() = runTest {
        login()
        now += 7 * 24 * 60 * 60

        try {
            calls.call { it }
            throw AssertionError("Expected the session to expire")
        } catch (_: SessionExpiredException) {
        }

        assertFalse(session.isLoggedIn())
    }

    @Test
    fun `a server error while refreshing does not log the user out`() = runTest {
        login()
        now += 15 * 60
        mockApi.failNext { MockErrors.http(500, "SERVER_ERROR", "Try again later") }

        try {
            calls.call { it }
            throw AssertionError("Expected a 500")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }

        assertTrue(session.isLoggedIn())
    }

    @Test
    fun `calls without a session fail right away`() = runTest {
        try {
            calls.call { it }
            throw AssertionError("Expected SessionExpiredException")
        } catch (_: SessionExpiredException) {
        }

        assertEquals(0, auth.refreshCalls)
    }

    @Test
    fun `parallel calls with an expired token trigger a single refresh`() = runTest {
        login()
        now += 15 * 60

        val bearers = (1..4).map { async { calls.call { it } } }.awaitAll()

        assertEquals(1, auth.refreshCalls)
        assertEquals(setOf("Bearer ${session.accessToken()}"), bearers.toSet())
    }
}
