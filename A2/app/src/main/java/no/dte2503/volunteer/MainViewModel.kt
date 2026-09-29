package no.dte2503.volunteer

import android.location.Location
import androidx.lifecycle.ViewModel
import no.dte2503.volunteer.data.GeoPointData
import no.dte2503.volunteer.data.TaskWithDistance
import no.dte2503.volunteer.data.MockRepository
import no.dte2503.volunteer.data.Announcement
import no.dte2503.volunteer.data.ChatMessage
import no.dte2503.volunteer.data.TaskConversation
import no.dte2503.volunteer.data.TaskStatus
import no.dte2503.volunteer.data.VolunteerTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val currentPosition: GeoPointData? = null,
    val locationPermissionDenied: Boolean = false,
    val selectedTaskId: String? = null,
    val signedInName: String = "",
    val signedInEmail: String = "",
    val selectedConversationTaskId: String? = null,
    val tasks: List<VolunteerTask> = MockRepository.tasks,
    val announcements: List<Announcement> = MockRepository.announcements,
    val conversations: List<TaskConversation> = MockRepository.conversations,
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val profile = MockRepository.profile
    val shift = MockRepository.shift
    val scenarios = MockRepository.scenarios
    val permissions = MockRepository.permissions
    val functions = MockRepository.functions
    val locations = MockRepository.locations

    fun login(username: String) = _uiState.update {
        val value = username.trim()
        it.copy(
            isLoggedIn = true,
            signedInName = value.substringBefore('@').ifBlank { "Volunteer" },
            signedInEmail = value,
        )
    }

    fun logout() = _uiState.update { AppUiState() }

    fun updateEmail(email: String): Boolean {
        val value = email.trim()
        if (value.isEmpty()) return false
        _uiState.update { it.copy(signedInEmail = value) }
        return true
    }

    fun setCurrentPosition(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(currentPosition = GeoPointData(latitude, longitude), locationPermissionDenied = false)
        }
    }

    fun setLocationPermissionDenied(denied: Boolean) {
        _uiState.update { it.copy(locationPermissionDenied = denied) }
    }

    fun selectTask(taskId: String?) {
        if (taskId == null || _uiState.value.tasks.any { it.id == taskId }) {
            _uiState.update { it.copy(selectedTaskId = taskId) }
        }
    }

    fun markAnnouncementRead(id: String) {
        _uiState.update { state ->
            state.copy(announcements = state.announcements.map { announcement ->
                if (announcement.id == id) announcement.copy(isRead = true) else announcement
            })
        }
    }

    fun openConversation(taskId: String?) {
        _uiState.update { state ->
            state.copy(
                selectedConversationTaskId = taskId,
                conversations = if (taskId == null) state.conversations else state.conversations.map { conversation ->
                    if (conversation.taskId == taskId) conversation.copy(unreadCount = 0) else conversation
                },
            )
        }
    }

    fun sendMessage(taskId: String, body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        _uiState.update { state ->
            val existing = state.conversations.firstOrNull { it.taskId == taskId }
            val message = ChatMessage(
                id = "local_${UUID.randomUUID()}",
                sender = state.signedInName,
                body = text,
                time = "Now",
                isFromVolunteer = true,
            )
            val updated = if (existing == null) {
                state.conversations + TaskConversation(
                    id = "conversation_$taskId",
                    taskId = taskId,
                    coordinatorName = "Shift coordinator",
                    messages = listOf(message),
                    unreadCount = 0,
                )
            } else {
                state.conversations.map { conversation ->
                    if (conversation.taskId == taskId) conversation.copy(messages = conversation.messages + message, unreadCount = 0)
                    else conversation
                }
            }
            state.copy(conversations = updated)
        }
    }

    fun advanceTask(taskId: String) {
        _uiState.update { state ->
            state.copy(tasks = state.tasks.map { task ->
                if (task.id != taskId) task else task.copy(status = when (task.status) {
                    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                    TaskStatus.IN_PROGRESS -> TaskStatus.DONE
                    TaskStatus.DONE -> TaskStatus.DONE
                })
            })
        }
    }

    fun assignedFunctions() = functions.filter { it.id in profile.assignedFunctionIds }

    fun effectivePermissions() = assignedFunctions()
        .flatMap { function -> permissions.filter { it.id in function.permissionIds } }
        .distinctBy { it.id }

    fun tasksWithDistance(position: GeoPointData?, tasks: List<VolunteerTask> = _uiState.value.tasks): List<TaskWithDistance> =
        tasks.map { task ->
            val distance = position?.let {
                val result = FloatArray(1)
                Location.distanceBetween(
                    it.latitude,
                    it.longitude,
                    task.place.position.latitude,
                    task.place.position.longitude,
                    result,
                )
                result[0]
            }
            TaskWithDistance(task, distance)
        }.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }
}
