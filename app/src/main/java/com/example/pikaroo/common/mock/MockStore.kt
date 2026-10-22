package com.example.pikaroo.common.mock

import com.example.pikaroo.common.preferences.StringStore
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class MockCollection(val name: String, val file: String? = null, val key: String? = null)

object MockCollections {
    val CATEGORIES = MockCollection("categories", "catalog.json", "categories")
    val GROUPS = MockCollection("groups", "catalog.json", "groups")
    val PRODUCTS = MockCollection("products", "catalog.json", "products")
    val ITEMS = MockCollection("items", "catalog.json", "items")
    val SHOPS = MockCollection("shops", "shops.json", "shops")
    val USERS = MockCollection("users", "users.json", "users")
    val ROLES = MockCollection("roles", "roles.json", "roles")
    val ORDERS = MockCollection("orders", "orders.seed.json", "orders")
    val DRIVER_LOCATIONS = MockCollection("driver_locations", "driver-locations.seed.json", "locations")
    val SESSIONS = MockCollection("sessions")

    val all = listOf(CATEGORIES, GROUPS, PRODUCTS, ITEMS, SHOPS, USERS, ROLES, ORDERS, DRIVER_LOCATIONS, SESSIONS)
}

/**
 * Documents (JSON objects with a string "id") per collection. Reads come from the seed files
 * until a collection is written; from then on the whole collection lives in [storage].
 */
class MockStore(private val seed: SeedSource, private val storage: StringStore) {

    private val cache = mutableMapOf<String, MutableList<JsonObject>>()

    @Synchronized
    fun all(collection: MockCollection): List<JsonObject> =
        load(collection).map { it.deepCopy() }

    @Synchronized
    fun find(collection: MockCollection, id: String): JsonObject? =
        load(collection).firstOrNull { it.idOrNull() == id }?.deepCopy()

    @Synchronized
    fun put(collection: MockCollection, document: JsonObject) {
        val id = document.idOrNull() ?: throw IllegalArgumentException("Document needs a string \"id\"")
        val documents = load(collection).toMutableList()
        val index = documents.indexOfFirst { it.idOrNull() == id }
        if (index >= 0) documents[index] = document.deepCopy() else documents.add(document.deepCopy())
        persist(collection, documents)
        cache[collection.name] = documents
    }

    @Synchronized
    fun remove(collection: MockCollection, id: String): Boolean {
        val documents = load(collection)
        val removed = documents.removeAll { it.idOrNull() == id }
        if (removed) persist(collection, documents)
        return removed
    }

    @Synchronized
    fun reset() {
        MockCollections.all.forEach { storage.remove(storageKey(it)) }
        cache.clear()
    }

    private fun load(collection: MockCollection): MutableList<JsonObject> =
        cache.getOrPut(collection.name) { readPersisted(collection) ?: readSeed(collection) }

    private fun readPersisted(collection: MockCollection): MutableList<JsonObject>? {
        val raw = storage.get(storageKey(collection)) ?: return null
        return try {
            JsonParser().parse(raw).asJsonArray.map { it.asJsonObject }.toMutableList()
        } catch (_: Exception) {
            storage.remove(storageKey(collection))
            null
        }
    }

    private fun readSeed(collection: MockCollection): MutableList<JsonObject> {
        val file = collection.file ?: return mutableListOf()
        return JsonParser().parse(seed.read(file)).asJsonObject
            .getAsJsonArray(collection.key)
            .map { it.asJsonObject }
            .toMutableList()
    }

    private fun persist(collection: MockCollection, documents: List<JsonObject>) {
        val array = JsonArray().apply { documents.forEach { add(it) } }
        storage.put(storageKey(collection), array.toString())
    }

    private fun storageKey(collection: MockCollection) = "mock_store_${collection.name}"

    private fun JsonObject.idOrNull(): String? =
        get("id")?.takeIf { it.isJsonPrimitive }?.asString
}
