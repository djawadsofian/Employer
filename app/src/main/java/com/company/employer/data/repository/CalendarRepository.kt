package com.company.employer.data.repository

import com.company.employer.data.local.CacheManager
import com.company.employer.data.model.CalendarResponse
import com.company.employer.data.remote.ApiService
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class CalendarRepository(
    private val apiService: ApiService,
    private val cacheManager: CacheManager
) {

    fun getCalendarEvents(
        eventType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<CalendarResponse>> = flow {
        emit(Result.Loading)

        try {
            // Try to fetch from network
            val response = apiService.getCalendarEvents(eventType, startDate, endDate)

            // Cache the result
            cacheManager.cacheCalendar(response)

            emit(Result.Success(response))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get calendar events from network")

            // Try to use cached data
            val cachedCalendar = cacheManager.getCachedCalendar()
            if (cachedCalendar != null) {
                Timber.d("Using cached calendar data (offline mode)")
                emit(Result.Success(cachedCalendar))
            } else {
                emit(Result.Error(e.message ?: "Échec de récupération du calendrier"))
            }
        }
    }
}