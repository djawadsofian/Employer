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
                })
            }

            // Authentication
            install(Auth) {
                bearer {
                    loadTokens {
                        runBlocking {
                            val token = tokenManager.getAccessToken().first()
                            token?.let {
                                BearerTokens(accessToken = it, refreshToken = "")
                            }
                        }
                    }

                    refreshTokens {
                        runBlocking {
                            val refreshToken = tokenManager.getRefreshToken().first()
                            if (refreshToken != null) {
                                try {
                                    val response: io.ktor.client.statement.HttpResponse = client.post("/api/jwt/refresh/") {
                                        contentType(ContentType.Application.Json)
                                        setBody(mapOf("refresh" to refreshToken))
                                    }

                                    if (response.status == HttpStatusCode.OK) {
                                        val newToken: kotlinx.serialization.json.JsonObject = response.body()
                                        val accessToken = newToken["access"]?.toString()?.trim('"')

                                        if (accessToken != null) {
                                            tokenManager.saveAccessToken(accessToken)
                                            BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                                        } else null
                                    } else null
                                } catch (e: Exception) {
                                    Timber.e(e, "Token refresh failed")
                                    null
                                }
                            } else null
                        }
                    }
                }
            }

            // Logging
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("HTTP").d(message)
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
        }
    }
}