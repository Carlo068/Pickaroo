package com.example.pikaroo.ui.navigation

sealed class AppRoute(val route: String) {
    object Login : AppRoute("login")
    object Tabs : AppRoute("tabs")
    
    // Rutas dentro de las pestañas
    object Inicio : AppRoute("inicio")
    object Ofertas : AppRoute("ofertas")
    object Ordenar : AppRoute("ordenar")
    object Productos : AppRoute("productos")
    object Usuario : AppRoute("usuario")
}
