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

    // Debounce calendar refresh events
    private val _calendarRefreshEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val calendarRefreshEvent: SharedFlow<Unit> = _calendarRefreshEvent.asSharedFlow()

    // Track notification IDs to avoid duplicate sounds
    private val notifiedIds = mutableSetOf<Int>()

    // Debounce sound notifications
    private val _soundNotificationEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val soundNotificationEvent: SharedFlow<Unit> = _soundNotificationEvent.asSharedFlow()

    init {
        startSseConnection()
        setupDebouncedRefresh()
    }

    private fun setupDebouncedRefresh() {
        viewModelScope.launch {
            // Debounce calendar refresh by 2 seconds
            _calendarRefreshEvent
                .debounce(2000)
                .collect {
                    Timber.d("Triggering debounced calendar refresh")
                }
        }
    }

    private fun startSseConnection() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken().first()
                if (token != null) {
                    sseService = NotificationSseService(token)
                    sseService?.observeNotifications()?.collect { event ->
                        if (event.event == "notification" && event.data != null) {
                            Timber.d("New notification received via SSE: ${event.data.title}")
                            handleNewNotification(event.data)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "SSE connection error")
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
            currentNotifications[existingIndex] = notification
        } else {
            // Add new notification to top
            currentNotifications.add(0, notification)
        }

        _state.value = _state.value.copy(
            notifications = currentNotifications
        )

        // Only trigger refresh and sound for truly new notifications after initial load
        if (isInitialLoadComplete && isNewNotification) {
            // Check if we haven't already notified for this ID
            if (notifiedIds.add(notification.id)) {
                // Trigger sound notification (debounced)
                viewModelScope.launch {
                    _soundNotificationEvent.emit(Unit)

                    // Clean up old IDs after 5 minutes to prevent memory leak
                    delay(300_000)
                    notifiedIds.remove(notification.id)
                }
            }

            // Trigger calendar refresh (debounced)
            viewModelScope.launch {
                _calendarRefreshEvent.emit(Unit)
            }
        } else if (!isInitialLoadComplete) {
            // Mark initial load as complete after first batch
            viewModelScope.launch {
                delay(3000) // Wait 3 seconds after app start
                isInitialLoadComplete = true
                Timber.d("Initial notification load complete")
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
                // Remove the notification from the list (backend handles it)
                val updatedNotifications = _state.value.notifications.filter { it.id != notificationId }
                _state.value = _state.value.copy(
                    notifications = updatedNotifications
                )
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result is Result.Success) {
                // Clear all notifications (backend handles it)
                _state.value = _state.value.copy(
                    notifications = emptyList()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseService?.close()
        notifiedIds.clear()
    }
}