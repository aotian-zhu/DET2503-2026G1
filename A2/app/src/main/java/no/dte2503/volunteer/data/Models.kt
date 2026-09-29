package no.dte2503.volunteer.data

data class GeoPointData(val latitude: Double, val longitude: Double)

data class SupportScenario(
    val id: String,
    val name: String,
    val description: String,
)

data class Permission(
    val id: String,
    val label: String,
)

data class VolunteerFunction(
    val id: String,
    val name: String,
    val description: String,
    val scenarioIds: Set<String>,
    val permissionIds: Set<String>,
)

data class VolunteerProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val assignedFunctionIds: Set<String>,
)

data class Shift(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val meetingPointId: String,
)

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

data class TaskPlace(
    val position: GeoPointData,
    val area: String,
    val floor: String?,
)

data class VolunteerTask(
    val id: String,
    val title: String,
    val details: String,
    val locationId: String,
    val place: TaskPlace,
    val scenarioId: String,
    val requiredPermissionIds: Set<String>,
    val dueTime: String,
    val status: TaskStatus,
)

data class ServiceLocation(
    val id: String,
    val name: String,
    val description: String,
    val position: GeoPointData,
    val scenarioIds: Set<String>,
)

data class LocationWithDistance(
    val location: ServiceLocation,
    val distanceMetres: Float?,
)

data class TaskWithDistance(
    val task: VolunteerTask,
    val distanceMetres: Float?,
)

enum class MessagePriority { NORMAL, IMPORTANT, URGENT }

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val sender: String,
    val time: String,
    val priority: MessagePriority,
    val isRead: Boolean,
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val body: String,
    val time: String,
    val isFromVolunteer: Boolean,
)

data class TaskConversation(
    val id: String,
    val taskId: String,
    val coordinatorName: String,
    val messages: List<ChatMessage>,
    val unreadCount: Int,
)
