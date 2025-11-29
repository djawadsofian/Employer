package com.company.employer.data.repository

import com.company.employer.data.model.CalendarResponse
import com.company.employer.data.remote.ApiService
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class CalendarRepository(private val apiService: ApiService) {

    fun getCalendarEvents(
        eventType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<CalendarResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getCalendarEvents(eventType, startDate, endDate)
            emit(Result.Success(response))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get calendar events")
            emit(Result.Error(e.message ?: "Échec de récupération du calendrier"))
        }
    }
}