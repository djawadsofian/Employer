package com.company.employer.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.employer.data.model.CalendarEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToNotifications: () -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Calendrier") },
                actions = {
                    // Event type filter
                    FilterChip(
                        selected = state.selectedEventType != EventTypeFilter.ALL,
                        onClick = { /* Show filter menu */ },
                        label = {
                            Text(when (state.selectedEventType) {
                                EventTypeFilter.ALL -> "Tous"
                                EventTypeFilter.PROJECT -> "Projets"
                                EventTypeFilter.MAINTENANCE -> "Maintenances"
                            })
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error!!,
                        onRetry = { viewModel.onEvent(CalendarUiEvent.Refresh) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Calendar view
                        CalendarView(
                            events = state.events,
                            selectedDate = state.selectedDate,
                            onDateSelected = { date ->
                                viewModel.onEvent(CalendarUiEvent.DateSelected(date))
                            }
                        )

                        Divider()

                        // Events list for selected date
                        EventsList(
                            events = state.filteredEvents,
                            onEventClick = { event ->
                                viewModel.onEvent(CalendarUiEvent.EventClicked(event))
                            }
                        )
                    }
                }
            }

            // Event details dialog
            state.selectedEvent?.let { event ->
                EventDetailsDialog(
                    event = event,
                    onDismiss = { viewModel.onEvent(CalendarUiEvent.DismissEventDetails) }
                )
            }
        }
    }
}

@Composable
fun CalendarView(
    events: List<CalendarEvent>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val firstDayOfWeek = remember { DayOfWeek.MONDAY }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Previous month */ }) {
                Icon(Icons.Default.KeyboardArrowLeft, "Mois précédent")
            }

            Text(
                text = calendarState.firstVisibleMonth.yearMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
                ).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { /* Next month */ }) {
                Icon(Icons.Default.KeyboardArrowRight, "Mois suivant")
            }
        }

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                        .uppercase(),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar grid
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                Day(
                    day = day,
                    isSelected = day.date == selectedDate,
                    hasEvents = events.any { event ->
                        event.start.startsWith(day.date.toString())
                    },
                    onClick = { onDateSelected(day.date) }
                )
            }
        )
    }
}

@Composable
fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    day.position == DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (hasEvents && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun EventsList(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucun événement pour cette date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                EventCard(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.type == "project")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (event.type == "project")
                    Icons.Default.Work
                else
                    Icons.Default.Build,
                contentDescription = null,
                tint = if (event.type == "project")
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (event.type == "project")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Client: ${event.clientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.type == "project")
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )

                if (event.type == "maintenance") {
                    Text(
                        text = "Type: ${event.maintenanceType ?: "Standard"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Voir détails",
                tint = if (event.type == "project")
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EventDetailsDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.projectName,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                DetailRow("Type", if (event.type == "project") "Projet" else "Maintenance")
                DetailRow("Client", event.clientName)
                DetailRow("Début", event.start)
                event.end?.let { DetailRow("Fin", it) }
                event.status?.let { DetailRow("Statut", it) }
                event.clientAddress.let { address ->
                    DetailRow("Adresse", "${address.city}, ${address.province}")
                }
                event.maintenanceType?.let { DetailRow("Type maintenance", it) }
                event.isOverdue?.let {
                    if (it) DetailRow("⚠️", "Maintenance en retard", isWarning = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}