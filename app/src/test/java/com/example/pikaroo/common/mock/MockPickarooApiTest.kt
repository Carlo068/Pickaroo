package com.example.pikaroo.common.mock

import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.google.gson.JsonObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class MockPickarooApiTest {

    private val storage = InMemoryStringStore()

    private fun newApi(latency: () -> Long = { 0L }) =
        MockPickarooApi(MockStore(FileSeedSource(), storage), latency)

    private suspend fun expectHttpError(block: suspend () -> Unit): HttpException {
        try {
            block()
        } catch (e: HttpException) {
            return e
        }
        throw AssertionError("Expected an HttpException")
    }

    @Test
    fun `call returns the result of the block`() = runTest {
        assertEquals(10, newApi().call { all(ORDERS).size })
    }

    @Test
    fun `default latency is between 300 and 600 ms`() = runTest {
        val api = MockPickarooApi(MockStore(FileSeedSource(), storage))

        repeat(30) {
            val start = currentTime
            api.call { Unit }
            assertTrue(currentTime - start in 300L..600L)
        }
    }

    @Test
    fun `latency can be replaced`() = runTest {
        val api = newApi(latency = { 1234L })

        val start = currentTime
        api.call { Unit }

        assertEquals(1234L, currentTime - start)
    }

    @Test
    fun `failNext makes only the next call fail and skips its block`() = runTest {
        val api = newApi()
        var executed = 0
        api.failNext { MockErrors.conflict("SLOT_FULL") }

        val error = expectHttpError { api.call { executed++ } }
        api.call { executed++ }

        assertEquals(409, error.code())
        assertEquals("SLOT_FULL", error.toApiError()!!.code)
        assertEquals(1, executed)
    }

    @Test
    fun `failNext can queue several failures in order`() = runTest {
        val api = newApi()
        api.failNext(times = 2) { MockErrors.unauthorized() }
        api.failNext { MockErrors.notFound() }

        val codes = (1..3).map { expectHttpError { api.call { Unit } }.code() }
        api.call { Unit }

        assertEquals(listOf(401, 401, 404), codes)
    }

    @Test
    fun `every injected failure is a fresh exception`() = runTest {
        val api = newApi()
        api.failNext(times = 2) { MockErrors.forbidden() }

        val first = expectHttpError { api.call { Unit } }
        val second = expectHttpError { api.call { Unit } }

        assertNotSame(first, second)
        assertNotNull(first.toApiError())
        assertNotNull(second.toApiError())
    }

    @Test
    fun `errors thrown inside the block reach the caller`() = runTest {
        val api = newApi()

        val error = expectHttpError { api.call { throw MockErrors.forbidden() } }

        assertEquals(403, error.code())
    }

    @Test
    fun `writes made through call survive a restart`() = runTest {
        newApi().call { put(ORDERS, JsonObject().apply { addProperty("id", "order_2000") }) }

        val found = newApi().call { find(ORDERS, "order_2000") }

        assertNotNull(found)
    }

    @Test
    fun `reset clears writes and pending failures`() = runTest {
        val api = newApi()
        api.call { put(ORDERS, JsonObject().apply { addProperty("id", "order_2000") }) }
        api.failNext { MockErrors.unauthorized() }

        api.reset()

        assertEquals(10, api.call { all(ORDERS).size })
    }
}
