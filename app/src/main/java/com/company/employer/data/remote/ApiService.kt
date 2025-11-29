package com.company.employer.data.remote

import com.company.employer.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ApiService(private val client: HttpClient) {

    suspend fun login(username: String, password: String): LoginResponse {
        return client.post("/api/jwt/create/") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body()
    }

    suspend fun getCurrentUser(): User {
        return client.get("/api/users/me/").body()
    }

    suspend fun getCalendarEvents(
        eventType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): CalendarResponse {
        return client.get("/api/my-calendar/") {
            eventType?.let { parameter("event_type", it) }
            startDate?.let { parameter("start_date", it) }
            endDate?.let { parameter("end_date", it) }
        }.body()
    }

    suspend fun getNotifications(page: Int = 1): NotificationResponse {
        return client.get("/api/notifications/") {
            parameter("page", page)
        }.body()
    }

    suspend fun getUnreadNotificationCount(): UnreadCountResponse {
        return client.get("/api/notifications/unread-count/").body()
    }

    suspend fun markNotificationAsRead(notificationId: Int) {
        client.post("/api/notifications/$notificationId/mark-read/")
    }

    suspend fun markAllNotificationsAsRead() {
        client.post("/api/notifications/mark-all-read/")
    }

    suspend fun updateProfile(request: UpdateProfileRequest): User {
        return client.patch("/api/users/me/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun changePassword(request: ChangePasswordRequest) {
        client.post("/api/users/set_password/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}