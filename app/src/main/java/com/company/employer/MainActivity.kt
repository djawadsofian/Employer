package com.company.employer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.company.employer.fcm.FCMHelper
import com.company.employer.presentation.navigation.AppNavigation
import com.company.employer.presentation.theme.EmployerTheme
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission and get FCM token
        setupFCM()

        setContent {
            EmployerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun setupFCM() {
        // Request notification permission
        FCMHelper.requestNotificationPermission(this) { granted ->
            if (granted) {
                Timber.d("🔥 Notification permission granted")

                // Get FCM token
                lifecycleScope.launch {
                    val token = FCMHelper.getFCMToken()
                    if (token != null) {
                        FCMHelper.saveToken(this@MainActivity, token)
                        Timber.d("🔥 FCM Token: $token")

                        // TODO: Send token to backend when user logs in
                        // You'll do this in LoginViewModel
                    }
                }
            } else {
                Timber.w("🔥 Notification permission denied")
            }
        }
    }
}