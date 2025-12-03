package com.company.employer.presentation.notifications

import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.local.TokenManager
import com.company.employer.data.model.Notification
import com.company.employer.data.remote.NotificationSseService
import com.company.employer.data.repository.NotificationRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class NotificationBadgeState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNotificationList: Boolean = false,
    val selectedNotification: Notification? = null,
    val lastNotificationId: Int? = null // Track last notification to trigger refresh
)

sealed class NotificationBadgeEvent {
    data object ToggleNotificationList : NotificationBadgeEvent()
    data object DismissNotificationList : NotificationBadgeEvent()
    data class NotificationClicked(val notification: Notification) : NotificationBadgeEvent()
    data object DismissNotificationDetails : NotificationBadgeEvent()
    data class MarkAsRead(val notificationId: Int) : NotificationBadgeEvent()
    data object MarkAllAsRead : NotificationBadgeEvent()
}

class NotificationBadgeViewModel(
    private val notificationRepository: NotificationRepository,
    private val tokenManager: TokenManager,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationBadgeState())
    val state: StateFlow<NotificationBadgeState> = _state.asStateFlow()

    // Shared flow to trigger calendar refresh
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 0)
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()

    private var sseService: NotificationSseService? = null

    init {
        startSseConnection()
    }

    private fun startSseConnection() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken().first()
                if (token != null) {
                    sseService = NotificationSseService(token)
                    sseService?.observeNotifications()?.collect { event ->
                        when (event.event) {
                            "notification" -> {
                                event.data?.let { notification ->
                                    Timber.d("📬 New notification received: ${notification.actualNotificationType} - ${notification.title}")
                                    handleNewNotification(notification)
                                }
                            }
                            "connected" -> {
                                Timber.d("✅ SSE connected successfully")
                            }
                            "ping" -> {
                                Timber.d("🏓 SSE ping received")
                            }
                            else -> {
                                Timber.d("❓ Unknown SSE event: ${event.event}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ SSE connection error")
                _state.value = _state.value.copy(error = "Connexion perdue. Veuillez redémarrer.")
            }
        }
    }

    private fun handleNewNotification(notification: Notification) {
        val currentNotifications = _state.value.notifications.toMutableList()

        // Check if notification already exists (by ID)
        val existingIndex = currentNotifications.indexOfFirst { it.id == notification.id }

        if (existingIndex != -1) {
            // Update existing notification
            Timber.d("🔄 Updating existing notification #${notification.id}")
            currentNotifications[existingIndex] = notification
        } else {
            // Add new notification to top
            Timber.d("➕ Adding new notification #${notification.id}")
            currentNotifications.add(0, notification)

            // Play notification sound for new notifications only
            playNotificationSound()
        }

        // Update state with new notification list
        _state.value = _state.value.copy(
            notifications = currentNotifications,
            lastNotificationId = notification.id
        )

        // Trigger calendar refresh for relevant notification types
        if (shouldTriggerRefresh(notification.actualNotificationType)) {
            Timber.d("🔄 Triggering calendar refresh for: ${notification.actualNotificationType}")
            viewModelScope.launch {
                _refreshTrigger.emit(Unit)
            }
        }
    }

    private fun shouldTriggerRefresh(notificationType: String): Boolean {
        // All notification types that should refresh the calendar
        return when (notificationType) {
            "PROJECT_ASSIGNED",
            "PROJECT_STARTING_SOON",
            "PROJECT_MODIFIED",
            "PROJECT_DELETED",
            "MAINTENANCE_STARTING_SOON",
            "MAINTENANCE_ADDED",
            "MAINTENANCE_MODIFIED",
            "MAINTENANCE_DELETED" -> true
            else -> false
        }
    }

    private fun playNotificationSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
            Timber.d("🔔 Notification sound played")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to play notification sound")
        }
    }

    fun onEvent(event: NotificationBadgeEvent) {
        when (event) {
            is NotificationBadgeEvent.ToggleNotificationList -> {
                _state.value = _state.value.copy(
                    showNotificationList = !_state.value.showNotificationList
                )
            }
            is NotificationBadgeEvent.DismissNotificationList -> {
                _state.value = _state.value.copy(showNotificationList = false)
            }
            is NotificationBadgeEvent.NotificationClicked -> {
                _state.value = _state.value.copy(
                    selectedNotification = event.notification,
                    showNotificationList = false
                )
            }
            is NotificationBadgeEvent.DismissNotificationDetails -> {
                _state.value = _state.value.copy(selectedNotification = null)
            }
            is NotificationBadgeEvent.MarkAsRead -> {
                markAsRead(event.notificationId)
            }
            is NotificationBadgeEvent.MarkAllAsRead -> {
                markAllAsRead()
            }
        }
    }

    private fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)
            if (result is Result.Success) {
                Timber.d("✅ Notification #$notificationId marked as read")
                // Remove the notification from the list (backend handles it)
                val updatedNotifications = _state.value.notifications.filter { it.id != notificationId }
                _state.value = _state.value.copy(
                    notifications = updatedNotifications
                )
            } else if (result is Result.Error) {
                Timber.e("❌ Failed to mark notification as read: ${result.message}")
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result is Result.Success) {
                Timber.d("✅ All notifications marked as read")
                // Clear all notifications (backend handles it)
                _state.value = _state.value.copy(
                    notifications = emptyList()
                )
            } else if (result is Result.Error) {
                Timber.e("❌ Failed to mark all as read: ${result.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseService?.close()
        Timber.d("🛑 SSE service closed")
    }
}