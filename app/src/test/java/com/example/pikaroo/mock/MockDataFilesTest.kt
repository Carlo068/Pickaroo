package com.example.pikaroo.mock

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalTime

class MockDataFilesTest {

    private val dir: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/mock") }
        .first { it.isDirectory }

    private val fileNames = listOf(
        "catalog.json", "shops.json", "users.json", "roles.json", "orders.seed.json", "driver-locations.seed.json"
    )

    private val knownPermissions = setOf(
        "catalog:read", "cart:use", "order:create", "order:read:own", "order:cancel:own",
        "shop:orders:read", "order:status:update", "product:stock:update", "product:manage",
        "slots:manage", "staff:manage", "shop:stats:read", "delivery:read:available",
        "delivery:accept", "delivery:update", "location:write"
    )

    private val knownStatuses = setOf(
        "PLACED", "ACCEPTED", "PREPARING", "READY", "PICKED_UP",
        "DRIVER_ASSIGNED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    )

    private fun text(name: String) = File(dir, name).readBytes().toString(Charsets.UTF_8)

    private fun list(name: String, key: String): List<JsonObject> =
        JsonParser().parse(text(name)).asJsonObject.getAsJsonArray(key).map { it.asJsonObject }

    private fun JsonObject.str(key: String) = get(key).asString
    private fun JsonObject.int(key: String) = get(key).asInt
    private fun JsonObject.dec(key: String): BigDecimal = get(key).asBigDecimal
    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private val shops get() = list("shops.json", "shops")
    private val users get() = list("users.json", "users")
    private val roles get() = list("roles.json", "roles")
    private val products get() = list("catalog.json", "products")
    private val items get() = list("catalog.json", "items")
    private val groups get() = list("catalog.json", "groups")
    private val categories get() = list("catalog.json", "categories")
    private val orders get() = list("orders.seed.json", "orders")
    private val locations get() = list("driver-locations.seed.json", "locations")

    private fun ids(objects: List<JsonObject>) = objects.map { it.str("id") }

    private fun roleIdsOf(user: JsonObject) =
        user.getAsJsonArray("roles").map { it.asJsonObject.str("role") }

    @Test
    fun `every file is strict UTF-8 JSON without BOM or replacement characters`() {
        for (name in fileNames) {
            val bytes = File(dir, name).readBytes()
            assertFalse("$name has a BOM", bytes.take(3) == listOf(0xEF, 0xBB, 0xBF).map { it.toByte() })

            val decoded = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()

            assertFalse("$name contains U+FFFD", decoded.contains('�'))
            JsonParser().parse(decoded)
        }
    }

    @Test
    fun `accented names are stored correctly`() {
        assertTrue(text("catalog.json").contains("\"Panadería\""))
        assertTrue(text("catalog.json").contains("\"Café Premium\""))
        assertTrue(text("catalog.json").contains("\"Lácteos\""))
        assertTrue(text("shops.json").contains("Panadería La Espiga"))
        assertTrue(text("roles.json").contains("Dueño de tienda"))
    }

    @Test
    fun `catalog ids are unique and every reference resolves`() {
        val shopIds = ids(shops).toSet()
        val categoryIds = ids(categories).toSet()
        val itemIds = ids(items).toSet()
        val productsById = products.associateBy { it.str("id") }

        assertEquals(products.size, productsById.size)
        assertEquals(items.size, itemIds.size)
        assertEquals(groups.size, ids(groups).toSet().size)

        for (product in products) {
            val pid = product.str("id")
            assertTrue("product $pid shop", product.str("shop_id") in shopIds)
            assertTrue("product $pid category", product.str("category_id") in categoryIds)
            assertTrue("product $pid stock", product.int("stock") >= 0)
            assertTrue("product $pid availability", product.str("availability") in setOf("available", "unavailable"))
            product.obj("discount")?.let { assertTrue("product $pid discount", it.int("percentage") in 1..100) }

            val lines = product.getAsJsonArray("items").map { it.asJsonObject }
            assertTrue("product $pid has items", lines.isNotEmpty())
            for (line in lines) {
                assertTrue("product $pid item ${line.str("item_id")}", line.str("item_id") in itemIds)
                assertTrue("product $pid quantity", line.int("quantity") > 0)
            }
        }

        for (item in items) {
            val owner = productsById[item.str("product_id")]
            assertTrue("item ${item.str("id")} product", owner != null)
        }
    }

    @Test
    fun `single item products point back to their own item and bundles stay inside their shop`() {
        val productsById = products.associateBy { it.str("id") }
        val itemsById = items.associateBy { it.str("id") }

        for (product in products) {
            val pid = product.str("id")
            val lines = product.getAsJsonArray("items").map { it.asJsonObject }
            if (product.str("type") == "item") {
                assertEquals("product $pid", 1, lines.size)
                assertEquals("product $pid", pid, itemsById.getValue(lines[0].str("item_id")).str("product_id"))
            } else {
                for (line in lines) {
                    val owner = productsById.getValue(itemsById.getValue(line.str("item_id")).str("product_id"))
                    assertEquals("bundle $pid", product.str("shop_id"), owner.str("shop_id"))
                }
            }
        }
    }

    @Test
    fun `groups belong to a shop and only list that shop's products`() {
        val shopIds = ids(shops).toSet()
        val productsById = products.associateBy { it.str("id") }

        for (group in groups) {
            val gid = group.str("id")
            assertTrue("group $gid shop", group.str("shop_id") in shopIds)
            assertTrue("group $gid type", group.str("type") in setOf("items", "portfolios"))
            for (pid in group.getAsJsonArray("product_ids").map { it.asString }) {
                val product = productsById[pid]
                assertTrue("group $gid product $pid", product != null)
                assertEquals("group $gid product $pid shop", group.str("shop_id"), product!!.str("shop_id"))
            }
            val starts = group.get("starts_at")?.takeIf { !it.isJsonNull }?.asString
            val ends = group.get("ends_at")?.takeIf { !it.isJsonNull }?.asString
            if (starts != null && ends != null) {
                assertTrue("group $gid schedule", Instant.parse(starts).isBefore(Instant.parse(ends)))
            }
        }
    }

    @Test
    fun `catalog contains the cases the screens need to handle`() {
        assertTrue("scheduled group", groups.any { !it.get("starts_at").isJsonNull })
        assertTrue("always-on group", groups.any { it.get("starts_at").isJsonNull })
        assertTrue("discounted product", products.any { it.obj("discount") != null })
        assertTrue("unavailable product", products.any { it.str("availability") == "unavailable" })
        assertTrue("portfolio product", products.any { it.str("type") == "portfolio" })
        for (shop in ids(shops)) {
            assertTrue("shop $shop has products", products.any { it.str("shop_id") == shop })
            assertTrue("shop $shop has groups", groups.any { it.str("shop_id") == shop })
        }
    }

    @Test
    fun `shops have valid hours, slots and delivery settings`() {
        assertEquals(shops.size, ids(shops).toSet().size)

        for (shop in shops) {
            val sid = shop.str("id")
            assertTrue("shop $sid lat", shop.get("lat").asDouble in -90.0..90.0)
            assertTrue("shop $sid lng", shop.get("lng").asDouble in -180.0..180.0)

            for (block in shop.getAsJsonArray("hours").map { it.asJsonObject }) {
                assertTrue("shop $sid days", block.getAsJsonArray("days").all { it.asInt in 1..7 })
                assertTrue("shop $sid open < close", LocalTime.parse(block.str("open")).isBefore(LocalTime.parse(block.str("close"))))
            }

            val pickup = shop.obj("pickup")!!
            assertTrue("shop $sid slot_minutes", pickup.int("slot_minutes") > 0)
            assertTrue("shop $sid capacity", pickup.int("capacity_per_slot") > 0)
            assertTrue("shop $sid lead", pickup.int("lead_minutes") >= 0)

            val delivery = shop.obj("delivery")!!
            assertTrue("shop $sid fee", delivery.dec("fee") >= BigDecimal.ZERO)
            if (delivery.get("enabled").asBoolean) {
                assertTrue("shop $sid radius", delivery.dec("radius_km") > BigDecimal.ZERO)
            }
        }

        assertTrue("a shop with delivery", shops.any { it.obj("delivery")!!.get("enabled").asBoolean })
        assertTrue("a pickup-only shop", shops.any { !it.obj("delivery")!!.get("enabled").asBoolean })
    }

    @Test
    fun `roles only grant known permissions and keep the permission matrix`() {
        val byId = roles.associate { it.str("id") to it.getAsJsonArray("permissions").map { p -> p.asString }.toSet() }

        assertEquals(setOf("customer", "staff", "owner", "driver"), byId.keys)
        for ((role, permissions) in byId) {
            assertTrue("$role has unknown permissions", knownPermissions.containsAll(permissions))
            assertTrue("$role can read the catalog", "catalog:read" in permissions)
        }

        assertTrue(byId.getValue("owner").containsAll(byId.getValue("staff")))
        assertFalse("shop:orders:read" in byId.getValue("customer"))
        assertFalse("order:create" in byId.getValue("driver"))
        assertFalse("product:manage" in byId.getValue("staff"))
        assertTrue("delivery:accept" in byId.getValue("driver"))
        assertFalse("delivery:accept" in byId.getValue("owner"))
    }

    @Test
    fun `users have unique logins, valid roles and mock-only data`() {
        val roleIds = ids(roles).toSet()
        val shopIds = ids(shops).toSet()

        assertEquals(users.size, ids(users).toSet().size)
        assertEquals(users.size, users.map { it.str("username") }.toSet().size)

        for (user in users) {
            val uid = user.str("id")
            assertTrue("$uid password", user.str("password_mock").isNotBlank())
            assertTrue("$uid email", user.str("email").endsWith("@pickaroo.test"))
            for (assignment in user.getAsJsonArray("roles").map { it.asJsonObject }) {
                val role = assignment.str("role")
                assertTrue("$uid role $role", role in roleIds)
                val shopId = assignment.get("shop_id")?.takeIf { !it.isJsonNull }?.asString
                if (role == "staff" || role == "owner") {
                    assertTrue("$uid $role needs a shop", shopId != null && shopId in shopIds)
                } else {
                    assertTrue("$uid $role has no shop", shopId == null)
                }
            }
        }

        for (role in roleIds) {
            assertTrue("a user with role $role", users.any { role in roleIdsOf(it) })
        }
        assertTrue("a user with several roles", users.any { roleIdsOf(it).size > 1 })
        assertTrue("owners of different shops", users.flatMap { u ->
            u.getAsJsonArray("roles").map { it.asJsonObject }.filter { it.str("role") == "owner" }.map { it.str("shop_id") }
        }.toSet().size >= 2)
    }

    @Test
    fun `seed orders reference real shops, users and products`() {
        val shopsById = shops.associateBy { it.str("id") }
        val usersById = users.associateBy { it.str("id") }
        val productsById = products.associateBy { it.str("id") }

        assertTrue("about ten orders", orders.size >= 8)
        assertEquals(orders.size, ids(orders).toSet().size)

        for (order in orders) {
            val oid = order.str("id")
            val shop = shopsById.getValue(order.str("shop_id"))
            assertTrue("$oid status", order.str("status") in knownStatuses)
            assertTrue("$oid customer", "customer" in roleIdsOf(usersById.getValue(order.str("customer_id"))))

            for (line in order.getAsJsonArray("items").map { it.asJsonObject }) {
                val product = productsById[line.str("product_id")]
                assertTrue("$oid product ${line.str("product_id")}", product != null)
                assertEquals("$oid product shop", shop.str("id"), product!!.str("shop_id"))
            }

            when (order.str("fulfilment")) {
                "pickup" -> {
                    assertTrue("$oid pickup block", order.obj("pickup") != null && order.obj("delivery") == null)
                    assertTrue("$oid pickup code", order.obj("pickup")!!.str("code").length == 6)
                }
                "delivery" -> {
                    assertTrue("$oid delivery block", order.obj("delivery") != null && order.obj("pickup") == null)
                    assertTrue("$oid shop delivers", shop.obj("delivery")!!.get("enabled").asBoolean)
                    val driverId = order.obj("delivery")!!.get("driver_id")?.takeIf { !it.isJsonNull }?.asString
                    if (driverId != null) assertTrue("$oid driver", "driver" in roleIdsOf(usersById.getValue(driverId)))
                }
                else -> throw AssertionError("$oid unknown fulfilment")
            }
        }

        val codes = orders.mapNotNull { it.obj("pickup")?.str("code") }
        assertEquals("pickup codes are unique", codes.size, codes.toSet().size)
    }

    @Test
    fun `seed order totals add up to the cent`() {
        for (order in orders) {
            val oid = order.str("id")
            var subtotal = BigDecimal.ZERO

            for (line in order.getAsJsonArray("items").map { it.asJsonObject }) {
                val expected = line.dec("unit_price")
                    .multiply(BigDecimal(line.int("quantity")))
                    .multiply(BigDecimal(100 - line.int("discount_percentage")))
                    .divide(BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP)
                assertEquals("$oid line ${line.str("product_id")}", 0, expected.compareTo(line.dec("line_total")))
                subtotal += line.dec("line_total")
            }

            assertEquals("$oid subtotal", 0, subtotal.compareTo(order.dec("subtotal")))
            assertEquals("$oid total", 0, (order.dec("subtotal") + order.dec("delivery_fee")).compareTo(order.dec("total")))
        }
    }

    @Test
    fun `seed order events tell a consistent story`() {
        val userIds = ids(users).toSet()

        for (order in orders) {
            val oid = order.str("id")
            val events = order.getAsJsonArray("events").map { it.asJsonObject }

            assertEquals("$oid first event", "PLACED", events.first().str("status"))
            assertEquals("$oid last event matches status", order.str("status"), events.last().str("status"))
            assertEquals("$oid starts at creation", order.str("created_at"), events.first().str("at"))

            val times = events.map { Instant.parse(it.str("at")) }
            assertEquals("$oid events are in order", times.sorted(), times)
            events.forEach { assertTrue("$oid event author", it.str("by") in userIds) }
        }
    }

    @Test
    fun `driver locations belong to drivers on a delivery they are carrying`() {
        val usersById = users.associateBy { it.str("id") }
        val ordersById = orders.associateBy { it.str("id") }

        assertTrue("at least one location", locations.isNotEmpty())
        assertEquals(locations.size, ids(locations).toSet().size)

        for (location in locations) {
            val driverId = location.str("id")
            assertTrue("$driverId is a driver", "driver" in roleIdsOf(usersById.getValue(driverId)))
            assertTrue("$driverId lat", location.get("lat").asDouble in -90.0..90.0)
            assertTrue("$driverId lng", location.get("lng").asDouble in -180.0..180.0)
            Instant.parse(location.str("updated_at"))

            val order = ordersById.getValue(location.str("order_id"))
            assertEquals("delivery", order.str("fulfilment"))
            assertEquals(driverId, order.obj("delivery")!!.str("driver_id"))
            assertTrue("order on the way", order.str("status") in setOf("DRIVER_ASSIGNED", "OUT_FOR_DELIVERY"))
        }
    }
}
