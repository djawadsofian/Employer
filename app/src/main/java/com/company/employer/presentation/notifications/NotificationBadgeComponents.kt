package com.company.employer.presentation.notifications

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
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
                    if (state.unreadCount > 0) {
                        AnimatedBadge(
                            count = state.unreadCount,
                            hasNewNotification = state.hasNewNotification
                        )
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

        // Notification List Dropdown - only show first 10 notifications
        if (state.showNotificationList) {
            NotificationListDropdown(
                notifications = state.notifications.take(10),
                unreadCount = state.unreadCount,
                onNotificationClick = { notification ->
                    viewModel.onEvent(NotificationBadgeEvent.NotificationClicked(notification))
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
fun AnimatedBadge(count: Int, hasNewNotification: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewNotification) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )

    Badge(
        containerColor = if (hasNewNotification)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
        modifier = Modifier
            .offset(x = (-4).dp, y = 4.dp)
            .scale(scale)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NotificationListDropdown(
    notifications: List<Notification>,
    unreadCount: Int,
    onNotificationClick: (Notification) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use DropdownMenu for proper positioning
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(380.dp)
            .heightIn(max = 500.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    notifications.forEach { notification ->
                        NotificationListItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (!notification.isRead)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon - use actualNotificationType
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
    }
}

@Composable
fun NotificationIcon(
    type: String,
    priority: String,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        "PROJECT_ASSIGNED" -> Icons.Outlined.Assignment
        "PROJECT_STARTING_SOON" -> Icons.Outlined.Event
        "PROJECT_MODIFIED" -> Icons.Outlined.Edit
        "PROJECT_DELETED" -> Icons.Outlined.Delete
        "MAINTENANCE_STARTING_SOON" -> Icons.Outlined.Build
        "MAINTENANCE_ADDED" -> Icons.Outlined.Add
        "MAINTENANCE_MODIFIED" -> Icons.Outlined.Edit
        "MAINTENANCE_DELETED" -> Icons.Outlined.Delete
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
fun EmptyNotificationsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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