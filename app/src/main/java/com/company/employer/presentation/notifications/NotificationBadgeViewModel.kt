package com.company.employer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.local.TokenManager
import com.company.employer.data.model.Notification
import com.company.employer.data.remote.NotificationSseService
import com.company.employer.data.repository.NotificationRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import timber.log.Timber

data class NotificationBadgeState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNotificationList: Boolean = false,
    val selectedNotification: Notification? = null
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
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationBadgeState())
    val state: StateFlow<NotificationBadgeState> = _state.asStateFlow()

    private var sseService: NotificationSseService? = null

    // Track if initial load is complete to avoid refresh spam
    private var isInitialLoadComplete = false

    // Calendar refresh event - no debounce, emit immediately
    private val _calendarRefreshEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val calendarRefreshEvent: SharedFlow<Unit> = _calendarRefreshEvent.asSharedFlow()

    // Track notification IDs to avoid duplicate sounds
    private val notifiedIds = mutableSetOf<Int>()

    // Sound notification event - emit immediately, no debounce
    private val _soundNotificationEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val soundNotificationEvent: SharedFlow<Unit> = _soundNotificationEvent.asSharedFlow()

    init {
        startSseConnection()
        // Mark initial load as complete after 5 seconds
        viewModelScope.launch {
            delay(5000)
            isInitialLoadComplete = true
            Timber.d("✅ Initial notification load period complete - will now play sounds for new notifications")
        }
    }

    private fun startSseConnection() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken().first()
                if (token != null) {
                    Timber.d("🔌 Starting SSE connection for notifications")
                    sseService = NotificationSseService(token)
                    sseService?.observeNotifications()?.collect { event ->
                        if (event.event == "notification" && event.data != null) {
                            Timber.d("📨 New notification received via SSE: ${event.data.title}")
                            handleNewNotification(event.data)
                        }
                    }
                } else {
                    Timber.e("❌ No access token available for SSE")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ SSE connection error")
            }
        }
    }

    private fun handleNewNotification(notification: Notification) {
        val currentNotifications = _state.value.notifications.toMutableList()

        // Check if notification already exists (by ID)
        val existingIndex = currentNotifications.indexOfFirst { it.id == notification.id }

        val isNewNotification = existingIndex == -1

        if (existingIndex != -1) {
            // Update existing notification
            Timber.d("🔄 Updating existing notification ID: ${notification.id}")
            currentNotifications[existingIndex] = notification
        } else {
            // Add new notification to top
            Timber.d("➕ Adding new notification ID: ${notification.id}")
            currentNotifications.add(0, notification)
        }

        _state.value = _state.value.copy(
            notifications = currentNotifications
        )

        // Only trigger refresh and sound for truly new notifications after initial load
        if (isInitialLoadComplete && isNewNotification) {
            Timber.d("🔔 Processing new notification - initial load complete: $isInitialLoadComplete")

            // Check if we haven't already notified for this ID
            if (notifiedIds.add(notification.id)) {
                Timber.d("🔊 Playing sound for notification ID: ${notification.id}")

                // Trigger sound notification immediately
                viewModelScope.launch {
                    val emitResult = _soundNotificationEvent.tryEmit(Unit)
                    Timber.d("🔊 Sound emission result: $emitResult")

                    // Clean up old IDs after 5 minutes to prevent memory leak
                    delay(300_000)
                    notifiedIds.remove(notification.id)
                }
            } else {
                Timber.d("🔇 Skipping sound for notification ID: ${notification.id} (already notified)")
            }

            // Trigger calendar refresh immediately
            viewModelScope.launch {
                val emitResult = _calendarRefreshEvent.tryEmit(Unit)
                Timber.d("🔄 Calendar refresh emission result: $emitResult")
            }
        } else {
            if (!isInitialLoadComplete) {
                Timber.d("⏳ Skipping sound/refresh - still in initial load period")
            } else if (!isNewNotification) {
                Timber.d("🔄 Skipping sound/refresh - notification is an update, not new")
            }
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
                Timber.d("✅ Marked notification $notificationId as read")
                // Remove the notification from the list
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
                Timber.d("✅ Marked all notifications as read")
                // Clear all notifications
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
        notifiedIds.clear()
        Timber.d("🧹 NotificationBadgeViewModel cleared")
    }
}