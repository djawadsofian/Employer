package com.company.employer.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.employer.data.model.Notification
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun NotificationBadgeButton(
    viewModel: NotificationBadgeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unreadCount = state.notifications.count { !it.isRead }

    Box {
        IconButton(
            onClick = { viewModel.onEvent(NotificationBadgeEvent.ToggleNotificationList) },
            modifier = modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Notification List Dialog
        if (state.showNotificationList) {
            NotificationListDialog(
                notifications = state.notifications,
                unreadCount = unreadCount,
                onNotificationClick = { notification ->
                    viewModel.onEvent(NotificationBadgeEvent.NotificationClicked(notification))
                },
                onMarkAsRead = { notificationId ->
                    viewModel.onEvent(NotificationBadgeEvent.MarkAsRead(notificationId))
                },
                onMarkAllAsRead = {
                    viewModel.onEvent(NotificationBadgeEvent.MarkAllAsRead)
                },
                onDismiss = {
                    viewModel.onEvent(NotificationBadgeEvent.DismissNotificationList)
                }
            )
        }

        // Notification Details Dialog
        state.selectedNotification?.let { notification ->
            NotificationDetailsDialog(
                notification = notification,
                onDismiss = {
                    viewModel.onEvent(NotificationBadgeEvent.DismissNotificationDetails)
                },
                onMarkAsRead = if (!notification.isRead) {
                    {
                        viewModel.onEvent(NotificationBadgeEvent.MarkAsRead(notification.id))
                        viewModel.onEvent(NotificationBadgeEvent.DismissNotificationDetails)
                    }
                } else null
            )
        }
    }
}

@Composable
fun NotificationIcon(
    type: String,
    priority: String,
    modifier: Modifier = Modifier
) {
    // Enhanced icon mapping for ALL notification types
    val icon = when (type) {
        "PROJECT_ASSIGNED" -> Icons.Outlined.Assignment
        "PROJECT_STARTING_SOON" -> Icons.Outlined.Event
        "PROJECT_MODIFIED" -> Icons.Outlined.Edit
        "PROJECT_DELETED" -> Icons.Outlined.Delete
        "MAINTENANCE_STARTING_SOON" -> Icons.Outlined.Build
        "MAINTENANCE_ADDED" -> Icons.Outlined.AddCircle
        "MAINTENANCE_MODIFIED" -> Icons.Outlined.Edit
        "MAINTENANCE_DELETED" -> Icons.Outlined.DeleteOutline
        "LOW_STOCK_ALERT" -> Icons.Outlined.Warning
        "OUT_OF_STOCK_ALERT" -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.Notifications
    }

    val tint = when (priority) {
        "URGENT" -> MaterialTheme.colorScheme.error
        "HIGH" -> MaterialTheme.colorScheme.tertiary
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun NotificationListDialog(
    notifications: List<Notification>,
    unreadCount: Int,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (Int) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount non lu${if (unreadCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (unreadCount > 0) {
                            TextButton(onClick = onMarkAllAsRead) {
                                Text("Tout marquer", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                HorizontalDivider()

                // Notification List
                if (notifications.isEmpty()) {
                    EmptyNotificationsList()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationListItem(
                                notification = notification,
                                onClick = { onNotificationClick(notification) },
                                onMarkAsRead = { onMarkAsRead(notification.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(
    notification: Notification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (!notification.isRead)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                NotificationIcon(
                    type = notification.actualNotificationType,
                    priority = notification.priority,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatNotificationTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mark as read button
            if (!notification.isRead) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onMarkAsRead,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Marquer comme lu",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun EmptyNotificationsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aucune notification",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotificationDetailsDialog(
    notification: Notification,
    onDismiss: () -> Unit,
    onMarkAsRead: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (notification.priority) {
                                "URGENT" -> MaterialTheme.colorScheme.errorContainer
                                "HIGH" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }.copy(alpha = 0.3f)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            NotificationPriorityBadge(priority = notification.priority)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    item { HorizontalDivider() }

                    notification.actualProjectName?.let {
                        item {
                            DetailRow(
                                icon = Icons.Outlined.Work,
                                label = "Projet",
                                value = it
                            )
                        }
                    }

                    notification.actualClientName?.let {
                        item {
                            DetailRow(
                                icon = Icons.Outlined.Person,
                                label = "Client",
                                value = it
                            )
                        }
                    }

                    notification.maintenanceStartDate?.let {
                        item {
                            DetailRow(
                                icon = Icons.Outlined.CalendarToday,
                                label = "Date maintenance",
                                value = it
                            )
                        }
                    }

                    item {
                        DetailRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Date",
                            value = formatFullDate(notification.createdAt)
                        )
                    }
                }

                // Footer
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    onMarkAsRead?.let { markAsReadAction ->
                        FilledTonalButton(
                            onClick = markAsReadAction,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marquer comme lu")
                        }
                    } ?: Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPriorityBadge(priority: String) {
    val (text, color) = when (priority) {
        "URGENT" -> "Urgent" to MaterialTheme.colorScheme.error
        "HIGH" -> "Haute" to MaterialTheme.colorScheme.tertiary
        "MEDIUM" -> "Moyenne" to MaterialTheme.colorScheme.primary
        else -> "Basse" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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