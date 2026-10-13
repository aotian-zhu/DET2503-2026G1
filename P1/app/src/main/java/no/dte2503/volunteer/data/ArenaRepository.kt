package no.dte2503.volunteer.data

import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RefreshStatus(val refreshing: Boolean = false, val syncError: String? = null, val weatherError: String? = null)

class ArenaRepository(
    private val store: LocalStore,
    val online: StateFlow<Boolean>,
    private val weatherSource: WeatherSource,
    private val remote: VolunteerRepository? = null,
    private val photos: PhotoStore? = null,
) {
    val local = store.state
    val hasBackend get() = remote != null
    private val refreshMutex = Mutex()
    private val operationMutex = Mutex()
    private val mutableRefresh = MutableStateFlow(RefreshStatus())
    val refreshStatus = mutableRefresh.asStateFlow()
    suspend fun initialize() = store.initialize()
    suspend fun startDemo() {
        store.update { it.copy(sessionMode = SessionMode.DEMO, activeUserId = null) }
        mutableRefresh.value = mutableRefresh.value.copy(syncError = null)
    }
    suspend fun setTheme(mode: ThemeMode) = store.update { it.copy(themeMode = mode) }

    suspend fun signIn(email: String, password: String) = operationMutex.withLock {
        require(online.value) { "Connect to the internet for your first sign-in. The demo works offline." }
        require(email.contains('@') && password.isNotBlank()) { "Enter your volunteer email and password." }
        val backend = requireNotNull(remote) { "The event account service has not been configured." }
        backend.signIn(email.trim(), password)
        val fresh = backend.loadVolunteerData()
        store.update { saved ->
            val previous = saved.accounts[fresh.profile.id]
            val account = previous?.copy(data = mergePending(fresh, previous)) ?: CachedAccount(fresh)
            saved.copy(sessionMode = SessionMode.REMOTE, activeUserId = fresh.profile.id, accounts = saved.accounts + (fresh.profile.id to account))
        }
        mutableRefresh.value = mutableRefresh.value.copy(syncError = null)
    }

    suspend fun signOut() = operationMutex.withLock {
        val remoteMode = local.value.sessionMode == SessionMode.REMOTE
        store.update { it.copy(sessionMode = SessionMode.NONE, activeUserId = null) }
        // A2 auth tokens are cleared if possible; cached outboxes remain account-isolated.
        if (remoteMode && remote != null) try { remote.signOut() } catch (error: Exception) { if (error is CancellationException) throw error }
    }

    suspend fun advanceTask(taskId: String) = changeTaskStatus(taskId) { status ->
        when (status) { TaskStatus.TODO -> TaskStatus.IN_PROGRESS; TaskStatus.IN_PROGRESS -> TaskStatus.DONE; TaskStatus.DONE -> TaskStatus.DONE }
    }

    suspend fun reopenTask(taskId: String) = changeTaskStatus(taskId) { status ->
        if (status == TaskStatus.DONE) TaskStatus.IN_PROGRESS else status
    }

    private suspend fun changeTaskStatus(taskId: String, change: (TaskStatus) -> TaskStatus) = operationMutex.withLock {
        store.update { saved ->
            check(saved.sessionMode != SessionMode.NONE) { "Open a volunteer session first." }
            val task = requireNotNull(saved.activeData?.tasks?.firstOrNull { it.id == taskId }) { "This task is no longer assigned to you." }
            val status = change(task.status)
            if (status == task.status) return@update saved
            updateActive(saved) { data ->
                data.copy(tasks = data.tasks.map { if (it.id == taskId) it.copy(status = status) else it })
            }.let { updated ->
                if (saved.sessionMode != SessionMode.REMOTE) updated else {
                    val id = requireNotNull(saved.activeUserId)
                    val account = requireNotNull(updated.accounts[id])
                    updated.copy(accounts = updated.accounts + (id to account.copy(pendingTaskStatuses = account.pendingTaskStatuses + (taskId to status))))
                }
            }
        }
    }

    suspend fun markAnnouncementRead(id: String) = store.update { saved ->
        val updated = updateActive(saved) { data -> data.copy(announcements = data.announcements.map { if (it.id == id) it.copy(isRead = true) else it }) }
        if (saved.sessionMode != SessionMode.REMOTE) updated else {
            val user = requireNotNull(saved.activeUserId)
            val account = requireNotNull(updated.accounts[user])
            updated.copy(accounts = updated.accounts + (user to account.copy(pendingReadIds = account.pendingReadIds + id)))
        }
    }

    suspend fun readConversation(taskId: String) = store.update { saved -> updateActive(saved) { data ->
        data.copy(conversations = data.conversations.map { if (it.taskId == taskId) it.copy(unreadCount = 0) else it })
    } }

    suspend fun saveDraft(draft: IncidentDraft) = store.update { saved ->
        if (saved.sessionMode == SessionMode.REMOTE) {
            val id = requireNotNull(saved.activeUserId)
            val account = requireNotNull(saved.accounts[id])
            saved.copy(accounts = saved.accounts + (id to account.copy(incidentDraft = draft)))
        } else saved.copy(demoIncidentDraft = draft)
    }

    suspend fun saveMessageDraft(taskId: String, body: String) {
        require(body.length <= 2000) { "Messages must contain no more than 2,000 characters." }
        // Capture the owner before waiting for disk IO, so a logout cannot move a draft to another account.
        val owner = local.value
        check(owner.sessionMode != SessionMode.NONE) { "Open a volunteer session first." }
        require(owner.activeData?.tasks?.any { it.id == taskId } == true) { "This task is no longer assigned to you." }
        store.update { saved ->
            updateMessageDrafts(saved, owner.sessionMode, owner.activeUserId) { drafts ->
                if (body.isEmpty()) drafts - taskId else drafts + (taskId to body)
            }
        }
    }

    suspend fun sendMessage(taskId: String, body: String) = operationMutex.withLock {
        val text = body.trim()
        require(text.length in 1..2000) { "Messages must contain 1–2000 characters." }
        val data = requireNotNull(local.value.activeData)
        require(data.tasks.any { it.id == taskId }) { "This task is no longer assigned to you." }
        val updated = if (local.value.sessionMode == SessionMode.REMOTE) {
            requireOnlineAction()
            requireNotNull(remote).sendMessage(taskId, text)
        } else {
            val message = ChatMessage(UUID.randomUUID().toString(), data.profile.displayName, text, LocalTime.now().toString().take(5), true)
            data.conversations.firstOrNull { it.taskId == taskId }?.let { it.copy(messages = it.messages + message, unreadCount = 0) }
                ?: TaskConversation("conversation_$taskId", taskId, "Shift coordinator", listOf(message), 0)
        }
        store.update { saved ->
            val withMessage = updateActive(saved) { current -> current.copy(conversations = current.conversations.filterNot { it.taskId == taskId } + updated) }
            updateMessageDrafts(withMessage) { drafts ->
                // Preserve anything the volunteer typed while the request was running.
                if (drafts[taskId] == body) drafts - taskId else drafts
            }
        }
    }

    suspend fun checkIn(payload: String): CheckInResult = operationMutex.withLock {
        val saved = local.value
        validateCheckIn(payload, requireNotNull(saved.activeData).shift.id)
        if (saved.sessionMode == SessionMode.REMOTE) {
            requireOnlineAction()
            return@withLock requireNotNull(remote).checkIn(payload)
        }
        require(payload == MockRepository.demoQr) { "Use the checkpoint code for this demo shift." }
        saved.demoCheckIns[payload]?.let { return@withLock it.copy(duplicate = true) }
        val result = CheckInResult(UUID.randomUUID().toString(), Instant.now().toString(), false)
        store.update { it.copy(demoCheckIns = it.demoCheckIns + (payload to result)) }
        result
    }

    suspend fun submitIncident(draft: IncidentDraft, bytes: ByteArray?, mime: String?): IncidentReceipt = operationMutex.withLock {
        require(draft.description.trim().length in 3..2000) { "Describe the issue using 3–2000 characters." }
        val data = requireNotNull(local.value.activeData)
        require(draft.locationId == null || data.locations.any { it.id == draft.locationId }) { "Choose a valid location." }
        require(draft.taskId == null || data.tasks.any { it.id == draft.taskId }) { "Choose an assigned task." }
        if (bytes != null) {
            require(bytes.size in 1..5_242_880) { "Choose a photo smaller than 5 MB." }
            require(mime in listOf("image/jpeg", "image/png", "image/webp")) { "Choose a JPEG, PNG or WebP photo." }
        }
        val id = UUID.randomUUID().toString()
        val receipt = if (local.value.sessionMode == SessionMode.REMOTE) {
            requireOnlineAction()
            requireNotNull(remote).submitIncident(draft.copy(description = draft.description.trim()), bytes, mime)
        } else {
            val path = bytes?.let { requireNotNull(photos).save(id, it, requireNotNull(mime)) }
            val result = IncidentReceipt(id, Instant.now().toString(), path)
            try { store.update { it.copy(demoIncidents = it.demoIncidents + SavedIncident(draft, result)) } }
            catch (error: Exception) { path?.let { photos?.delete(it) }; throw error }
            result
        }
        saveDraft(IncidentDraft())
        receipt
    }

    // Foreground sync replays idempotent task/read writes before fetching a new snapshot.
    // Serializing account changes and writes prevents an account switch during replay.
    suspend fun refresh(forceWeather: Boolean = false) {
        if (!online.value || !refreshMutex.tryLock()) return
        mutableRefresh.value = mutableRefresh.value.copy(refreshing = true)
        try {
            if (local.value.sessionMode == SessionMode.REMOTE) try {
                operationMutex.withLock {
                    val backend = requireNotNull(remote) { "Configure the event account service to sync." }
                    check(backend.restoreSession()) { "Sign in again to sync your saved changes." }
                    val userId = requireNotNull(local.value.activeUserId)
                    check(backend.signedInUserId == userId) { "The account changed. Please sign in again." }
                    val pending = requireNotNull(local.value.accounts[userId])
                    pending.pendingTaskStatuses.forEach { (id, status) ->
                        backend.updateTaskStatus(id, status)
                        store.update { saved ->
                            val current = requireNotNull(saved.accounts[userId])
                            saved.copy(accounts = saved.accounts + (userId to current.copy(pendingTaskStatuses = current.pendingTaskStatuses - id)))
                        }
                    }
                    pending.pendingReadIds.forEach { id ->
                        backend.markAnnouncementRead(id)
                        store.update { saved ->
                            val current = requireNotNull(saved.accounts[userId])
                            saved.copy(accounts = saved.accounts + (userId to current.copy(pendingReadIds = current.pendingReadIds - id)))
                        }
                    }
                    val fresh = backend.loadVolunteerData()
                    check(fresh.profile.id == userId) { "The account changed. Please sign in again." }
                    store.update { saved ->
                        val current = requireNotNull(saved.accounts[userId])
                        saved.copy(accounts = saved.accounts + (userId to current.copy(data = mergePending(fresh, current))))
                    }
                }
                mutableRefresh.value = mutableRefresh.value.copy(syncError = null)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableRefresh.value = mutableRefresh.value.copy(syncError = "Saved changes could not sync. Refresh to retry.")
            }
            val weather = local.value.weather
            if (forceWeather || weather == null || System.currentTimeMillis() - weather.fetchedAtMillis > 15 * 60_000) try {
                val fresh = weatherSource.fetch()
                store.update { it.copy(weather = fresh) }
                mutableRefresh.value = mutableRefresh.value.copy(weatherError = null)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableRefresh.value = mutableRefresh.value.copy(weatherError = "Weather could not refresh. Your last reading is kept.")
            }
        } finally { mutableRefresh.value = mutableRefresh.value.copy(refreshing = false); refreshMutex.unlock() }
    }

    private fun requireOnlineAction() {
        check(online.value) { "Connect to send this to your coordinator. Your incident draft stays saved." }
    }

    private fun updateMessageDrafts(
        saved: LocalState,
        mode: SessionMode = saved.sessionMode,
        userId: String? = saved.activeUserId,
        change: (Map<String, String>) -> Map<String, String>,
    ): LocalState = if (mode == SessionMode.REMOTE) {
        val id = requireNotNull(userId)
        val account = requireNotNull(saved.accounts[id])
        saved.copy(accounts = saved.accounts + (id to account.copy(messageDrafts = change(account.messageDrafts))))
    } else saved.copy(demoMessageDrafts = change(saved.demoMessageDrafts))

    private fun updateActive(saved: LocalState, change: (VolunteerData) -> VolunteerData): LocalState {
        check(saved.sessionMode != SessionMode.NONE) { "Open a volunteer session first." }
        return if (saved.sessionMode == SessionMode.REMOTE) {
            val id = requireNotNull(saved.activeUserId)
            val account = requireNotNull(saved.accounts[id])
            saved.copy(accounts = saved.accounts + (id to account.copy(data = change(account.data))))
        } else saved.copy(demoData = change(saved.demoData))
    }
}

internal fun mergePending(fresh: VolunteerData, account: CachedAccount) = fresh.copy(
    tasks = fresh.tasks.map { task -> account.pendingTaskStatuses[task.id]?.let { task.copy(status = it) } ?: task },
    announcements = fresh.announcements.map { if (it.id in account.pendingReadIds) it.copy(isRead = true) else it },
)
