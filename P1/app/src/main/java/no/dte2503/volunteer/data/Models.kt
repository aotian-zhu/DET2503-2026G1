package no.dte2503.volunteer.data

import kotlinx.serialization.Serializable

@Serializable data class GeoPointData(val latitude: Double, val longitude: Double)
@Serializable data class SupportScenario(val id: String, val name: String, val description: String)
@Serializable data class Permission(val id: String, val label: String)
@Serializable data class VolunteerFunction(val id: String, val name: String, val description: String, val scenarioIds: Set<String>, val permissionIds: Set<String>)
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
@Serializable data class CheckInResult(val id: String, val checkedInAt: String, val duplicate: Boolean)
@Serializable enum class IncidentCategory { CROWD, SAFETY, MEDICAL, SIGNAGE, ACCESSIBILITY, OTHER }
@Serializable data class IncidentDraft(val category: IncidentCategory = IncidentCategory.OTHER, val description: String = "", val locationId: String? = null, val taskId: String? = null, val photoUri: String? = null)
@Serializable data class IncidentReceipt(val id: String, val submittedAt: String, val photoPath: String?)
@Serializable data class SavedIncident(val draft: IncidentDraft, val receipt: IncidentReceipt)
@Serializable data class VolunteerData(val profile: VolunteerProfile, val shift: Shift, val locations: List<ServiceLocation>, val tasks: List<VolunteerTask>, val announcements: List<Announcement>, val conversations: List<TaskConversation>)
@Serializable enum class SessionMode { NONE, DEMO, REMOTE }
@Serializable enum class ThemeMode { SYSTEM, LIGHT, DARK }
@Serializable data class WeatherSnapshot(val temperatureC: Double, val feelsLikeC: Double, val windKmh: Double, val weatherCode: Int, val observedAt: String, val fetchedAtMillis: Long)
@Serializable data class TaskUpdate(val id: String, val taskId: String, val title: String, val previousStatus: TaskStatus, val status: TaskStatus, val changedAtMillis: Long)
@Serializable data class DeviceTestObservation(val scenarioId: String, val observed: String, val passed: Boolean, val appVersion: String, val recordedAtMillis: Long)

// Each backend account owns its cache and outbox; switching accounts cannot leak data.
@Serializable data class CachedAccount(
    val data: VolunteerData,
    val pendingTaskStatuses: Map<String, TaskStatus> = emptyMap(),
    val pendingReadIds: Set<String> = emptySet(),
    val incidentDraft: IncidentDraft = IncidentDraft(),
    val messageDrafts: Map<String, String> = emptyMap(),
    val taskUpdates: List<TaskUpdate> = emptyList(),
    val arrivalLocationId: String? = null,
    val lastSyncedAtMillis: Long? = null,
)

@Serializable data class LocalState(
    val schemaVersion: Int = 1,
    val sessionMode: SessionMode = SessionMode.NONE,
    val activeUserId: String? = null,
    val demoData: VolunteerData = MockRepository.data,
    val accounts: Map<String, CachedAccount> = emptyMap(),
    val demoCheckIns: Map<String, CheckInResult> = emptyMap(),
    val demoIncidents: List<SavedIncident> = emptyList(),
    val demoIncidentDraft: IncidentDraft = IncidentDraft(),
    val demoMessageDrafts: Map<String, String> = emptyMap(),
    val weather: WeatherSnapshot? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val demoTaskUpdates: List<TaskUpdate> = emptyList(),
    val demoArrivalLocationId: String? = null,
    val deviceChecks: Map<String, DeviceTestObservation> = emptyMap(),
) {
    val activeAccount get() = activeUserId?.let(accounts::get)
    val activeData get() = if (sessionMode == SessionMode.REMOTE) activeAccount?.data else demoData
    val incidentDraft get() = if (sessionMode == SessionMode.REMOTE) activeAccount?.incidentDraft ?: IncidentDraft() else demoIncidentDraft
    val messageDrafts get() = when (sessionMode) {
        SessionMode.REMOTE -> activeAccount?.messageDrafts.orEmpty()
        SessionMode.DEMO -> demoMessageDrafts
        SessionMode.NONE -> emptyMap()
    }
    val pendingCount get() = if (sessionMode == SessionMode.REMOTE) activeAccount?.let { it.pendingTaskStatuses.size + it.pendingReadIds.size } ?: 0 else 0
    val taskUpdates get() = if (sessionMode == SessionMode.REMOTE) activeAccount?.taskUpdates.orEmpty() else demoTaskUpdates
    val arrivalLocationId get() = if (sessionMode == SessionMode.REMOTE) activeAccount?.arrivalLocationId else demoArrivalLocationId
}
