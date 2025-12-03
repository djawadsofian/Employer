package com.company.employer.data.remote

import android.util.Log
import com.company.employer.BuildConfig
import com.company.employer.data.model.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
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

@Serializable
data class SseWrapper(
    val event: String,
    val data: Notification? = null,
    val timestamp: Long? = null
)

class NotificationSseService(private val accessToken: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // FIXED: More lenient JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun observeNotifications(): Flow<SseNotificationEvent> = callbackFlow {
        val url = "${BuildConfig.API_BASE_URL}api/notifications/stream/?token=$accessToken"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("SSE", "✅ Connection opened")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    Log.d("SSE", "📨 Received raw event - type: $type, data: $data")

                    // Parse the wrapper which contains the actual event type and notification data
                    val wrapper = json.decodeFromString<SseWrapper>(data)

                    when (wrapper.event) {
                        "connected" -> {
                            Log.d("SSE", "✅ Connected to SSE stream")
                        }
                        "notification" -> {
                            // The notification is in wrapper.data
                            wrapper.data?.let { notification ->
                                Log.d("SSE", "✅ Parsed notification: ${notification.title}")
                                Log.d("SSE", "   Type: ${notification.actualNotificationType}")
                                Log.d("SSE", "   Priority: ${notification.priority}")

                                trySend(SseNotificationEvent(
                                    event = "notification",
                                    data = notification
                                ))
                            } ?: Log.e("SSE", "❌ Notification event with null data")
                        }
                        "ping" -> {
                            Log.d("SSE", "🏓 Received ping at ${wrapper.timestamp}")
                        }
                        else -> {
                            Log.d("SSE", "❓ Unknown wrapper event type: ${wrapper.event}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SSE", "❌ Error parsing event: ${e.message}", e)
                    Log.e("SSE", "Raw data was: $data")
                    // Don't close the channel on parsing errors - just skip this notification
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("SSE", "🔌 Connection closed")
                channel.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("SSE", "❌ Connection failed", t)
                channel.close(t)
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(request, eventSourceListener)

        awaitClose {
            Log.d("SSE", "🛑 Closing event source")
            eventSource.cancel()
        }
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}