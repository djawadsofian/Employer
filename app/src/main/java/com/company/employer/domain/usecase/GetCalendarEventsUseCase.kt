package com.company.employer.domain.usecase

import com.company.employer.data.model.CalendarResponse
import com.company.employer.data.repository.CalendarRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow

class GetCalendarEventsUseCase(private val repository: CalendarRepository) {
    operator fun invoke(
        eventType: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<CalendarResponse>> {
        return repository.getCalendarEvents(eventType, startDate, endDate)
    }
}