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
import com.company.employer.BuildConfig
import com.company.employer.data.local.TokenManager
import com.company.employer.presentation.calendar.CalendarScreen
import com.company.employer.presentation.login.LoginScreen
import com.company.employer.presentation.profile.ProfileScreen
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    var startDestination by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Timber.d("🔑 [Auth] Checking authentication status on app start")

        val accessToken = tokenManager.getAccessToken().first()
        val refreshToken = tokenManager.getRefreshToken().first()

        if (accessToken == null || refreshToken == null) {
            Timber.d("🔑 [Auth] No tokens found - showing login")
            startDestination = Screen.Login.route
            return@LaunchedEffect
        }

        Timber.d("🔑 [Auth] Tokens found - validating with backend")

        // Try to validate tokens, but allow offline mode
        val isValid = validateTokensWithOfflineSupport(refreshToken)

        if (isValid == false) {
            // Only redirect to login if validation explicitly failed (not if offline)
            Timber.d("🔑 [Auth] Tokens invalid - clearing and showing login")
            scope.launch {
                tokenManager.clearTokens()
            }
            startDestination = Screen.Login.route
        } else {
            // Either valid or offline - allow user to proceed
            Timber.d("🔑 [Auth] Tokens valid or offline - proceeding to calendar")
            startDestination = Screen.Calendar.route
        }
    }

    key(startDestination) {
        if (startDestination == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = startDestination!!
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        onNavigateToHome = {
                            Timber.d("🔑 [Auth] Login successful, navigating to calendar")
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
                            Timber.d("🔑 [Auth] Logout requested from calendar")
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            Timber.d("🔑 [Auth] Logout requested from profile")
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Validates tokens with offline support
 * Returns:
 * - true: tokens are valid
 * - false: tokens are explicitly invalid (should logout)
 * - null: offline/network error (allow cached access)
 */
private suspend fun validateTokensWithOfflineSupport(refreshToken: String): Boolean? {
    return try {
        val client = HttpClient(Android) {
            expectSuccess = false

            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            // Set timeout for faster offline detection
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 5000
            }
        }

        val response = client.post("${BuildConfig.API_BASE_URL}api/jwt/refresh/") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refresh" to refreshToken))
        }

        client.close()

        when (response.status) {
            HttpStatusCode.OK -> {
                Timber.d("🔑 [Token Validation] Valid")
                true
            }
            HttpStatusCode.Unauthorized -> {
                Timber.d("🔑 [Token Validation] Invalid (401)")
                false
            }
            else -> {
                Timber.d("🔑 [Token Validation] Unexpected status: ${response.status}")
                false
            }
        }
    } catch (e: Exception) {
        // Network errors - allow offline mode
        Timber.w("🔑 [Token Validation] Network error (allowing offline): ${e.message}")
        null
    }
}

@Composable
fun MainScreen(
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val tokenManager: TokenManager = koinInject()
    val scope = rememberCoroutineScope()

    // Handle logout properly - simplified version
    val handleLogout: () -> Unit = {
        Timber.d("🚪 [MainScreen] handleLogout called")
        scope.launch {
            try {
                Timber.d("🚪 [MainScreen] Clearing tokens...")
                tokenManager.clearTokens()
                Timber.d("🚪 [MainScreen] Tokens cleared, calling onLogout()")
                onLogout()
            } catch (e: Exception) {
                Timber.e(e, "🚪 [MainScreen] Error clearing tokens")
                // Still logout even if clearing fails
                onLogout()
            }
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
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
                                    modifier = Modifier.size(28.dp)
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
                    onLogout = handleLogout
                )
            }
        }
    }
}