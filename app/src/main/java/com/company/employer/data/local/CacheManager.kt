package com.company.employer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.company.employer.data.model.CalendarResponse
import com.company.employer.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "cache_prefs")

class CacheManager(private val context: Context) {

    private val dataStore = context.cacheDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private val USER_CACHE_KEY = stringPreferencesKey("user_cache")
        private val CALENDAR_CACHE_KEY = stringPreferencesKey("calendar_cache")
        private val USER_TIMESTAMP_KEY = stringPreferencesKey("user_timestamp")
        private val CALENDAR_TIMESTAMP_KEY = stringPreferencesKey("calendar_timestamp")
    }

    // Cache User
    suspend fun cacheUser(user: User) {
        try {
            val userJson = json.encodeToString(user)
            dataStore.edit { prefs ->
                prefs[USER_CACHE_KEY] = userJson
                prefs[USER_TIMESTAMP_KEY] = System.currentTimeMillis().toString()
            }
            Timber.d("User cached successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache user")
        }
    }

    // Get Cached User
    suspend fun getCachedUser(): User? {
        return try {
            val prefs = dataStore.data.first()
            val userJson = prefs[USER_CACHE_KEY]
            if (userJson != null) {
                json.decodeFromString<User>(userJson)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cached user")
            null
        }
    }

    // Cache Calendar
    suspend fun cacheCalendar(calendar: CalendarResponse) {
        try {
            val calendarJson = json.encodeToString(calendar)
            dataStore.edit { prefs ->
                prefs[CALENDAR_CACHE_KEY] = calendarJson
                prefs[CALENDAR_TIMESTAMP_KEY] = System.currentTimeMillis().toString()
            }
            Timber.d("Calendar cached successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache calendar")
        }
    }

    // Get Cached Calendar
    suspend fun getCachedCalendar(): CalendarResponse? {
        return try {
            val prefs = dataStore.data.first()
            val calendarJson = prefs[CALENDAR_CACHE_KEY]
            if (calendarJson != null) {
                json.decodeFromString<CalendarResponse>(calendarJson)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cached calendar")
            null
        }
    }

    // Check if cache is fresh (less than 5 minutes old)
    suspend fun isUserCacheFresh(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            val timestamp = prefs[USER_TIMESTAMP_KEY]?.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            val ageMinutes = (now - timestamp) / 60_000
            ageMinutes < 5
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isCalendarCacheFresh(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            val timestamp = prefs[CALENDAR_TIMESTAMP_KEY]?.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            val ageMinutes = (now - timestamp) / 60_000
            ageMinutes < 5
        } catch (e: Exception) {
            false
        }
    }

    // Clear all cache
    suspend fun clearCache() {
        try {
            dataStore.edit { prefs ->
                prefs.remove(USER_CACHE_KEY)
                prefs.remove(CALENDAR_CACHE_KEY)
                prefs.remove(USER_TIMESTAMP_KEY)
                prefs.remove(CALENDAR_TIMESTAMP_KEY)
            }
            Timber.d("Cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
        }
    }
}