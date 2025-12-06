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

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        Timber.d("🔑 [Refresh Token] Checking authentication status on app start")

        val accessToken = tokenManager.getAccessToken().first()
        val refreshToken = tokenManager.getRefreshToken().first()

        if (accessToken == null || refreshToken == null) {
            Timber.d("🔑 [Refresh Token] No tokens found - user needs to login")
            Timber.d("🔑 [Refresh Token] Setting startDestination to Login")
            startDestination = Screen.Login.route
            Timber.d("🔑 [Refresh Token] startDestination is now: $startDestination")
            return@LaunchedEffect
        }

        Timber.d("🔑 [Refresh Token] Tokens found - validating with backend")

        // Validate tokens by making a test API call
        val isValid = validateTokens(accessToken, refreshToken)

        if (!isValid) {
            Timber.d("🔑 [Refresh Token] Tokens invalid/expired - clearing and showing login")
            Timber.d("🔑 [Refresh Token] About to clear tokens...")

            // Launch token clearing in a separate job - don't wait for it
            launch {
                tokenManager.clearTokens()
                Timber.d("🔑 [Refresh Token] Tokens cleared successfully")
            }

            // Immediately set destination without waiting
            Timber.d("🔑 [Refresh Token] Setting startDestination to Login")
            startDestination = Screen.Login.route
            Timber.d("🔑 [Refresh Token] startDestination is now: ${startDestination}")
            Timber.d("🔑 [Refresh Token] Exiting LaunchedEffect with destination: ${startDestination}")
        } else {
            Timber.d("🔑 [Refresh Token] Tokens valid - user is logged in")
            Timber.d("🔑 [Refresh Token] Setting startDestination to Calendar")
            startDestination = Screen.Calendar.route
            Timber.d("🔑 [Refresh Token] startDestination is now: $startDestination")
        }
    }

    // Force recomposition when startDestination changes
    key(startDestination) {
        Timber.d("🔑 [Refresh Token] Current startDestination value: $startDestination")

        if (startDestination == null) {
            Timber.d("🔑 [Refresh Token] Showing loading indicator")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Timber.d("🔑 [Refresh Token] Showing NavHost with destination: $startDestination")
            NavHost(
                navController = navController,
                startDestination = startDestination!!
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
                    val scope = rememberCoroutineScope()
                    MainScreen(
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onLogout = {
                            Timber.d("🔑 [Refresh Token] Logging out from calendar screen")
                            scope.launch {
                                tokenManager.clearTokens()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    val scope = rememberCoroutineScope()
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            Timber.d("🔑 [Refresh Token] Logging out from profile screen")
                            scope.launch {
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
}

// Simple token validation function
private suspend fun validateTokens(accessToken: String, refreshToken: String): Boolean {
    return try {
        val client = HttpClient(Android) {
            expectSuccess = false

            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        // Try to refresh the token to check if refresh token is valid
        val response = client.post("${BuildConfig.API_BASE_URL}api/jwt/refresh/") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refresh" to refreshToken))
        }

        client.close()

        val isValid = response.status == HttpStatusCode.OK
        Timber.d("🔑 [Token Validation] Result: ${if (isValid) "VALID" else "INVALID"} (${response.status})")
        isValid
    } catch (e: Exception) {
        Timber.e(e, "🔑 [Token Validation] Exception during validation")
        false
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