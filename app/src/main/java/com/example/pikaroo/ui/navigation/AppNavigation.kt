package com.example.pikaroo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pikaroo.ui.login.view.LoginView

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = AppRoute.Login.route
    ) {
        composable(AppRoute.Login.route) {
            LoginView(onLoginSuccess = {
                rootNavController.navigate(AppRoute.Tabs.route) {
                    popUpTo(AppRoute.Login.route) {
                        inclusive = true
                    }
                }
            })
        }
        composable(AppRoute.Tabs.route) {
            TabsScaffold()
        }
    }
}
