package com.example.pikaroo.ui.auth.model

/** Permisos que devuelve la API (docs/PLAN.md §4.1). La app decide qué mostrar según permisos, no según nombres de rol. */
object Permission {
    const val CATALOG_READ = "catalog:read"
    const val CART_USE = "cart:use"
    const val ORDER_CREATE = "order:create"
    const val ORDER_READ_OWN = "order:read:own"
    const val ORDER_CANCEL_OWN = "order:cancel:own"
    const val SHOP_ORDERS_READ = "shop:orders:read"
    const val ORDER_STATUS_UPDATE = "order:status:update"
    const val PRODUCT_STOCK_UPDATE = "product:stock:update"
    const val PRODUCT_MANAGE = "product:manage"
    const val SLOTS_MANAGE = "slots:manage"
    const val STAFF_MANAGE = "staff:manage"
    const val SHOP_STATS_READ = "shop:stats:read"
    const val DELIVERY_READ_AVAILABLE = "delivery:read:available"
    const val DELIVERY_ACCEPT = "delivery:accept"
    const val DELIVERY_UPDATE = "delivery:update"
    const val LOCATION_WRITE = "location:write"

    val all = listOf(
        CATALOG_READ, CART_USE, ORDER_CREATE, ORDER_READ_OWN, ORDER_CANCEL_OWN,
        SHOP_ORDERS_READ, ORDER_STATUS_UPDATE, PRODUCT_STOCK_UPDATE, PRODUCT_MANAGE,
        SLOTS_MANAGE, STAFF_MANAGE, SHOP_STATS_READ,
        DELIVERY_READ_AVAILABLE, DELIVERY_ACCEPT, DELIVERY_UPDATE, LOCATION_WRITE
    )
}

/** Modo en el que usa la app un usuario; el orden es la prioridad para elegir el modo inicial. */
enum class RoleMode {
    SHOP, DRIVER, CUSTOMER;

    companion object {
        fun of(role: String): RoleMode? = when (role) {
            "staff", "owner" -> SHOP
            "driver" -> DRIVER
            "customer" -> CUSTOMER
            else -> null
        }
    }
}
