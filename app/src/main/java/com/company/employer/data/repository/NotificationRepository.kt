package com.company.employer.data.repository

import com.company.employer.data.model.Notification
import com.company.employer.data.model.UnreadCountResponse
import com.company.employer.data.remote.ApiService
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class NotificationRepository(private val apiService: ApiService) {

    fun getNotifications(page: Int = 1): Flow<Result<List<Notification>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getNotifications(page)
            emit(Result.Success(response.results))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications")
            emit(Result.Error(e.message ?: "Échec de récupération des notifications"))
        }
    }

    fun getUnreadCount(): Flow<Result<Int>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getUnreadNotificationCount()
            emit(Result.Success(response.count))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get unread count")
            emit(Result.Error(e.message ?: "Échec de récupération du compteur"))
        }
    }

    suspend fun markAsRead(notificationId: Int): Result<Unit> {
        return try {
            apiService.markNotificationAsRead(notificationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
            Result.Error(e.message ?: "Échec de marquage comme lu")
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            apiService.markAllNotificationsAsRead()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all as read")
            Result.Error(e.message ?: "Échec de marquage comme lu")
        }
    }
}