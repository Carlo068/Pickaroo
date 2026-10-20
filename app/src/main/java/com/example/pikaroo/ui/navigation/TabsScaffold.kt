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
import com.example.pikaroo.ui.home.view.HomeView
import com.example.pikaroo.ui.offers.view.OffersView
import com.example.pikaroo.ui.order.viewmodel.PlaceOrderViewModel
import com.example.pikaroo.ui.order.view.OrderView
import com.example.pikaroo.ui.products.view.ProductsView
import com.example.pikaroo.ui.user.view.UserView
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.products.viewmodel.ProductsViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.pikaroo.ui.auth.model.RoleMode
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.pikaroo.ui.cart.view.CartView
import com.example.pikaroo.ui.orders.view.ReceiptView
import com.example.pikaroo.ui.orders.view.OrdersView
import com.example.pikaroo.ui.orders.view.TrackingView
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel

@Composable
fun TabsScaffold(
    onLogout: () -> Unit = {},
    otherModes: List<RoleMode> = emptyList(),
    onSwitchMode: (RoleMode) -> Unit = {}
) {
    val nestedNavController = rememberNavController()

    // Compartido para enviar el filtro de Inicio a Productos.
    val productosViewModel: ProductsViewModel = viewModel(factory = ProductsViewModel.Factory)

    val context = LocalContext.current
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModel.factory(context))
    val cartState by cartViewModel.uiState.collectAsState()
    val placeOrderViewModel: PlaceOrderViewModel = viewModel(factory = PlaceOrderViewModel.Factory)
    val placeState by placeOrderViewModel.state.collectAsState()

    val openCart = { nestedNavController.navigate(AppRoute.Cart.route) { launchSingleTop = true } }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = nestedNavController
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Home.route) {
                HomeView(
                    viewModel = productosViewModel,
                    onOpenProducts = { category ->
                        productosViewModel.selectCategory(category)
                        nestedNavController.navigateToTab(AppRoute.Products.route)
                    },
                    cartItemCount = cartState.itemCount,
                    onOpenCart = openCart,
                    onAddToCart = cartViewModel::add
                )
            }

            composable(AppRoute.Offers.route) {
                OffersView()
            }

            composable(AppRoute.Order.route) {
                OrderView(
                    cart = cartState,
                    onOpenLastOrder = { nestedNavController.navigate(AppRoute.Receipt.create("latest")) { launchSingleTop = true } },
                    isSaving = placeState.isSaving,
                    saveError = placeState.error,
                    onConfirmOrder = { delivery ->
                        val snapshot = cartState
                        placeOrderViewModel.place(snapshot, delivery) { order ->
                            cartViewModel.removeOrderedItems(snapshot.items)
                            nestedNavController.navigate(AppRoute.Receipt.create(order.id)) { launchSingleTop = true }
                        }
                    },
                    cartItemCount = cartState.itemCount,
                    onOpenCart = openCart
                )
            }

            composable(AppRoute.Products.route) {
                ProductsView(
                    viewModel = productosViewModel,
                    onAddToCart = cartViewModel::add
                )
            }

            composable(AppRoute.User.route) {
                UserView(
                    onLogout = onLogout,
                    otherModes = otherModes,
                    onSwitchMode = onSwitchMode,
                    onOpenOrders = { nestedNavController.navigate(AppRoute.Orders.route) { launchSingleTop = true } },
                    cartItemCount = cartState.itemCount,
                    onOpenCart = openCart
                )
            }

            composable(AppRoute.Orders.route) {
                OrdersView(
                    onBack = { nestedNavController.popBackStack() },
                    onOpenOrder = { orderId -> nestedNavController.navigate(AppRoute.Tracking.create(orderId)) }
                )
            }

            composable(
                route = AppRoute.Receipt.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("orderId")
                ReceiptView(
                    orderId = id?.takeUnless { it == "latest" },
                    onBack = { nestedNavController.popBackStack() },
                    onTracking = { orderId -> nestedNavController.navigate(AppRoute.Tracking.create(orderId)) }
                )
            }

            composable(
                route = AppRoute.Tracking.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                TrackingView(
                    orderId = entry.arguments?.getString("orderId").orEmpty(),
                    onBack = { nestedNavController.popBackStack() }
                )
            }

            composable(AppRoute.Cart.route) {
                CartView(
                    cartViewModel = cartViewModel,
                    onBack = { nestedNavController.popBackStack() },
                    onCheckout = { nestedNavController.navigateToTab(AppRoute.Order.route) }
                )
            }
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
        NavigationItem(AppRoute.Home, Icons.Outlined.Home, "Inicio"),
        NavigationItem(AppRoute.Offers, Icons.Outlined.LocalOffer, "Ofertas"),
        NavigationItem(AppRoute.Order, Icons.Outlined.ShoppingCart, "Ordenar"),
        NavigationItem(AppRoute.Products, Icons.Outlined.Inventory2, "Productos"),
        NavigationItem(AppRoute.User, Icons.Outlined.Person, "Usuario")
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
                        navController.navigateToTab(item.route.route)
                    }
                }
            )
        }
    }
}

// El carrito no es una pestaña: se saca del back stack antes de cambiar de pestaña
// para que no quede guardado y se restaure dentro de otra.
fun NavHostController.navigateToTab(route: String) {
    popBackStack(AppRoute.Cart.route, inclusive = true)
    popBackStack(AppRoute.Receipt.route, inclusive = true)
    popBackStack(AppRoute.Orders.route, inclusive = true)
    popBackStack(AppRoute.Tracking.route, inclusive = true)
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
