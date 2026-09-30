package no.dte2503.volunteer

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.dte2503.volunteer.data.*

// 页面共享的单一状态快照，避免各 Screen 分别维护互相冲突的业务状态。
data class AppUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val currentPosition: GeoPointData? = null,
    val locationPermissionDenied: Boolean = false,
    val selectedTaskId: String? = null,
    val signedInName: String = "",
    val signedInEmail: String = "",
    val selectedConversationTaskId: String? = null,
    val tasks: List<VolunteerTask> = MockRepository.tasks,
    val announcements: List<Announcement> = MockRepository.announcements,
    val conversations: List<TaskConversation> = MockRepository.conversations,
    val isSendingMessage: Boolean = false,
    val isSubmittingWorkflow: Boolean = false,
)

// ViewModel 只依赖仓库接口，因此 UI 无需知道当前使用演示数据还是 Supabase。
class MainViewModel(private val repository: VolunteerRepository = RepositoryProvider.repository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(isLoading = repository.isRemote))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    var profile = MockRepository.profile; private set
    var shift = MockRepository.shift; private set
    val scenarios = MockRepository.scenarios
    val permissions = MockRepository.permissions
    val functions = MockRepository.functions
    var locations = MockRepository.locations; private set
    val isDemoMode get() = !repository.isRemote

    init { if (repository.isRemote) restoreSession() }

    private fun restoreSession() = viewModelScope.launch {
        runCatching { repository.restoreSession() }.onSuccess { if (it) loadVolunteerData() else _uiState.update { state -> state.copy(isLoading = false) } }
            .onFailure(::showError)
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        val cleanEmail = email.trim()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            repository.signIn(cleanEmail, password)
            if (repository.isRemote) loadVolunteerData() else {
                val data = repository.loadVolunteerData()
                applyData(data, cleanEmail.substringBefore('@').ifBlank { "Volunteer" }, cleanEmail)
            }
        }.onFailure(::showError)
    }

    private suspend fun loadVolunteerData() {
        val data = repository.loadVolunteerData()
        applyData(data, data.profile.displayName, data.profile.email)
    }

    private fun applyData(data: VolunteerData, name: String, email: String) {
        profile = data.profile; shift = data.shift; locations = data.locations
        _uiState.update { it.copy(isLoggedIn = true, isLoading = false, errorMessage = null, signedInName = name, signedInEmail = email, tasks = data.tasks, announcements = data.announcements, conversations = data.conversations) }
    }

    fun logout() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { repository.signOut() }.onSuccess {
            profile = MockRepository.profile; shift = MockRepository.shift; locations = MockRepository.locations
            _uiState.value = AppUiState()
        }.onFailure(::showError)
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }
    fun updateEmail(email: String): Boolean { val value = email.trim(); if (value.isEmpty()) return false; _uiState.update { it.copy(signedInEmail = value) }; return true }
    fun setCurrentPosition(latitude: Double, longitude: Double) = _uiState.update { it.copy(currentPosition = GeoPointData(latitude, longitude), locationPermissionDenied = false) }
    fun setLocationPermissionDenied(denied: Boolean) = _uiState.update { it.copy(locationPermissionDenied = denied) }
    fun selectTask(taskId: String?) { if (taskId == null || _uiState.value.tasks.any { it.id == taskId }) _uiState.update { it.copy(selectedTaskId = taskId) } }

    fun markAnnouncementRead(id: String) {
        // 先即时更新界面；远程写入失败时用快照回滚，兼顾响应速度与一致性。
        val previous = _uiState.value.announcements
        _uiState.update { it.copy(announcements = it.announcements.map { announcement -> if (announcement.id == id) announcement.copy(isRead = true) else announcement }) }
        viewModelScope.launch { runCatching { repository.markAnnouncementRead(id) }.onFailure { error -> _uiState.update { it.copy(announcements = previous, errorMessage = error.userMessage()) } } }
    }

    fun openConversation(taskId: String?) = _uiState.update { state -> state.copy(selectedConversationTaskId = taskId, conversations = state.conversations.map { if (it.taskId == taskId) it.copy(unreadCount = 0) else it }) }

    fun sendMessage(taskId: String, body: String) {
        val text = body.trim(); if (text.isEmpty() || _uiState.value.isSendingMessage) return
        _uiState.update { it.copy(isSendingMessage = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.sendMessage(taskId, text) }.onSuccess { updated ->
                _uiState.update { state -> state.copy(isSendingMessage = false, conversations = state.conversations.filterNot { it.taskId == taskId } + updated) }
            }.onFailure { error -> _uiState.update { it.copy(isSendingMessage = false, errorMessage = error.userMessage()) } }
        }
    }

    fun checkIn(payload: String) {
        if (_uiState.value.isSubmittingWorkflow) return
        _uiState.update { it.copy(isSubmittingWorkflow = true, errorMessage = null, actionMessage = null) }
        viewModelScope.launch { runCatching { repository.checkIn(payload.trim()) }.onSuccess { result ->
            _uiState.update { it.copy(isSubmittingWorkflow = false, actionMessage = if (result.duplicate) "Already checked in at this checkpoint." else "Check-in recorded successfully.") }
        }.onFailure { error -> _uiState.update { it.copy(isSubmittingWorkflow = false, errorMessage = error.userMessage()) } } }
    }

    fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?) {
        if (_uiState.value.isSubmittingWorkflow) return
        _uiState.update { it.copy(isSubmittingWorkflow = true, errorMessage = null, actionMessage = null) }
        viewModelScope.launch { runCatching { repository.submitIncident(draft, photoBytes, photoMimeType) }.onSuccess { receipt ->
            _uiState.update { it.copy(isSubmittingWorkflow = false, actionMessage = "Incident ${receipt.id.take(8)} submitted.") }
        }.onFailure { error -> _uiState.update { it.copy(isSubmittingWorkflow = false, errorMessage = error.userMessage()) } } }
    }

    fun advanceTask(taskId: String) {
        // 客户端只允许 TODO→IN_PROGRESS→DONE；服务端 RPC 再校验归属，失败则回滚。
        val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        val nextStatus = when (task.status) { TaskStatus.TODO -> TaskStatus.IN_PROGRESS; TaskStatus.IN_PROGRESS -> TaskStatus.DONE; TaskStatus.DONE -> return }
        _uiState.update { state -> state.copy(errorMessage = null, tasks = state.tasks.map { if (it.id == taskId) it.copy(status = nextStatus) else it }) }
        viewModelScope.launch { runCatching { repository.updateTaskStatus(taskId, nextStatus) }.onFailure { error -> _uiState.update { state -> state.copy(errorMessage = error.userMessage(), tasks = state.tasks.map { if (it.id == taskId) it.copy(status = task.status) else it }) } } }
    }

    // 权限和距离都是由原始数据派生的展示模型，不额外保存可失真的副本。
    fun assignedFunctions() = functions.filter { it.id in profile.assignedFunctionIds }
    fun effectivePermissions() = assignedFunctions().flatMap { function -> permissions.filter { it.id in function.permissionIds } }.distinctBy { it.id }
    fun tasksWithDistance(position: GeoPointData?, tasks: List<VolunteerTask> = _uiState.value.tasks) = tasks.map { task ->
        val distance = position?.let { val result = FloatArray(1); Location.distanceBetween(it.latitude, it.longitude, task.place.position.latitude, task.place.position.longitude, result); result[0] }
        TaskWithDistance(task, distance)
    }.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }

    private fun showError(error: Throwable) = _uiState.update { it.copy(isLoading = false, errorMessage = error.userMessage()) }
}

private fun Throwable.userMessage() = message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
