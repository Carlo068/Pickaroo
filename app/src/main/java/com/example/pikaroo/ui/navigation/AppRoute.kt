package com.example.pikaroo.ui.navigation

import com.example.pikaroo.ui.auth.model.RoleMode

sealed class AppRoute(val route: String) {
    object Login : AppRoute("login")
    object Tabs : AppRoute("tabs")
    object ShopHome : AppRoute("shop")
    object DriverHome : AppRoute("driver")

    // Rutas dentro de las pestañas
    object Home : AppRoute("home")
    object Offers : AppRoute("offers")
    object Order : AppRoute("order")
    object Products : AppRoute("products")
    object User : AppRoute("user")
    object Cart : AppRoute("cart")
    object Receipt : AppRoute("receipt/{orderId}") {
        fun create(orderId: String) = "receipt/${android.net.Uri.encode(orderId)}"
    }
    object Orders : AppRoute("orders")
    object Tracking : AppRoute("tracking/{orderId}") {
        fun create(orderId: String) = "tracking/$orderId"
    }
}

/** Pantalla de inicio de cada modo; sin modo válido se vuelve al login. */
fun homeRouteFor(mode: RoleMode?): String = when (mode) {
    RoleMode.SHOP -> AppRoute.ShopHome.route
    RoleMode.DRIVER -> AppRoute.DriverHome.route
    RoleMode.CUSTOMER -> AppRoute.Tabs.route
    null -> AppRoute.Login.route
}

object NavTestTags {
    const val CUSTOMER_HOME = "home_customer"
    const val SHOP_HOME = "home_shop"
    const val DRIVER_HOME = "home_driver"
    fun switchTo(mode: RoleMode) = "switch_${mode.name}"
}
