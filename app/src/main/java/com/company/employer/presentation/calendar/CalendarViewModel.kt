package com.company.employer.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.model.CalendarEvent as DataCalendarEvent
import com.company.employer.data.repository.AuthRepository
import com.company.employer.domain.usecase.GetCalendarEventsUseCase
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarState(
    val events: List<DataCalendarEvent> = emptyList(),
    val filteredEvents: List<DataCalendarEvent> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedEventType: EventTypeFilter = EventTypeFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedEvent: DataCalendarEvent? = null,
    val userName: String = ""
)

enum class EventTypeFilter {
    ALL, PROJECT, MAINTENANCE
}

sealed class CalendarUiEvent {
    data object LoadEvents : CalendarUiEvent()
    data class DateSelected(val date: LocalDate) : CalendarUiEvent()
    data class EventTypeSelected(val type: EventTypeFilter) : CalendarUiEvent()
    data class EventClicked(val event: DataCalendarEvent) : CalendarUiEvent()
    data object DismissEventDetails : CalendarUiEvent()
    data object Refresh : CalendarUiEvent()
}

class CalendarViewModel(
    private val getCalendarEventsUseCase: GetCalendarEventsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        loadEvents()
        loadUserName()
    }

    fun onEvent(event: CalendarUiEvent) {
        when (event) {
            is CalendarUiEvent.LoadEvents -> loadEvents()
            is CalendarUiEvent.DateSelected -> {
                _state.value = _state.value.copy(selectedDate = event.date)
                filterEventsByDate(event.date)
            }
            is CalendarUiEvent.EventTypeSelected -> {
                _state.value = _state.value.copy(selectedEventType = event.type)
                filterEvents()
            }
            is CalendarUiEvent.EventClicked -> {
                _state.value = _state.value.copy(selectedEvent = event.event)
            }
            is CalendarUiEvent.DismissEventDetails -> {
                _state.value = _state.value.copy(selectedEvent = null)
            }
            is CalendarUiEvent.Refresh -> loadEvents()
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            getCalendarEventsUseCase().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = _state.value.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            events = result.data.events,
                            filteredEvents = result.data.events,
                            error = null
                        )
                        filterEvents()
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadUserName() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { result ->
                if (result is Result.Success) {
                    val fullName = "${result.data.firstName} ${result.data.lastName}".trim()
                    _state.value = _state.value.copy(userName = fullName.ifEmpty { result.data.username })
                }
            }
        }
    }

    private fun filterEventsByDate(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val filtered = _state.value.events.filter { event ->
            event.start.startsWith(dateStr)
        }
        _state.value = _state.value.copy(filteredEvents = filtered)
    }

    private fun filterEvents() {
        val currentState = _state.value
        val filtered = when (currentState.selectedEventType) {
            EventTypeFilter.ALL -> currentState.events
            EventTypeFilter.PROJECT -> currentState.events.filter { it.type == "project" }
            EventTypeFilter.MAINTENANCE -> currentState.events.filter { it.type == "maintenance" }
        }
        _state.value = currentState.copy(filteredEvents = filtered)
    }
}