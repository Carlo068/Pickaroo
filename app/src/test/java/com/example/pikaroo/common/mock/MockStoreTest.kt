package com.example.pikaroo.common.mock

import com.example.pikaroo.common.mock.MockCollections.ITEMS
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockCollections.PRODUCTS
import com.example.pikaroo.common.mock.MockCollections.SHOPS
import com.example.pikaroo.common.mock.MockCollections.USERS
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MockStoreTest {

    private val storage = InMemoryStringStore()

    private fun newStore() = MockStore(FileSeedSource(), storage)

    private fun doc(id: String, status: String = "PLACED") = JsonObject().apply {
        addProperty("id", id)
        addProperty("status", status)
    }

    @Test
    fun `reads documents from the seed files`() {
        val store = newStore()

        assertEquals(10, store.all(ORDERS).size)
        assertEquals(3, store.all(SHOPS).size)
        assertEquals("customer1", store.find(USERS, "user_1")!!.get("username").asString)
        assertNull(store.find(USERS, "missing"))
    }

    @Test
    fun `returns copies so callers cannot change the store by accident`() {
        val store = newStore()

        store.find(ORDERS, "order_1001")!!.addProperty("status", "HACKED")
        store.all(ORDERS).forEach { it.addProperty("status", "HACKED") }

        assertEquals("PICKED_UP", store.find(ORDERS, "order_1001")!!.get("status").asString)
    }

    @Test
    fun `put adds a new document`() {
        val store = newStore()

        store.put(ORDERS, doc("order_2000"))

        assertEquals(11, store.all(ORDERS).size)
        assertNotNull(store.find(ORDERS, "order_2000"))
    }

    @Test
    fun `put with an existing id replaces the document`() {
        val store = newStore()

        store.put(ORDERS, doc("order_1001", status = "CANCELLED"))

        assertEquals(10, store.all(ORDERS).size)
        assertEquals("CANCELLED", store.find(ORDERS, "order_1001")!!.get("status").asString)
    }

    @Test
    fun `put without an id is rejected`() {
        val store = newStore()

        assertThrows(IllegalArgumentException::class.java) { store.put(ORDERS, JsonObject()) }
    }

    @Test
    fun `writes survive a restart and accents are kept`() {
        newStore().put(ORDERS, doc("order_2000").apply { addProperty("note", "Panadería") })

        val afterRestart = newStore()

        assertEquals("Panadería", afterRestart.find(ORDERS, "order_2000")!!.get("note").asString)
        assertEquals(11, afterRestart.all(ORDERS).size)
    }

    @Test
    fun `only written collections are persisted`() {
        newStore().put(ORDERS, doc("order_2000"))

        assertEquals(setOf("mock_store_orders"), storage.data.keys)
        assertEquals(3, newStore().all(SHOPS).size)
    }

    @Test
    fun `collections from the same seed file are independent`() {
        val store = newStore()

        store.put(PRODUCTS, doc("999"))

        assertEquals(23, store.all(PRODUCTS).size)
        assertEquals(17, store.all(ITEMS).size)
        assertEquals(setOf("mock_store_products"), storage.data.keys)
    }

    @Test
    fun `remove deletes the document and is persisted`() {
        val store = newStore()

        assertTrue(store.remove(ORDERS, "order_1001"))
        assertFalse(store.remove(ORDERS, "order_1001"))

        assertNull(newStore().find(ORDERS, "order_1001"))
        assertEquals(9, newStore().all(ORDERS).size)
    }

    @Test
    fun `reset goes back to the seed data`() {
        val store = newStore()
        store.put(ORDERS, doc("order_2000"))
        store.remove(ORDERS, "order_1001")

        store.reset()

        assertTrue(storage.data.isEmpty())
        assertEquals(10, store.all(ORDERS).size)
        assertNotNull(store.find(ORDERS, "order_1001"))
        assertNull(store.find(ORDERS, "order_2000"))
    }

    @Test
    fun `damaged persisted data falls back to the seed`() {
        storage.put("mock_store_orders", "{not json")
        storage.put("mock_store_shops", "{\"not\":\"an array\"}")

        val store = newStore()

        assertEquals(10, store.all(ORDERS).size)
        assertEquals(3, store.all(SHOPS).size)
        assertTrue(storage.data.isEmpty())
    }
}
