package com.company.employer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarResponse(
    @SerialName("user_role") val userRole: String,
    @SerialName("user_name") val userName: String,
    @SerialName("total_events") val totalEvents: Int,
    @SerialName("applied_filters") val appliedFilters: Map<String, String> = emptyMap(),
    val events: List<CalendarEvent>
)

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val start: String,
    val end: String? = null,
    val type: String, // "project" or "maintenance"
    @SerialName("project_id") val projectId: Int,
    @SerialName("project_name") val projectName: String,
    @SerialName("client_name") val clientName: String,
    @SerialName("client_address") val clientAddress: ClientAddress,
    val team: List<String> = emptyList(),
    val status: String? = null,
    @SerialName("is_verified") val isVerified: Boolean? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("duration_days") val durationDays: Int? = null,
    @SerialName("progress_percentage") val progressPercentage: Double? = null,
    @SerialName("maintenance_id") val maintenanceId: Int? = null,
    @SerialName("maintenance_type") val maintenanceType: String? = null,
    @SerialName("is_overdue") val isOverdue: Boolean? = null,
    @SerialName("days_until_maintenance") val daysUntilMaintenance: Int? = null
)

@Serializable
data class ClientAddress(
    val province: String,
    val city: String,
    @SerialName("postal_code") val postalCode: String
)