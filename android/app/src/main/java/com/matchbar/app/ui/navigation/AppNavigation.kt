package com.matchbar.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matchbar.app.MatchBarApp
import com.matchbar.app.data.local.SessionStore
import com.matchbar.app.data.local.ThemeMode
import com.matchbar.app.data.model.Role
import com.matchbar.app.ui.common.GenericFactory
import com.matchbar.app.ui.screens.settings.SettingsScreen
import com.matchbar.app.ui.screens.admin.AdminPendingScreen
import com.matchbar.app.ui.screens.admin.AdminViewModel
import com.matchbar.app.ui.screens.auth.AuthViewModel
import com.matchbar.app.ui.screens.auth.LoginScreen
import com.matchbar.app.ui.screens.auth.RegisterScreen
import com.matchbar.app.ui.screens.bar.BarMatchesScreen
import com.matchbar.app.ui.screens.bar.BarMatchesViewModel
import com.matchbar.app.ui.screens.bar.BarReviewsScreen
import com.matchbar.app.ui.screens.bar.BarReviewsViewModel
import com.matchbar.app.ui.screens.bar.MyBarScreen
import com.matchbar.app.ui.screens.bar.MyBarViewModel
import com.matchbar.app.ui.screens.bars.BarDetailScreen
import com.matchbar.app.ui.screens.bars.BarDetailViewModel
import com.matchbar.app.ui.screens.favorites.FavoritesScreen
import com.matchbar.app.ui.screens.favorites.FavoritesViewModel
import com.matchbar.app.ui.screens.map.AllBarsMapScreen
import com.matchbar.app.ui.screens.map.AllBarsMapViewModel
import com.matchbar.app.ui.screens.map.NearbyBarsScreen
import com.matchbar.app.ui.screens.map.NearbyBarsViewModel
import com.matchbar.app.ui.screens.matches.MatchesScreen
import com.matchbar.app.ui.screens.matches.MatchesViewModel
import com.matchbar.app.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(app: MatchBarApp) {
    val navController = rememberNavController()
    val sessionFlow = app.sessionStore.session.collectAsState(initial = null)
    val session = sessionFlow.value
    val themeMode by app.sessionStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()

    val authFactory = remember {
        GenericFactory { AuthViewModel(app.api, app.sessionStore) }
    }
    val matchesFactory = remember { GenericFactory { MatchesViewModel(app.api) } }
    val allBarsMapFactory = remember {
        GenericFactory { AllBarsMapViewModel(app.api, app.applicationContext) }
    }
    val nearbyFactory = remember {
        GenericFactory { NearbyBarsViewModel(app.api, app.applicationContext) }
    }
    val barDetailFactory = remember { GenericFactory { BarDetailViewModel(app.api) } }
    val favoritesFactory = remember { GenericFactory { FavoritesViewModel(app.api) } }
    val myBarFactory = remember { GenericFactory { MyBarViewModel(app.api) } }
    val barMatchesFactory = remember { GenericFactory { BarMatchesViewModel(app.api) } }
    val barReviewsFactory = remember { GenericFactory { BarReviewsViewModel(app.api) } }
    val adminFactory = remember { GenericFactory { AdminViewModel(app.api) } }

    val logout: () -> Unit = {
        scope.launch { app.sessionStore.clear() }
    }

    // Si no hay sesión, mostramos el flujo de auth.
    if (session == null) {
        AuthGraph(navController = navController, factory = authFactory)
        return
    }

    // Flujo principal con bottom bar diferente según rol
    val startDest = when (session.role) {
        Role.USER -> Routes.MAP_ALL
        Role.BAR -> Routes.MY_BAR
        Role.ADMIN -> Routes.ADMIN_PENDING
    }

    Scaffold(
        bottomBar = {
            when (session.role) {
                Role.USER -> UserBottomBar(navController)
                Role.BAR -> BarBottomBar(navController)
                else -> {}
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            // ----- USER -----
            composable(Routes.MAP_ALL) {
                AllBarsMapScreen(
                    vmFactory = allBarsMapFactory,
                    onBarClick = { navController.navigate(Routes.barDetail(it.id)) }
                )
            }
            composable(Routes.MATCHES) {
                MatchesScreen(vmFactory = matchesFactory)
            }
            composable(
                route = Routes.MAP,
                arguments = listOf(navArgument("matchId") {
                    type = NavType.StringType; defaultValue = ""; nullable = false
                })
            ) { backStack ->
                val matchId = backStack.arguments?.getString("matchId")?.ifEmpty { null }
                NearbyBarsScreen(
                    matchId = matchId,
                    vmFactory = nearbyFactory,
                    onBarClick = { navController.navigate(Routes.barDetail(it.id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.BAR_DETAIL,
                arguments = listOf(navArgument("barId") { type = NavType.StringType })
            ) { backStack ->
                val barId = backStack.arguments?.getString("barId") ?: return@composable
                BarDetailScreen(
                    barId = barId,
                    currentRole = session.role,
                    vmFactory = barDetailFactory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    vmFactory = favoritesFactory,
                    onBarClick = { navController.navigate(Routes.barDetail(it.id)) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    session = session,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            // ----- BAR -----
            composable(Routes.MY_BAR) {
                MyBarScreen(
                    vmFactory = myBarFactory,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.MY_BAR_MATCHES) {
                BarMatchesScreen(
                    vmFactory = barMatchesFactory,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.MY_BAR_REVIEWS) {
                BarReviewsScreen(
                    vmFactory = barReviewsFactory,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            // ----- ADMIN -----
            composable(Routes.ADMIN_PENDING) {
                AdminPendingScreen(
                    vmFactory = adminFactory,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            // ----- Ajustes (todos los roles) -----
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    session = session,
                    themeMode = themeMode,
                    onThemeChange = { mode -> scope.launch { app.sessionStore.setThemeMode(mode) } },
                    onLogout = logout,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AuthGraph(
    navController: androidx.navigation.NavHostController,
    factory: ViewModelProvider.Factory
) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                vmFactory = factory,
                onLoginSuccess = { /* la sesión cambia y App se recompone */ },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                vmFactory = factory,
                onRegistered = { /* idem */ },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun UserBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Triple(Routes.MAP_ALL, "Mapa", Icons.Filled.Map),
        Triple(Routes.MATCHES, "Partidos", Icons.Filled.SportsSoccer),
        Triple(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite),
        Triple(Routes.PROFILE, "Cuenta", Icons.Filled.Person)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = current == route ||
                        (route == Routes.MATCHES && current?.startsWith("match") == true),
                onClick = {
                    if (current != route) {
                        navController.navigate(route) {
                            popUpTo(Routes.MAP_ALL) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(icon, label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun BarBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Triple(Routes.MY_BAR, "Mi bar", Icons.Filled.Storefront),
        Triple(Routes.MY_BAR_MATCHES, "Partidos", Icons.Filled.SportsSoccer),
        Triple(Routes.MY_BAR_REVIEWS, "Reseñas", Icons.Filled.RateReview)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = current == route,
                onClick = {
                    if (current != route) {
                        navController.navigate(route) {
                            popUpTo(Routes.MY_BAR) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(icon, label) },
                label = { Text(label) }
            )
        }
    }
}

// Re-exportamos para que MainActivity no necesite imports complicados
typealias AppSession = SessionStore.Session
