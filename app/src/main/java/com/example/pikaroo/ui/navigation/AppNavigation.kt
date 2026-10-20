package com.example.pikaroo.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.LoginView
import com.example.pikaroo.ui.auth.viewmodel.LogoutViewModel
import com.example.pikaroo.ui.driver.view.DriverHomeView
import com.example.pikaroo.ui.onboarding.data.OnboardingPreferences
import com.example.pikaroo.ui.onboarding.view.OnboardingView
import com.example.pikaroo.ui.shop.view.ShopHomeView

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val context = LocalContext.current
    val session = remember { context.appContainer().sessionRepository }

    val startDestination = remember {
        when {
            session.isLoggedIn() && session.activeMode() != null -> homeRouteFor(session.activeMode())
            OnboardingPreferences(context).hasCompletedOnboarding() -> AppRoute.Login.route
            else -> "onboarding"
        }
    }

    // Lleva al usuario al inicio de su modo activo; sin modo válido no hay sesión utilizable.
    fun goHome(leaving: String) {
        val target = homeRouteFor(session.activeMode())
        if (target == leaving || target == AppRoute.Login.route) session.clear()
        val destination = if (target == leaving) AppRoute.Login.route else target
        rootNavController.navigate(destination) {
            popUpTo(leaving) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun switchMode(leaving: String, mode: RoleMode) {
        session.setActiveMode(mode)
        goHome(leaving)
    }

    fun otherModes(current: RoleMode) = session.availableModes().filter { it != current }

    NavHost(
        navController = rootNavController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingView(
                onFinishOnboarding = {
                    rootNavController.navigate(AppRoute.Login.route) {
                        popUpTo("onboarding") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoute.Login.route) {
            LoginView(onLoginSuccess = { goHome(AppRoute.Login.route) })
        }

        composable(AppRoute.Tabs.route) {
            val logoutViewModel: LogoutViewModel = viewModel()
            RequirePermission(session, Permission.CART_USE, onDenied = { goHome(AppRoute.Tabs.route) }) {
                Box(Modifier.fillMaxSize().testTag(NavTestTags.CUSTOMER_HOME)) {
                    TabsScaffold(
                        onLogout = {
                            logoutViewModel.logout {
                                rootNavController.navigate(AppRoute.Login.route) {
                                    popUpTo(AppRoute.Tabs.route) { inclusive = true }
                                }
                            }
                        },
                        otherModes = otherModes(RoleMode.CUSTOMER),
                        onSwitchMode = { switchMode(AppRoute.Tabs.route, it) }
                    )
                }
            }
        }

        composable(AppRoute.ShopHome.route) {
            val logoutViewModel: LogoutViewModel = viewModel()
            RequirePermission(session, Permission.SHOP_ORDERS_READ, onDenied = { goHome(AppRoute.ShopHome.route) }) {
                Box(Modifier.fillMaxSize().testTag(NavTestTags.SHOP_HOME)) {
                    ShopHomeView(
                        userName = session.session()?.user?.name.orEmpty(),
                        otherModes = otherModes(RoleMode.SHOP),
                        onSwitchMode = { switchMode(AppRoute.ShopHome.route, it) },
                        onLogout = {
                            logoutViewModel.logout {
                                rootNavController.navigate(AppRoute.Login.route) {
                                    popUpTo(AppRoute.ShopHome.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }

        composable(AppRoute.DriverHome.route) {
            val logoutViewModel: LogoutViewModel = viewModel()
            RequirePermission(session, Permission.DELIVERY_READ_AVAILABLE, onDenied = { goHome(AppRoute.DriverHome.route) }) {
                Box(Modifier.fillMaxSize().testTag(NavTestTags.DRIVER_HOME)) {
                    DriverHomeView(
                        userName = session.session()?.user?.name.orEmpty(),
                        otherModes = otherModes(RoleMode.DRIVER),
                        onSwitchMode = { switchMode(AppRoute.DriverHome.route, it) },
                        onLogout = {
                            logoutViewModel.logout {
                                rootNavController.navigate(AppRoute.Login.route) {
                                    popUpTo(AppRoute.DriverHome.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
