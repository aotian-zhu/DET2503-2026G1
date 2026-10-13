package no.dte2503.volunteer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.dte2503.volunteer.data.*

private data class TransientState(
    val initialized: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val position: GeoPointData? = null,
    val accuracyMetres: Float? = null,
    val locationMessage: String? = null,
    val selectedTaskId: String? = null,
    val selectedLocationId: String? = null,
    val conversationTaskId: String? = null,
    val sendingMessage: Boolean = false,
    val submitting: Boolean = false,
    val busyTasks: Set<String> = emptySet(),
)

data class AppUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isDemoMode: Boolean = true,
    val hasBackend: Boolean = false,
    val isOnline: Boolean = false,
    val refreshing: Boolean = false,
    val pendingCount: Int = 0,
    val syncError: String? = null,
    val weatherError: String? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val data: VolunteerData = MockRepository.data,
    val currentPosition: GeoPointData? = null,
    val accuracyMetres: Float? = null,
    val locationMessage: String? = null,
    val selectedTaskId: String? = null,
    val selectedLocationId: String? = null,
    val selectedConversationTaskId: String? = null,
    val isSendingMessage: Boolean = false,
    val isSubmittingWorkflow: Boolean = false,
    val busyTasks: Set<String> = emptySet(),
    val weather: WeatherSnapshot? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val incidentDraft: IncidentDraft = IncidentDraft(),
    val messageDrafts: Map<String, String> = emptyMap(),
    val incidentCount: Int = 0,
    val checkInCount: Int = 0,
) {
    val tasks get() = data.tasks
    val profile get() = data.profile
    val shift get() = data.shift
    val locations get() = data.locations
    val announcements get() = data.announcements
    val conversations get() = data.conversations
    val signedInName get() = profile.displayName
    val signedInEmail get() = profile.email
}

