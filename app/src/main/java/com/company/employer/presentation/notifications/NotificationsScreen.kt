package com.company.employer.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.employer.data.model.Notification
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (state.notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.onEvent(NotificationEvent.MarkAllAsRead) }
                        ) {
                            Text("Tout marquer comme lu")
                        }
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
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
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onEvent(NotificationEvent.Refresh) }) {
                            Text("Réessayer")
                        }
                    }
                }
                state.notifications.isEmpty() -> {
                    EmptyNotifications(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    NotificationsList(
                        notifications = state.notifications,
                        onNotificationClick = { notification ->
                            viewModel.onEvent(NotificationEvent.NotificationClicked(notification))
                        }
                    )
                }
            }

            // Notification details dialog
            state.selectedNotification?.let { notification ->
                NotificationDetailsDialog(
                    notification = notification,
                    onDismiss = { viewModel.onEvent(NotificationEvent.DismissDetails) },
                    onMarkAsRead = {
                        viewModel.onEvent(NotificationEvent.MarkAsRead(notification.id))
                        viewModel.onEvent(NotificationEvent.DismissDetails)
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationsList(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { notification ->
            NotificationCard(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon based on notification type
            NotificationIcon(
                type = notification.notificationType,
                priority = notification.priority,
                modifier = Modifier.align(Alignment.Top)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Priority badge
                    PriorityBadge(priority = notification.priority)

                    Spacer(modifier = Modifier.weight(1f))

                    // Time
                    Text(
                        text = formatNotificationTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationIcon(
    type: String,
    priority: String,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        "PROJECT_ASSIGNED" -> Icons.Default.Assignment
        "PROJECT_STARTING_SOON" -> Icons.Default.Event
        "PROJECT_MODIFIED" -> Icons.Default.Edit
        "PROJECT_DELETED" -> Icons.Default.Delete
        "MAINTENANCE_STARTING_SOON" -> Icons.Default.Build
        "MAINTENANCE_ADDED" -> Icons.Default.Add
        "MAINTENANCE_MODIFIED" -> Icons.Default.Edit
        "MAINTENANCE_DELETED" -> Icons.Default.Delete
        "LOW_STOCK_ALERT" -> Icons.Default.Warning
        "OUT_OF_STOCK_ALERT" -> Icons.Default.ErrorOutline
        else -> Icons.Default.Notifications
    }

    val tint = when (priority) {
        "URGENT" -> MaterialTheme.colorScheme.error
        "HIGH" -> MaterialTheme.colorScheme.tertiary
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(32.dp),
        tint = tint
    )
}

@Composable
fun PriorityBadge(priority: String) {
    val (text, color) = when (priority) {
        "URGENT" -> "Urgent" to MaterialTheme.colorScheme.error
        "HIGH" -> "Haute" to MaterialTheme.colorScheme.tertiary
        "MEDIUM" -> "Moyenne" to MaterialTheme.colorScheme.primary
        else -> "Basse" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun NotificationDetailsDialog(
    notification: Notification,
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(notification.title) },
        text = {
            Column {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))

                // Additional details
                notification.projectName?.let {
                    DetailRow("Projet", it)
                }

                notification.clientName?.let {
                    DetailRow("Client", it)
                }

                notification.maintenanceStartDate?.let {
                    DetailRow("Date maintenance", it)
                }

                DetailRow("Priorité", notification.priority)
                DetailRow("Date", formatFullDate(notification.createdAt))
            }
        },
        confirmButton = {
            if (!notification.isRead) {
                TextButton(onClick = onMarkAsRead) {
                    Text("Marquer comme lu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucune notification",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vous êtes à jour !",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatNotificationTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val diff = now.epochSecond - instant.epochSecond

        when {
            diff < 60 -> "À l'instant"
            diff < 3600 -> "${diff / 60}m"
            diff < 86400 -> "${diff / 3600}h"
            diff < 604800 -> "${diff / 86400}j"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH)
                formatter.format(instant.atZone(ZoneId.systemDefault()))
            }
        }
    } catch (e: Exception) {
        ""
    }
}

fun formatFullDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        formatter.format(instant.atZone(ZoneId.systemDefault()))
    } catch (e: Exception) {
        isoString
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}