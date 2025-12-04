package com.company.employer.data.remote

import com.company.employer.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(
    val message: String? = null,
    val detail: String? = null,
    val error: String? = null
)

class ApiService(private val client: HttpClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun login(username: String, password: String): LoginResponse {
        val response: HttpResponse = client.post("/api/jwt/create/") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            val errorBody = response.bodyAsText()
            val errorResponse = try {
                json.decodeFromString<ErrorResponse>(errorBody)
            } catch (e: Exception) {
                ErrorResponse(message = "Erreur de connexion")
            }

            // Apply the comments: if message is "Non authentifié" or "No active account found"
            // show specific message, otherwise display backend message
            val errorMessage = when {
                errorResponse.message?.contains("Non authentifié", ignoreCase = true) == true ||
                        errorResponse.message?.contains("No active account found", ignoreCase = true) == true ||
                        errorResponse.message?.contains("Invalid credentials", ignoreCase = true) == true ||
                        errorResponse.detail?.contains("Invalid credentials", ignoreCase = true) == true -> {
                    "Nom d'utilisateur ou mot de passe incorrect"
                }
                !errorResponse.message.isNullOrBlank() -> {
                    errorResponse.message
                }
                !errorResponse.detail.isNullOrBlank() -> {
                    errorResponse.detail
                }
                else -> "Échec de connexion"
            }

            throw Exception(errorMessage)
        }
    }

    suspend fun getCurrentUser(): User {
        return client.get("/api/users/me/").body()
    }

    suspend fun getCalendarEvents(
        eventType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): CalendarResponse {
        return client.get("/api/my-calendar/?is_verified=true") {
            eventType?.let { parameter("event_type", it) }
            startDate?.let { parameter("start_date", it) }
            endDate?.let { parameter("end_date", it) }
        }.body()
    }

    suspend fun markNotificationAsRead(notificationId: Int) {
        client.post("/api/notifications/$notificationId/mark_read/")
    }

    suspend fun markAllNotificationsAsRead() {
        client.post("/api/notifications/mark_all_read/")
    }

    suspend fun updateProfile(request: UpdateProfileRequest): User {
        return client.patch("/api/users/me/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun changePassword(request: ChangePasswordRequest) {
        val response: HttpResponse = client.post("/api/users/set_password/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val errorResponse = try {
                json.decodeFromString<ErrorResponse>(errorBody)
            } catch (e: Exception) {
                ErrorResponse(message = "Erreur lors du changement de mot de passe")
            }

            // Apply translations based on error messages
            val errorMessage = when {
                // "Donnée invalide" --> "mot de passe actuel est incorrecte"
                errorResponse.message?.contains("Donnée invalide", ignoreCase = true) == true -> {
                    "Mot de passe actuel incorrect"
                }

                // Password validation errors - translate common Django REST Framework messages
                errorResponse.message?.contains("This password is too short", ignoreCase = true) == true -> {
                    "Ce mot de passe est trop court. Il doit contenir au moins 8 caractères."
                }
                errorResponse.message?.contains("too similar", ignoreCase = true) == true -> {
                    "Le nouveau mot de passe est trop similaire aux informations personnelles."
                }
                errorResponse.message?.contains("too common", ignoreCase = true) == true -> {
                    "Ce mot de passe est trop commun."
                }
                errorResponse.message?.contains("entirely numeric", ignoreCase = true) == true -> {
                    "Le mot de passe ne peut pas être entièrement numérique."
                }
                errorResponse.message?.contains("The two password fields didn't match", ignoreCase = true) == true -> {
                    "Les deux mots de passe ne correspondent pas."
                }
                errorResponse.message?.contains("current password is incorrect", ignoreCase = true) == true -> {
                    "Le mot de passe actuel est incorrect."
                }

                // Use backend message if available
                !errorResponse.message.isNullOrBlank() -> errorResponse.message
                !errorResponse.detail.isNullOrBlank() -> errorResponse.detail

                // Default fallback
                else -> "Échec de changement du mot de passe"
            }

            throw Exception(errorMessage)
        }
    }
}