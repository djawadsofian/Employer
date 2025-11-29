package com.company.employer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.model.Notification
import com.company.employer.data.repository.NotificationRepository
import com.company.employer.domain.usecase.GetNotificationsUseCase
import com.company.employer.domain.usecase.GetUnreadCountUseCase
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedNotification: Notification? = null
)

sealed class NotificationEvent {
    data object LoadNotifications : NotificationEvent()
    data object LoadUnreadCount : NotificationEvent()
    data class NotificationClicked(val notification: Notification) : NotificationEvent()
    data class MarkAsRead(val notificationId: Int) : NotificationEvent()
    data object MarkAllAsRead : NotificationEvent()
    data object DismissDetails : NotificationEvent()
    data object Refresh : NotificationEvent()
}

class NotificationsViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val repository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.LoadNotifications -> loadNotifications()
            is NotificationEvent.LoadUnreadCount -> loadUnreadCount()
            is NotificationEvent.NotificationClicked -> {
                _state.value = _state.value.copy(selectedNotification = event.notification)
                if (!event.notification.isRead) {
                    markAsRead(event.notification.id)
                }
            }
            is NotificationEvent.MarkAsRead -> markAsRead(event.notificationId)
            is NotificationEvent.MarkAllAsRead -> markAllAsRead()
            is NotificationEvent.DismissDetails -> {
                _state.value = _state.value.copy(selectedNotification = null)
            }
            is NotificationEvent.Refresh -> {
                loadNotifications()
                loadUnreadCount()
            }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            getNotificationsUseCase().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = _state.value.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            notifications = result.data,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            getUnreadCountUseCase().collect { result ->
                if (result is Result.Success) {
                    _state.value = _state.value.copy(unreadCount = result.data)
                }
            }
        }
    }

    private fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            val result = repository.markAsRead(notificationId)
            if (result is Result.Success) {
                loadNotifications()
                loadUnreadCount()
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = repository.markAllAsRead()
            if (result is Result.Success) {
                loadNotifications()
                loadUnreadCount()
            }
        }
    }
}