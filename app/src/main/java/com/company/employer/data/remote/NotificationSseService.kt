package com.company.employer.data.remote

import android.util.Log
import com.company.employer.BuildConfig
import com.company.employer.data.model.Notification

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

data class SseNotificationEvent(
    val event: String,
    val data: Notification? = null
)

class NotificationSseService(private val accessToken: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun observeNotifications(): Flow<SseNotificationEvent> = callbackFlow {
        val url = "${BuildConfig.API_BASE_URL}/api/notifications/stream/?token=$accessToken"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("SSE", "Connection opened")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    Log.d("SSE", "Received event: $data")
                    val eventData = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(data)

                    val eventType = eventData["event"]?.toString()?.trim('"') ?: ""

                    if (eventType == "notification") {
                        val notificationData = eventData["data"]?.toString() ?: ""
                        val notification = json.decodeFromString<Notification>(notificationData)

                        trySend(SseNotificationEvent(
                            event = eventType,
                            data = notification
                        ))
                    }
                } catch (e: Exception) {
                    Log.e("SSE", "Error parsing event", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("SSE", "Connection closed")
                channel.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("SSE", "Connection failed", t)
                channel.close(t)
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(request, eventSourceListener)

        awaitClose {
            Log.d("SSE", "Closing event source")
            eventSource.cancel()
        }
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}