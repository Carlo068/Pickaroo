package com.example.pikaroo.common.mock

import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.common.mock.MockCollections.CATEGORIES
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockCollections.PRODUCTS
import com.example.pikaroo.common.mock.MockCollections.ROLES
import com.example.pikaroo.common.mock.MockCollections.SHOPS
import com.example.pikaroo.common.mock.MockCollections.USERS
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPickarooApiInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newApi() = MockPickarooApi.create(context, latencyMillis = { 0L })

    @After
    fun cleanUp() {
        newApi().reset()
    }

    @Test
    fun bundledSeedFilesAreReadable() = runBlocking {
        val api = newApi()

        assertEquals(10, api.call { all(ORDERS).size })
        assertEquals(3, api.call { all(SHOPS).size })
        assertEquals(5, api.call { all(USERS).size })
        assertEquals(4, api.call { all(ROLES).size })
        assertEquals(22, api.call { all(PRODUCTS).size })
    }

    @Test
    fun seedAccentsAreReadCorrectly() = runBlocking {
        val names = newApi().call { all(CATEGORIES).map { it.get("name").asString } }

        assertTrue("Panadería" in names)
        assertTrue("Lácteos" in names)
    }

    @Test
    fun writesSurviveANewInstanceBackedBySharedPreferences() = runBlocking {
        newApi().call {
            put(ORDERS, JsonObject().apply {
                addProperty("id", "order_2000")
                addProperty("note", "Panadería")
            })
            remove(ORDERS, "order_1001")
        }

        val restarted = newApi()

        assertEquals("Panadería", restarted.call { find(ORDERS, "order_2000") }!!.get("note").asString)
        assertNull(restarted.call { find(ORDERS, "order_1001") })
        assertEquals(10, restarted.call { all(ORDERS).size })
    }

    @Test
    fun resetRestoresTheSeedData() = runBlocking {
        val api = newApi()
        api.call { put(ORDERS, JsonObject().apply { addProperty("id", "order_2000") }) }

        api.reset()

        assertNull(newApi().call { find(ORDERS, "order_2000") })
        assertNotNull(newApi().call { find(ORDERS, "order_1001") })
    }
}
