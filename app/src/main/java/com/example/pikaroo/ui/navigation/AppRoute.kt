package com.example.pikaroo.ui.navigation

sealed class AppRoute(val route: String) {
    object Login : AppRoute("login")
    object Tabs : AppRoute("tabs")
    
    // Rutas dentro de las pestañas
    object Home : AppRoute("home")
    object Offers : AppRoute("offers")
    object Order : AppRoute("order")
    object Products : AppRoute("products")
    object User : AppRoute("user")
}