// Compose only observes this StateFlow. Repositories own network, persistence and sync.
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dependencies = RepositoryProvider.create(application)
    private val repository = dependencies.repository
    private val transient = MutableStateFlow(TransientState())
    val uiState = combine(repository.local, repository.online, repository.refreshStatus, transient) { saved, online, refresh, temp ->
        AppUiState(
            isInitialized = temp.initialized, isLoading = temp.loading,
            isLoggedIn = saved.sessionMode != SessionMode.NONE && saved.activeData != null,
            isDemoMode = saved.sessionMode != SessionMode.REMOTE, hasBackend = repository.hasBackend,
            isOnline = online, refreshing = refresh.refreshing, pendingCount = saved.pendingCount,
            syncError = refresh.syncError, weatherError = refresh.weatherError,
            errorMessage = temp.error, actionMessage = temp.notice, data = saved.activeData ?: MockRepository.data,
            currentPosition = temp.position, accuracyMetres = temp.accuracyMetres, locationMessage = temp.locationMessage,
            selectedTaskId = temp.selectedTaskId, selectedLocationId = temp.selectedLocationId,
            selectedConversationTaskId = temp.conversationTaskId, isSendingMessage = temp.sendingMessage,
            isSubmittingWorkflow = temp.submitting, busyTasks = temp.busyTasks,
            weather = saved.weather, themeMode = saved.themeMode, incidentDraft = saved.incidentDraft,
            messageDrafts = saved.messageDrafts,
            incidentCount = saved.demoIncidents.size, checkInCount = saved.demoCheckIns.size,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        viewModelScope.launch {
            try { repository.initialize() } catch (error: Exception) { showError(error) }
            transient.update { it.copy(initialized = true) }
            combine(repository.online, repository.local.map { it.sessionMode }.distinctUntilChanged()) { online, mode -> online && mode != SessionMode.NONE }
                .distinctUntilChanged().collect { ready -> if (ready) repository.refresh() }
        }
    }

    fun enterDemo() = runAction {
        repository.startDemo()
        transient.update { it.copy(error = null) }
    }

    fun login(email: String, password: String) {
        if (transient.value.loading) return
        transient.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try { repository.signIn(email, password) } catch (error: Exception) { showError(error) }
            finally { transient.update { it.copy(loading = false) } }
        }
    }

    fun logout() = runAction {
        repository.signOut()
        transient.value = TransientState(initialized = true)
    }
    fun refresh() = runAction { repository.refresh(forceWeather = true) }
    fun onResume() { if (uiState.value.isLoggedIn) runAction { repository.refresh() } }
    fun setTheme(mode: ThemeMode) = runAction { repository.setTheme(mode) }
    fun clearError() = transient.update { it.copy(error = null) }
    fun clearActionMessage() = transient.update { it.copy(notice = null) }
    fun notifyUser(message: String) = transient.update { it.copy(notice = message) }
    fun setCurrentPosition(latitude: Double, longitude: Double, accuracy: Float = 0f) = transient.update {
        it.copy(position = GeoPointData(latitude, longitude), accuracyMetres = accuracy, locationMessage = null)
    }
    fun setLocationMessage(message: String?, clearPosition: Boolean = false) = transient.update {
        it.copy(locationMessage = message, position = if (clearPosition) null else it.position)
    }
    fun selectTask(id: String?) = transient.update { it.copy(selectedTaskId = id, selectedLocationId = null) }
    fun selectLocation(id: String?) = transient.update { it.copy(selectedLocationId = id, selectedTaskId = null) }
    fun assignedFunctions() = MockRepository.functions.filter { it.id in uiState.value.profile.assignedFunctionIds }
    fun effectivePermissions() = assignedFunctions().flatMap { function -> MockRepository.permissions.filter { it.id in function.permissionIds } }.distinctBy { it.id }
    fun tasksWithDistance(position: GeoPointData?, tasks: List<VolunteerTask> = uiState.value.tasks) = tasksByDistance(tasks, position)

    fun advanceTask(id: String) = changeTask(id) { repository.advanceTask(id) }
    fun reopenTask(id: String) = changeTask(id) {
        repository.reopenTask(id)
        transient.update { it.copy(notice = "Task reopened. Find it under Open.") }
    }

    private fun changeTask(id: String, action: suspend () -> Unit) {
        if (id in transient.value.busyTasks) return
        transient.update { it.copy(busyTasks = it.busyTasks + id) }
        viewModelScope.launch {
            try { action() } catch (error: Exception) { showError(error) }
            finally { transient.update { it.copy(busyTasks = it.busyTasks - id) } }
            repository.refresh()
        }
    }
    fun markAnnouncementRead(id: String) = runAction { repository.markAnnouncementRead(id); repository.refresh() }
    fun openConversation(taskId: String?) {
        transient.update { it.copy(conversationTaskId = taskId) }
        if (taskId != null) runAction { repository.readConversation(taskId) }
    }

    fun saveMessageDraft(taskId: String, text: String) = runAction { repository.saveMessageDraft(taskId, text) }
    fun sendMessage(taskId: String, text: String, onSaved: () -> Unit = {}) {
        if (transient.value.sendingMessage) return
        transient.update { it.copy(sendingMessage = true, error = null) }
        viewModelScope.launch {
            try { repository.sendMessage(taskId, text); onSaved() }
            catch (error: Exception) { showError(error) }
            finally { transient.update { it.copy(sendingMessage = false) } }
        }
    }
    fun saveIncidentDraft(draft: IncidentDraft) = runAction { repository.saveDraft(draft) }
    fun checkIn(payload: String) = submitWorkflow {
        val result = repository.checkIn(payload.trim())
        transient.update { it.copy(notice = if (result.duplicate) "Already checked in at this checkpoint." else if (uiState.value.isDemoMode) "Demo check-in saved on this device." else "Check-in confirmed.") }
    }

    fun submitIncident(draft: IncidentDraft, photo: Uri?, onSaved: () -> Unit) = submitWorkflow {
        val resolver = getApplication<Application>().contentResolver
        val mime = photo?.let(resolver::getType)
        val bytes = photo?.let { uri -> withContext(Dispatchers.IO) {
            requireNotNull(resolver.openInputStream(uri)) { "This photo could not be opened. Please choose it again." }.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var count = input.read(buffer)
                while (count != -1) {
                    require(output.size() + count <= 5_242_880) { "Choose a photo smaller than 5 MB." }
                    output.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                output.toByteArray()
            }
        } }
        repository.submitIncident(draft, bytes, mime)
        onSaved()
        transient.update { it.copy(notice = if (uiState.value.isDemoMode) "Demo report saved on this device." else "Report sent to your coordinator.") }
    }

    private fun submitWorkflow(action: suspend () -> Unit) {
        if (transient.value.submitting) return
        transient.update { it.copy(submitting = true, error = null, notice = null) }
        viewModelScope.launch {
            try { action() } catch (error: Exception) { showError(error) }
            finally { transient.update { it.copy(submitting = false) } }
        }
    }
    private fun runAction(action: suspend () -> Unit) = viewModelScope.launch {
        try { action() } catch (error: Exception) { showError(error) }
    }
    private fun showError(error: Exception) {
        if (error is CancellationException) throw error
        transient.update { it.copy(error = if (error is IllegalArgumentException || error is IllegalStateException) error.message ?: "Please try again." else "This action could not be completed. Your saved data is kept. Please try again.") }
    }
    override fun onCleared() { dependencies.network.close(); super.onCleared() }
}
