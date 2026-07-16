package com.saad.tvcast.core.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saad.tvcast.feature.activity.ActivityScreen
import com.saad.tvcast.feature.browser.BrowserScreen
import com.saad.tvcast.feature.devices.DevicesScreen
import com.saad.tvcast.feature.home.HomeScreen
import com.saad.tvcast.feature.library.LibraryScreen
import com.saad.tvcast.feature.mirroring.MirroringScreen
import com.saad.tvcast.feature.onboarding.OnboardingScreen
import com.saad.tvcast.feature.player.PlayerScreen
import com.saad.tvcast.feature.premium.PremiumScreen
import com.saad.tvcast.feature.settings.SettingsScreen
import com.saad.tvcast.feature.splash.SplashScreen

@Composable
fun TVCastApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(Destination.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Splash.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Splash.route) {
                SplashScreen(
                    viewModel = hiltViewModel(),
                    onNavigateHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateOnboarding = {
                        navController.navigate(Destination.Onboarding.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destination.Onboarding.route) {
                OnboardingScreen(
                    viewModel = hiltViewModel(),
                    onFinished = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Destination.Home.route) {
                HomeScreen(
                    viewModel = hiltViewModel(),
                    onScanDevices = { navController.navigate(Destination.Devices.route) },
                    onMirror = { navController.navigate(Destination.Mirroring.route) },
                    onOpenLibrary = { navController.navigate(Destination.Library.route) },
                    onOpenBrowser = { navController.navigate(Destination.Browser.route) }
                )
            }
            composable(Destination.Browser.route) {
                BrowserScreen(viewModel = hiltViewModel())
            }
            composable(Destination.Library.route) {
                LibraryScreen(
                    viewModel = hiltViewModel(),
                    onPlayMedia = { uri, title ->
                        navController.navigate(Destination.Player.route(uri, title))
                    }
                )
            }
            composable(Destination.Activity.route) {
                ActivityScreen(viewModel = hiltViewModel())
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onPremium = { navController.navigate(Destination.Premium.route) }
                )
            }
            composable(Destination.Devices.route) {
                DevicesScreen(viewModel = hiltViewModel(), onBack = { navController.popBackStack() })
            }
            composable(Destination.Mirroring.route) {
                MirroringScreen(viewModel = hiltViewModel(), onBack = { navController.popBackStack() })
            }
            composable(Destination.Premium.route) {
                PremiumScreen(viewModel = hiltViewModel(), onBack = { navController.popBackStack() })
            }
            composable(
                route = Destination.Player.route,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType; nullable = true },
                    navArgument("title") { type = NavType.StringType; nullable = true }
                )
            ) { entry ->
                PlayerScreen(
                    uri = entry.arguments?.getString("uri")?.let(Uri::decode),
                    title = entry.arguments?.getString("title")?.let(Uri::decode),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
