package com.company.employer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.local.TokenManager
import com.company.employer.data.model.Notification
import com.company.employer.data.remote.NotificationSseService
import com.company.employer.data.repository.NotificationRepository
import com.company.employer.domain.usecase.GetNotificationsUseCase
import com.company.employer.domain.usecase.GetUnreadCountUseCase
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class NotificationBadgeState(
    val unreadCount: Int = 0,
    val hasNewNotification: Boolean = false,
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
    data object LoadNotifications : NotificationBadgeEvent()
}

class NotificationBadgeViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val notificationRepository: NotificationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationBadgeState())
    val state: StateFlow<NotificationBadgeState> = _state.asStateFlow()

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
        currentNotifications.add(0, notification) // Add to top

        val newUnreadCount = if (!notification.isRead) {
            _state.value.unreadCount + 1
        } else {
            _state.value.unreadCount
        }

        _state.value = _state.value.copy(
            notifications = currentNotifications,
            hasNewNotification = true,
            unreadCount = newUnreadCount
        )

        // Auto-hide the "new notification" indicator after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _state.value = _state.value.copy(hasNewNotification = false)
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
            is NotificationBadgeEvent.LoadNotifications -> {
                // No longer needed - we only use stream data
            }
        }
    }

    private fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)
            if (result is Result.Success) {
                // Update local state
                val updatedNotifications = _state.value.notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                _state.value = _state.value.copy(
                    notifications = updatedNotifications,
                    unreadCount = maxOf(0, _state.value.unreadCount - 1)
                )
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result is Result.Success) {
                val updatedNotifications = _state.value.notifications.map {
                    it.copy(isRead = true)
                }
                _state.value = _state.value.copy(
                    notifications = updatedNotifications,
                    unreadCount = 0
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseService?.close()
    }
}