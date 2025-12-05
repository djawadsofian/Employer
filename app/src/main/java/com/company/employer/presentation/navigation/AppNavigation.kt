package com.company.employer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.company.employer.data.local.TokenManager
import com.company.employer.presentation.calendar.CalendarScreen
import com.company.employer.presentation.login.LoginScreen
import com.company.employer.presentation.profile.ProfileScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Calendar : Screen("calendar")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
}

sealed class BottomNavScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Calendar : BottomNavScreen("calendar", "Calendrier", Icons.Default.CalendarMonth)
    data object Profile : BottomNavScreen("profile", "Profil", Icons.Default.Person)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val tokenManager: TokenManager = koinInject()
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            Timber.d("🔑 [Refresh Token] Checking authentication status on app start")
            val token = tokenManager.getAccessToken().first()
            Timber.d("🔑 [Refresh Token] Initial auth check - Access token exists: ${token != null}")

            if (token != null) {
                Timber.d("🔑 [Refresh Token] User is logged in with token (first 10 chars): ${token.take(10)}...")
                // Also check refresh token
                val refreshToken = tokenManager.getRefreshToken().first()
                Timber.d("🔑 [Refresh Token] Refresh token exists: ${refreshToken != null}")
            } else {
                Timber.d("🔑 [Refresh Token] No access token found - user needs to login")
            }

            isLoggedIn = token != null
            isCheckingAuth = false
        }
    }

    if (isCheckingAuth) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Calendar.route else Screen.Login.route
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToHome = {
                        Timber.d("🔑 [Refresh Token] Login successful, navigating to home")
                        navController.navigate(Screen.Calendar.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Calendar.route) {
                MainScreen(
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onLogout = {
                        Timber.d("🔑 [Refresh Token] Logging out from calendar screen")
                        scope.launch {
                            Timber.d("🔑 [Refresh Token] Clearing tokens and navigating to login")
                            tokenManager.clearTokens()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        Timber.d("🔑 [Refresh Token] Logging out from profile screen")
                        scope.launch {
                            Timber.d("🔑 [Refresh Token] Clearing tokens and navigating to login")
                            tokenManager.clearTokens()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            // Taller navigation bar like calendar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp), // Increased height
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    modifier = Modifier.height(80.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    listOf(
                        BottomNavScreen.Calendar,
                        BottomNavScreen.Profile
                    ).forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(28.dp) // Slightly larger icons
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Calendar.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavScreen.Calendar.route) {
                CalendarScreen(onNavigateToNotifications = onNavigateToNotifications)
            }

            composable(BottomNavScreen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = onLogout
                )
            }
        }
    }
}