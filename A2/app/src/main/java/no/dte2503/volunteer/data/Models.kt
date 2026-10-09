package no.dte2503.volunteer.data

import kotlinx.serialization.Serializable

@Serializable data class GeoPointData(val latitude: Double, val longitude: Double)
data class SupportScenario(val id: String, val name: String, val description: String)
data class Permission(val id: String, val label: String)
data class VolunteerFunction(val id: String, val name: String, val description: String, val scenarioIds: Set<String>, val permissionIds: Set<String>)
@Serializable data class VolunteerProfile(val id: String, val displayName: String, val email: String, val phone: String, val assignedFunctionIds: Set<String>)
@Serializable data class Shift(val id: String, val title: String, val startTime: String, val endTime: String, val meetingPointId: String)
@Serializable enum class TaskStatus { TODO, IN_PROGRESS, DONE }
@Serializable data class TaskPlace(val position: GeoPointData, val area: String, val floor: String?)
@Serializable data class VolunteerTask(val id: String, val title: String, val details: String, val locationId: String, val place: TaskPlace, val scenarioId: String, val requiredPermissionIds: Set<String>, val dueTime: String, val status: TaskStatus)
@Serializable data class ServiceLocation(val id: String, val name: String, val description: String, val position: GeoPointData, val scenarioIds: Set<String>)
data class LocationWithDistance(val location: ServiceLocation, val distanceMetres: Float?)
data class TaskWithDistance(val task: VolunteerTask, val distanceMetres: Float?)
@Serializable enum class MessagePriority { NORMAL, IMPORTANT, URGENT }
@Serializable data class Announcement(val id: String, val title: String, val body: String, val sender: String, val time: String, val priority: MessagePriority, val isRead: Boolean)
@Serializable data class ChatMessage(val id: String, val sender: String, val body: String, val time: String, val isFromVolunteer: Boolean)
@Serializable data class TaskConversation(val id: String, val taskId: String, val coordinatorName: String, val messages: List<ChatMessage>, val unreadCount: Int)
data class CheckInResult(val id: String, val checkedInAt: String, val duplicate: Boolean)
@Serializable enum class IncidentCategory { CROWD, SAFETY, MEDICAL, SIGNAGE, ACCESSIBILITY, OTHER }
@Serializable data class IncidentDraft(val category: IncidentCategory, val description: String, val locationId: String?, val taskId: String?)
data class IncidentReceipt(val id: String, val submittedAt: String, val photoPath: String?)
