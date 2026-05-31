package com.example.pikaroo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pikaroo.ui.inicio.view.InicioView
import com.example.pikaroo.ui.ofertas.view.OfertasView
import com.example.pikaroo.ui.ordenar.view.OrdenarView
import com.example.pikaroo.ui.productos.view.ProductosView
import com.example.pikaroo.ui.usuario.view.UsuarioView
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

@Composable
fun TabsScaffold() {
    val nestedNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = nestedNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = AppRoute.Inicio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Inicio.route) { InicioView() }
            composable(AppRoute.Ofertas.route) { OfertasView() }
            composable(AppRoute.Ordenar.route) { OrdenarView() }
            composable(AppRoute.Productos.route) { ProductosView() }
            composable(AppRoute.Usuario.route) { UsuarioView() }
        }
    }
}

data class NavigationItem(
    val route: AppRoute,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem(AppRoute.Inicio, Icons.Outlined.Home, "Inicio"),
        NavigationItem(AppRoute.Ofertas, Icons.Outlined.LocalOffer, "Ofertas"),
        NavigationItem(AppRoute.Ordenar, Icons.Outlined.ShoppingCart, "Ordenar"),
        NavigationItem(AppRoute.Productos, Icons.Outlined.Inventory2, "Productos"),
        NavigationItem(AppRoute.Usuario, Icons.Outlined.Person, "Usuario")
    )
    
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        items.forEach { item ->
            val isSelected = currentRoute == item.route.route
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.label
                    ) 
                },
                label = { 
                    Text(
                        text = item.label, 
                        fontSize = 11.sp
                    ) 
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PikarooOrange,
                    selectedTextColor = PikarooOrange,
                    unselectedIconColor = PikarooTextGray,
                    unselectedTextColor = PikarooTextGray,
                    indicatorColor = Color.Transparent
                ),
                onClick = {
                    if (currentRoute != item.route.route) {
                        navController.navigate(item.route.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
