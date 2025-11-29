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
    @SerialName("notification_type") val notificationType: String,
    val title: String,
    val message: String,
    val priority: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("read_at") val readAt: String?,
    @SerialName("is_confirmed") val isConfirmed: Boolean,
    @SerialName("confirmed_at") val confirmedAt: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("sent_at") val sentAt: String?,
    @SerialName("last_sent_at") val lastSentAt: String?,
    @SerialName("send_count") val sendCount: Int,
    val data: Map<String, String>? = null,
    @SerialName("related_project") val relatedProject: Int?,
    @SerialName("related_maintenance") val relatedMaintenance: Int?,
    @SerialName("related_product") val relatedProduct: Int?,
    @SerialName("project_name") val projectName: String?,
    @SerialName("client_name") val clientName: String?,
    @SerialName("maintenance_start_date") val maintenanceStartDate: String?,
    @SerialName("product_name") val productName: String?,
    @SerialName("product_quantity") val productQuantity: Int?,
    @SerialName("age_in_seconds") val ageInSeconds: Int,
    @SerialName("is_urgent") val isUrgent: Boolean,
    @SerialName("requires_confirmation") val requiresConfirmation: Boolean
)

@Serializable
data class UnreadCountResponse(
    val count: Int
)