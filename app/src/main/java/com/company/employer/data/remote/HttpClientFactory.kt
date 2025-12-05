package com.company.employer.data.remote

import android.content.Context
import com.company.employer.BuildConfig
import com.company.employer.data.local.TokenManager
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import timber.log.Timber

object HttpClientFactory {

    fun create(context: Context, tokenManager: TokenManager): HttpClient {
        return HttpClient(Android) {
            // Base URL configuration
            defaultRequest {
                url(BuildConfig.API_BASE_URL)
                contentType(ContentType.Application.Json)
            }

            // JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    explicitNulls = false
                })
            }

            // Authentication
            install(Auth) {
                bearer {
                    loadTokens {
                        Timber.d("🔄 [Refresh Token] Loading tokens for request")
                        runBlocking {
                            val token = tokenManager.getAccessToken().first()
                            Timber.d("🔄 [Refresh Token] Loaded access token: ${token?.take(10)}... (exists: ${token != null})")
                            token?.let {
                                BearerTokens(accessToken = it, refreshToken = "")
                            } ?: run {
                                Timber.w("🔄 [Refresh Token] No access token found during loadTokens")
                                null
                            }
                        }
                    }

                    refreshTokens {
                        Timber.d("🔄 [Refresh Token] Starting token refresh process")
                        Timber.d("🔄 [Refresh Token] Thread: ${Thread.currentThread().name}")

                        runBlocking {
                            val refreshToken = tokenManager.getRefreshToken().first()
                            Timber.d("🔄 [Refresh Token] Retrieved refresh token from storage: ${refreshToken?.take(10)}...")
                            Timber.d("🔄 [Refresh Token] Refresh token exists: ${refreshToken != null}")

                            if (refreshToken != null) {
                                try {
                                    Timber.d("🔄 [Refresh Token] Sending refresh request to /api/jwt/refresh/")
                                    Timber.d("🔄 [Refresh Token] Using refresh token (first 10 chars): ${refreshToken.take(10)}...")

                                    val response: io.ktor.client.statement.HttpResponse = client.post("/api/jwt/refresh/") {
                                        contentType(ContentType.Application.Json)
                                        setBody(mapOf("refresh" to refreshToken))

                                        // Log the request body
                                        Timber.d("🔄 [Refresh Token] Request body: ${mapOf("refresh" to "${refreshToken.take(10)}...")}")
                                    }

                                    Timber.d("🔄 [Refresh Token] Refresh response status: ${response.status}")
                                    Timber.d("🔄 [Refresh Token] Response headers: ${response.headers}")

                                    if (response.status == HttpStatusCode.OK) {
                                        Timber.d("🔄 [Refresh Token] Refresh successful (200 OK)")

                                        val newToken: kotlinx.serialization.json.JsonObject = response.body()
                                        Timber.d("🔄 [Refresh Token] Response body keys: ${newToken.keys}")

                                        val accessToken = newToken["access"]?.toString()?.trim('"')
                                        Timber.d("🔄 [Refresh Token] New access token raw from response: $accessToken")
                                        Timber.d("🔄 [Refresh Token] New access token length: ${accessToken?.length}")

                                        if (accessToken != null) {
                                            Timber.d("🔄 [Refresh Token] Saving new access token to storage")
                                            tokenManager.saveAccessToken(accessToken)

                                            Timber.d("🔄 [Refresh Token] Returning new BearerTokens")
                                            BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                                        } else {
                                            Timber.e("🔄 [Refresh Token] Access token is null in response!")
                                            null
                                        }
                                    } else {
                                        Timber.e("🔄 [Refresh Token] Refresh failed with status: ${response.status}")
                                        try {
                                            val errorBody: String = response.body()
                                            Timber.e("🔄 [Refresh Token] Error response body: $errorBody")
                                        } catch (e: Exception) {
                                            Timber.e(e, "🔄 [Refresh Token] Failed to read error response body")
                                        }
                                        null
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "🔄 [Refresh Token] Exception during token refresh")
                                    Timber.e("🔄 [Refresh Token] Error type: ${e.javaClass.simpleName}")
                                    Timber.e("🔄 [Refresh Token] Error message: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            } else {
                                Timber.w("🔄 [Refresh Token] No refresh token available for refresh")
                                null
                            }
                        }.also { result ->
                            Timber.d("🔄 [Refresh Token] Refresh tokens result: ${if (result != null) "SUCCESS" else "FAILED"}")
                        }
                    }
                }
            }

            // Enhanced Logging
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (message.contains("refresh", ignoreCase = true) ||
                            message.contains("token", ignoreCase = true) ||
                            message.contains("auth", ignoreCase = true) ||
                            message.contains("401", ignoreCase = true) ||
                            message.contains("jwt", ignoreCase = true)) {
                            Timber.tag("HTTP-AUTH").d("🔄 $message")
                        } else {
                            Timber.tag("HTTP").d(message)
                        }
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            }

            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }

            // Default headers
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }

            // Response logging for auth failures
            HttpResponseValidator {
                validateResponse { response ->
                    val statusCode = response.status.value
                    Timber.d("🔄 [Refresh Token] Response validation:  - Status: $statusCode")

                    if (statusCode == HttpStatusCode.Unauthorized.value) {
                        Timber.w("🔄 [Refresh Token] Received 401 Unauthorized - will trigger token refresh")
                    }
                }

                handleResponseExceptionWithRequest { exception, _ ->
                    Timber.e(exception, "🔄 [Refresh Token] Response exception")
                }
            }
        }
    }
}