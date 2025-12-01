package com.company.employer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Notification>
)

@Serializable
data class Notification(
    val id: Int,

    // Handle both "notification_type" and "type" from backend
    @SerialName("notification_type")
    val notificationType: String? = null,
    @SerialName("type")
    private val typeAlias: String? = null,

    val title: String,
    val message: String,
    val priority: String,

    @SerialName("is_read")
    val isRead: Boolean,
    @SerialName("read_at")
    val readAt: String? = null,

    @SerialName("is_confirmed")
    val isConfirmed: Boolean,
    @SerialName("confirmed_at")
    val confirmedAt: String? = null,

    @SerialName("created_at")
    val createdAt: String,
    @SerialName("sent_at")
    val sentAt: String? = null,
    @SerialName("last_sent_at")
    val lastSentAt: String? = null,
    @SerialName("send_count")
    val sendCount: Int? = null,

    val data: Map<String, String>? = null,

    // Project info - can come from different places
    @SerialName("related_project")
    val relatedProject: Int? = null,
    @SerialName("project_name")
    val projectName: String? = null,
    @SerialName("client_name")
    val clientName: String? = null,

    // Or from nested project object
    val project: ProjectInfo? = null,

    @SerialName("related_maintenance")
    val relatedMaintenance: Int? = null,
    @SerialName("maintenance_start_date")
    val maintenanceStartDate: String? = null,

    @SerialName("related_product")
    val relatedProduct: Int? = null,
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("product_quantity")
    val productQuantity: Int? = null,

    @SerialName("age_in_seconds")
    val ageInSeconds: Int? = null,
    @SerialName("is_urgent")
    val isUrgent: Boolean? = null,
    @SerialName("requires_confirmation")
    val requiresConfirmation: Boolean? = null
) {
    // Computed property to get the actual notification type
    val actualNotificationType: String
        get() = typeAlias ?: notificationType ?: "UNKNOWN"

    // Get project name from either direct field or nested object
    val actualProjectName: String?
        get() = projectName ?: project?.name

    // Get client name from either direct field or data map
    val actualClientName: String?
        get() = clientName ?: data?.get("client_name")
}

@Serializable
data class ProjectInfo(
    val id: Int,
    val name: String
)

@Serializable
data class UnreadCountResponse(
    val count: Int
)