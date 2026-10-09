package no.dte2503.volunteer

import android.app.Application
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.dte2503.volunteer.data.*

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val currentPosition: GeoPointData? = null,
    val locationPermissionDenied: Boolean = false,
    val locationError: String? = null,
    val locateRequestId: Int = 0,
    val selectedTaskId: String? = null,
    val signedInName: String = "",
    val signedInEmail: String = "",
    val selectedConversationTaskId: String? = null,
    val tasks: List<VolunteerTask> = MockRepository.tasks,
    val announcements: List<Announcement> = MockRepository.announcements,
    val conversations: List<TaskConversation> = MockRepository.conversations,
    val isSendingMessage: Boolean = false,
    val messageSentTaskId: String? = null,
    val messageSendVersion: Int = 0,
    val isSubmittingWorkflow: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VolunteerRepository = RepositoryProvider.repository
    private val offlineData = OfflineDataStore(application)
    private val operations = OfflineOperationStore(application)
    private val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
    private var isSyncing = false
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { viewModelScope.launch { syncPendingOperations() } }
    }
    private val _uiState = MutableStateFlow(AppUiState(isLoading = repository.isRemote))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    var profile = MockRepository.profile; private set
    var shift = MockRepository.shift; private set
    val scenarios = MockRepository.scenarios
    val permissions = MockRepository.permissions
    val functions = MockRepository.functions
    var locations = MockRepository.locations; private set
    val isDemoMode get() = !repository.isRemote

    init {
        offlineData.lastUserId()?.let { userId ->
            offlineData.load(userId)?.let { applyData(it, it.profile.displayName, it.profile.email, false) }
        }
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
        if (repository.isRemote) restoreSession()
    }

    private fun restoreSession() = viewModelScope.launch {
        runCatching { repository.restoreSession() }.onSuccess { restored ->
            if (restored) {
                syncPendingOperations()
                runCatching { loadVolunteerData() }.onFailure { showOfflineState(it) }
            } else if (!_uiState.value.isLoggedIn) _uiState.update { it.copy(isLoading = false) }
        }.onFailure(::showOfflineState)
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        val cleanEmail = email.trim()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            repository.signIn(cleanEmail, password)
            val data = repository.loadVolunteerData()
            applyData(data, data.profile.displayName.ifBlank { cleanEmail.substringBefore('@') }, data.profile.email.ifBlank { cleanEmail })
            syncPendingOperations()
        }.onFailure(::showError)
    }

    private suspend fun loadVolunteerData() {
        val data = repository.loadVolunteerData()
        applyData(data, data.profile.displayName, data.profile.email)
    }

    private fun applyData(data: VolunteerData, name: String, email: String, persist: Boolean = true) {
        profile = data.profile
        shift = data.shift
        locations = data.locations
        _uiState.update { it.copy(isLoggedIn = true, isLoading = false, errorMessage = null, signedInName = name, signedInEmail = email, tasks = data.tasks, announcements = data.announcements, conversations = data.conversations) }
        if (persist) offlineData.save(data.profile.id, currentData())
    }

    private fun currentData() = VolunteerData(profile, shift, locations, _uiState.value.tasks, _uiState.value.announcements, _uiState.value.conversations)
    private fun saveSnapshot() { if (_uiState.value.isLoggedIn) offlineData.save(profile.id, currentData()) }

    fun logout() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { repository.signOut() }
        offlineData.clear(profile.id)
        profile = MockRepository.profile
        shift = MockRepository.shift
        locations = MockRepository.locations
        _uiState.value = AppUiState()
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }
    fun updateEmail(email: String): Boolean { val value = email.trim(); if (value.isEmpty()) return false; _uiState.update { it.copy(signedInEmail = value) }; return true }
    fun setCurrentPosition(latitude: Double, longitude: Double) = _uiState.update { it.copy(currentPosition = GeoPointData(latitude, longitude), locationPermissionDenied = false, locationError = null, locateRequestId = it.locateRequestId + 1) }
    fun setLocationPermissionDenied(denied: Boolean) = _uiState.update { it.copy(locationPermissionDenied = denied) }
    fun setLocationError(message: String) = _uiState.update { it.copy(locationError = message) }
    fun selectTask(taskId: String?) { if (taskId == null || _uiState.value.tasks.any { it.id == taskId }) _uiState.update { it.copy(selectedTaskId = taskId) } }

    fun markAnnouncementRead(id: String) {
        if (_uiState.value.announcements.none { it.id == id && !it.isRead }) return
        _uiState.update { it.copy(announcements = it.announcements.map { announcement -> if (announcement.id == id) announcement.copy(isRead = true) else announcement }) }
        saveSnapshot()
        operations.add(PendingOperation(UUID.randomUUID().toString(), profile.id, PendingOperationType.MARK_ANNOUNCEMENT_READ, announcementId = id))
        viewModelScope.launch { syncPendingOperations() }
    }

    fun openConversation(taskId: String?) = _uiState.update { state -> state.copy(selectedConversationTaskId = taskId, conversations = state.conversations.map { if (it.taskId == taskId) it.copy(unreadCount = 0) else it }) }.also { saveSnapshot() }

    fun sendMessage(taskId: String, body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        val operation = PendingOperation(UUID.randomUUID().toString(), profile.id, PendingOperationType.SEND_MESSAGE, taskId = taskId, body = text)
        val localMessage = ChatMessage("offline_${operation.id}", _uiState.value.signedInName.ifBlank { "Volunteer" }, text, "Pending", true)
        _uiState.update { state ->
            val existing = state.conversations.firstOrNull { it.taskId == taskId }
            val updated = existing?.copy(messages = existing.messages + localMessage, unreadCount = 0)
                ?: TaskConversation("offline_$taskId", taskId, "Shift coordinator", listOf(localMessage), 0)
            state.copy(conversations = state.conversations.filterNot { it.taskId == taskId } + updated, messageSentTaskId = taskId, messageSendVersion = state.messageSendVersion + 1, actionMessage = "Message saved. It will sync automatically when online.")
        }
        saveSnapshot()
        operations.add(operation)
        viewModelScope.launch { syncPendingOperations() }
    }

    fun retryPendingMessage(taskId: String) { viewModelScope.launch { syncPendingOperations() } }

    fun checkIn(payload: String) {
        if (_uiState.value.isSubmittingWorkflow) return
        operations.add(PendingOperation(UUID.randomUUID().toString(), profile.id, PendingOperationType.CHECK_IN, qrPayload = payload.trim()))
        _uiState.update { it.copy(actionMessage = "Check-in saved and waiting for server verification.") }
        viewModelScope.launch { syncPendingOperations() }
    }

    fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?) {
        if (_uiState.value.isSubmittingWorkflow) return
        val operationId = UUID.randomUUID().toString()
        val photoPath = photoBytes?.let { bytes ->
            File(getApplication<Application>().filesDir, "pending_incidents").apply { mkdirs() }
                .resolve(operationId).also { it.writeBytes(bytes) }.absolutePath
        }
        operations.add(PendingOperation(operationId, profile.id, PendingOperationType.SUBMIT_INCIDENT, incident = draft, photoPath = photoPath, photoMimeType = photoMimeType))
        _uiState.update { it.copy(actionMessage = "Incident saved. It will upload automatically when online.") }
        viewModelScope.launch { syncPendingOperations() }
    }

    fun advanceTask(taskId: String) {
        val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val nextStatus = when (task.status) { TaskStatus.TODO -> TaskStatus.IN_PROGRESS; TaskStatus.IN_PROGRESS -> TaskStatus.DONE; TaskStatus.DONE -> return }
        _uiState.update { state -> state.copy(errorMessage = null, tasks = state.tasks.map { if (it.id == taskId) it.copy(status = nextStatus) else it }, actionMessage = "Task updated. Changes will sync automatically.") }
        saveSnapshot()
        operations.add(PendingOperation(UUID.randomUUID().toString(), profile.id, PendingOperationType.UPDATE_TASK, taskId = taskId, taskStatus = nextStatus))
        viewModelScope.launch { syncPendingOperations() }
    }

    private suspend fun syncPendingOperations() {
        if (!repository.isRemote || isSyncing || !_uiState.value.isLoggedIn) return
        isSyncing = true
        var syncedAny = false
        try {
            for (operation in operations.all().filter { it.userId == profile.id }) {
                val result = runCatching {
                    when (operation.type) {
                        PendingOperationType.UPDATE_TASK -> repository.updateTaskStatus(requireNotNull(operation.taskId), requireNotNull(operation.taskStatus))
                        PendingOperationType.MARK_ANNOUNCEMENT_READ -> repository.markAnnouncementRead(requireNotNull(operation.announcementId))
                        PendingOperationType.SEND_MESSAGE -> {
                            val updated = repository.sendMessage(requireNotNull(operation.taskId), requireNotNull(operation.body))
                            _uiState.update { state -> state.copy(conversations = state.conversations.filterNot { it.taskId == operation.taskId } + updated) }
                        }
                        PendingOperationType.CHECK_IN -> {
                            val checkIn = repository.checkIn(requireNotNull(operation.qrPayload))
                            _uiState.update { it.copy(actionMessage = if (checkIn.duplicate) "Already checked in at this checkpoint." else "Check-in verified and recorded.") }
                        }
                        PendingOperationType.SUBMIT_INCIDENT -> {
                            val photoFile = operation.photoPath?.let(::File)
                            val receipt = repository.submitIncident(requireNotNull(operation.incident), photoFile?.takeIf(File::exists)?.readBytes(), operation.photoMimeType)
                            photoFile?.delete()
                            _uiState.update { it.copy(actionMessage = "Incident ${receipt.id.take(8)} submitted.") }
                        }
                    }
                }
                if (result.isFailure) break
                operations.remove(operation.id)
                syncedAny = true
            }
            if (syncedAny) {
                saveSnapshot()
                runCatching { loadVolunteerData() }
            }
        } finally {
            isSyncing = false
        }
    }

    fun assignedFunctions() = functions.filter { it.id in profile.assignedFunctionIds }
    fun effectivePermissions() = assignedFunctions().flatMap { function -> permissions.filter { it.id in function.permissionIds } }.distinctBy { it.id }
    fun tasksWithDistance(position: GeoPointData?, tasks: List<VolunteerTask> = _uiState.value.tasks) = tasks.map { task ->
        val distance = position?.let { val result = FloatArray(1); Location.distanceBetween(it.latitude, it.longitude, task.place.position.latitude, task.place.position.longitude, result); result[0] }
        TaskWithDistance(task, distance)
    }.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }

    private fun showOfflineState(error: Throwable) {
        if (_uiState.value.isLoggedIn) _uiState.update { it.copy(isLoading = false, actionMessage = "Offline mode: showing saved data. Changes will sync automatically.") }
        else showError(error)
    }
    private fun showError(error: Throwable) = _uiState.update { it.copy(isLoading = false, errorMessage = error.userMessage()) }

    override fun onCleared() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
    }
}

private fun Throwable.userMessage() = message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
