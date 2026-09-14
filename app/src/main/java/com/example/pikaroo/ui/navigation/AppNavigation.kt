package com.example.pikaroo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pikaroo.ui.login.view.LoginView
import com.example.pikaroo.ui.onboarding.data.OnboardingPreferences
import com.example.pikaroo.ui.onboarding.view.OnboardingView

@Composable
fun AppNavigation() {
    val rootNavController = rememberNavController()
    val context = LocalContext.current

    val startDestination = remember {
        if (OnboardingPreferences(context).hasCompletedOnboarding()) {
            AppRoute.Login.route
        } else {
            "onboarding"
        }
    }

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
            LoginView(
                onLoginSuccess = {
                    rootNavController.navigate(AppRoute.Tabs.route) {
                        popUpTo(AppRoute.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppRoute.Tabs.route) {
            TabsScaffold()
        }
    }
}