package no.dte2503.volunteer.data

import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ArenaRepositoryTest {
    private val weather = WeatherSource { WeatherSnapshot(2.0, -1.0, 12.0, 3, "2026-10-12T10:00", System.currentTimeMillis()) }
    private fun repository(store: MemoryStore, online: MutableStateFlow<Boolean>, backend: FakeBackend? = null, source: WeatherSource = weather) = ArenaRepository(store, online, source, backend)

    @Test fun demoChangesSurviveSerializationAndReload() = runTest {
        val first = MemoryStore()
        val repo = repository(first, MutableStateFlow(false))
        repo.startDemo()
        repo.advanceTask("task_03")
        repo.markAnnouncementRead("announcement_01")
        repo.sendMessage("task_03", "The entrance is clear.")
        repo.saveDraft(IncidentDraft(description = "A route sign is missing."))
        val encoded = Json.encodeToString(first.state.value)
        val reopened = MemoryStore(Json.decodeFromString(encoded))
        assertEquals(SessionMode.DEMO, reopened.state.value.sessionMode)
        assertEquals(TaskStatus.IN_PROGRESS, reopened.state.value.demoData.tasks.first { it.id == "task_03" }.status)
        assertTrue(reopened.state.value.demoData.announcements.first { it.id == "announcement_01" }.isRead)
        assertEquals("The entrance is clear.", reopened.state.value.demoData.conversations.first { it.taskId == "task_03" }.messages.last().body)
        assertEquals("A route sign is missing.", reopened.state.value.incidentDraft.description)
    }

    @Test fun reopenedDemoTaskSurvivesRestartAndPreservesOtherTasks() = runTest {
        val store = MemoryStore(LocalState(sessionMode = SessionMode.DEMO))
        val repo = repository(store, MutableStateFlow(false))
        val untouched = store.state.value.activeData!!.tasks.first { it.id == "task_02" }
        repo.advanceTask("task_03")
        repo.advanceTask("task_03")
        assertEquals(TaskStatus.DONE, store.state.value.activeData!!.tasks.first { it.id == "task_03" }.status)
        repo.reopenTask("task_03")
        val reopened = Json.decodeFromString<LocalState>(Json.encodeToString(store.state.value))
        assertEquals(TaskStatus.IN_PROGRESS, reopened.activeData!!.tasks.first { it.id == "task_03" }.status)
        assertEquals(untouched, reopened.activeData!!.tasks.first { it.id == "task_02" })
    }

    @Test fun reopenedBackendTaskCoalescesOfflineAndRetriesAfterFailure() = runTest {
        val store = MemoryStore()
        val online = MutableStateFlow(true)
        val backend = FakeBackend()
        val repo = repository(store, online, backend)
        repo.signIn("volunteer@example.org", "password")
        online.value = false
        repo.advanceTask("task_03")
        repo.advanceTask("task_03")
        repo.reopenTask("task_03")
        val restored = MemoryStore(Json.decodeFromString<LocalState>(Json.encodeToString(store.state.value)))
        val resumed = repository(restored, online, backend)
        assertEquals(mapOf("task_03" to TaskStatus.IN_PROGRESS), restored.state.value.activeAccount!!.pendingTaskStatuses)
        online.value = true
        backend.failWrites = true
        resumed.refresh()
        assertEquals(1, restored.state.value.pendingCount)
        assertEquals(TaskStatus.IN_PROGRESS, restored.state.value.activeData!!.tasks.first { it.id == "task_03" }.status)
        backend.failWrites = false
        resumed.refresh()
        assertEquals(0, restored.state.value.pendingCount)
        assertEquals(TaskStatus.IN_PROGRESS, backend.data.tasks.first { it.id == "task_03" }.status)
    }

    @Test fun reopeningUnknownTaskCannotChangeAssignments() = runTest {
        val store = MemoryStore(LocalState(sessionMode = SessionMode.DEMO))
        val repo = repository(store, MutableStateFlow(false))
        val before = store.state.value
        assertTrue(runCatching { repo.reopenTask("not-assigned") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(before, store.state.value)
    }

    @Test fun messageDraftSurvivesReloadAndOnlySentTaskIsCleared() = runTest {
        val first = MemoryStore(LocalState(sessionMode = SessionMode.DEMO))
        val repo = repository(first, MutableStateFlow(false))
        repo.saveMessageDraft("task_02", "Please confirm the arrival time.")
        repo.saveMessageDraft("task_03", "I still need a radio.")
        val reopened = MemoryStore(Json.decodeFromString<LocalState>(Json.encodeToString(first.state.value)))
        val next = repository(reopened, MutableStateFlow(false))
        assertEquals("Please confirm the arrival time.", reopened.state.value.messageDrafts["task_02"])
        next.sendMessage("task_02", "Please confirm the arrival time.")
        assertNull(reopened.state.value.messageDrafts["task_02"])
        assertEquals("I still need a radio.", reopened.state.value.messageDrafts["task_03"])
        assertEquals("Please confirm the arrival time.", reopened.state.value.activeData!!.conversations.first { it.taskId == "task_02" }.messages.last().body)
    }

    @Test fun failedRemoteSendKeepsMessageDraft() = runTest {
        val store = MemoryStore()
        val backend = FakeBackend()
        val repo = repository(store, MutableStateFlow(true), backend)
        repo.signIn("volunteer@example.org", "password")
        repo.saveMessageDraft("task_02", "Please confirm my meeting point.")
        backend.failWrites = true
        assertTrue(runCatching { repo.sendMessage("task_02", "Please confirm my meeting point.") }.exceptionOrNull() is IOException)
        assertEquals("Please confirm my meeting point.", store.state.value.messageDrafts["task_02"])
        assertEquals(1, backend.sendCalls)
    }

    @Test fun typingDuringRemoteSendKeepsTheNewDraft() = runTest {
        val store = MemoryStore()
        val backend = FakeBackend()
        val repo = repository(store, MutableStateFlow(true), backend)
        repo.signIn("volunteer@example.org", "password")
        repo.saveMessageDraft("task_02", "First message.")
        backend.beforeMessageResult = { repo.saveMessageDraft("task_02", "Another question.") }
        repo.sendMessage("task_02", "First message.")
        assertEquals("Another question.", store.state.value.messageDrafts["task_02"])
        assertEquals("First message.", store.state.value.activeData!!.conversations.first { it.taskId == "task_02" }.messages.last().body)
    }

    @Test fun offlineUpdatesCoalesceAndRetryWithoutLosingLocalProgress() = runTest {
        val store = MemoryStore()
        val online = MutableStateFlow(true)
        val backend = FakeBackend()
        val repo = repository(store, online, backend)
        repo.signIn("volunteer@example.org", "password")
        online.value = false
        repo.advanceTask("task_03")
        repo.advanceTask("task_03")
        repo.markAnnouncementRead("announcement_01")
        assertEquals(2, store.state.value.pendingCount)
        assertEquals(TaskStatus.DONE, store.state.value.activeData!!.tasks.first { it.id == "task_03" }.status)
        repo.refresh()
        assertEquals(0, backend.updateCalls)
        online.value = true
        backend.failWrites = true
        repo.refresh()
        assertEquals(2, store.state.value.pendingCount)
        assertNotNull(repo.refreshStatus.value.syncError)
        backend.failWrites = false
        repo.refresh()
        assertEquals(0, store.state.value.pendingCount)
        assertEquals(TaskStatus.DONE, backend.data.tasks.first { it.id == "task_03" }.status)
        assertTrue(backend.data.announcements.first { it.id == "announcement_01" }.isRead)
        assertNull(repo.refreshStatus.value.syncError)
    }

    @Test fun accountSwitchKeepsOutboxesSeparate() = runTest {
        val store = MemoryStore()
        val online = MutableStateFlow(true)
        val backend = FakeBackend()
        val repo = repository(store, online, backend)
        repo.signIn("first@example.org", "password")
        repo.saveMessageDraft("task_02", "First volunteer's unfinished message.")
        online.value = false
        repo.advanceTask("task_03")
        repo.signOut()
        backend.data = MockRepository.data.copy(profile = MockRepository.profile.copy(id = "second-user"))
        online.value = true
        repo.signIn("second@example.org", "password")
        assertEquals("second-user", store.state.value.activeUserId)
        assertEquals(0, store.state.value.pendingCount)
        assertTrue(store.state.value.messageDrafts.isEmpty())
        repo.saveMessageDraft("task_02", "Second volunteer's message.")
        assertEquals("First volunteer's unfinished message.", store.state.value.accounts[MockRepository.profile.id]!!.messageDrafts["task_02"])
        assertEquals("Second volunteer's message.", store.state.value.messageDrafts["task_02"])
        assertEquals(TaskStatus.TODO, store.state.value.activeData!!.tasks.first { it.id == "task_03" }.status)
        assertEquals(1, store.state.value.accounts[MockRepository.profile.id]!!.pendingTaskStatuses.size)
    }

    @Test fun unexpectedBackendAccountCannotReceiveQueuedReads() = runTest {
        val store = MemoryStore()
        val online = MutableStateFlow(true)
        val backend = FakeBackend()
        val repo = repository(store, online, backend)
        repo.signIn("volunteer@example.org", "password")
        online.value = false
        repo.markAnnouncementRead("announcement_01")
        backend.userOverride = "different-user"
        online.value = true
        repo.refresh()
        assertEquals(0, backend.readCalls)
        assertEquals(1, store.state.value.pendingCount)
    }

    @Test fun cachedWeatherRemainsWhenExternalServiceFails() = runTest {
        val saved = WeatherSnapshot(-3.0, -6.0, 10.0, 71, "2026-10-12T09:00", 1)
        val store = MemoryStore(LocalState(weather = saved))
        val repo = repository(store, MutableStateFlow(true), source = WeatherSource { throw IOException("Service unavailable") })
        repo.refresh(forceWeather = true)
        assertEquals(saved, store.state.value.weather)
        assertNotNull(repo.refreshStatus.value.weatherError)
        assertFalse(repo.refreshStatus.value.refreshing)
    }

    @Test fun duplicateCheckInSurvivesRepositoryRecreation() = runTest {
        val store = MemoryStore()
        val repo = repository(store, MutableStateFlow(false))
        repo.startDemo()
        val first = repo.checkIn(MockRepository.demoQr)
        val reopened = repository(MemoryStore(Json.decodeFromString<LocalState>(Json.encodeToString(store.state.value))), MutableStateFlow(false))
        val second = reopened.checkIn(MockRepository.demoQr)
        assertEquals(first.id, second.id)
        assertTrue(second.duplicate)
        assertEquals(1, store.state.value.demoCheckIns.size)
    }

    @Test fun failedDiskCommitDoesNotPublishTaskProgress() = runTest {
        val store = MemoryStore(LocalState(sessionMode = SessionMode.DEMO))
        val repo = repository(store, MutableStateFlow(false))
        store.failWrites = true
        assertTrue(runCatching { repo.advanceTask("task_03") }.exceptionOrNull() is IOException)
        assertEquals(TaskStatus.TODO, store.state.value.demoData.tasks.first { it.id == "task_03" }.status)
    }

    @Test fun pendingLocalChangesOverlayAnOlderServerSnapshot() {
        val cached = CachedAccount(MockRepository.data, mapOf("task_03" to TaskStatus.DONE), setOf("announcement_01"))
        val merged = mergePending(MockRepository.data, cached)
        assertEquals(TaskStatus.DONE, merged.tasks.first { it.id == "task_03" }.status)
        assertTrue(merged.announcements.first { it.id == "announcement_01" }.isRead)
    }

    @Test fun invalidCheckpointAndIncidentAreRejectedBeforeSaving() = runTest {
        val store = MemoryStore(LocalState(sessionMode = SessionMode.DEMO))
        val repo = repository(store, MutableStateFlow(false))
        assertTrue(runCatching { repo.checkIn("unrelated-qr") }.isFailure)
        assertTrue(runCatching { repo.checkIn(MockRepository.demoQr.replace("narvik-morning", "another-shift")) }.isFailure)
        assertTrue(runCatching { repo.submitIncident(IncidentDraft(description = " "), null, null) }.isFailure)
        assertTrue(store.state.value.demoCheckIns.isEmpty())
        assertTrue(store.state.value.demoIncidents.isEmpty())
    }
}

private class MemoryStore(initial: LocalState = LocalState()) : LocalStore {
    override val state = MutableStateFlow(initial)
    var failWrites = false
    override suspend fun initialize() = Unit
    override suspend fun update(change: (LocalState) -> LocalState) { if (failWrites) throw IOException("Disk full"); state.value = change(state.value) }
}

private class FakeBackend : VolunteerRepository {
    override val isRemote = true
    var data = MockRepository.data
    var failWrites = false
    var updateCalls = 0
    var readCalls = 0
    var sendCalls = 0
    var beforeMessageResult: (suspend () -> Unit)? = null
    var userOverride: String? = null
    override val signedInUserId get() = userOverride ?: data.profile.id
    override suspend fun restoreSession() = true
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun loadVolunteerData() = data
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) { updateCalls++; if (failWrites) throw IOException("Offline"); data = data.copy(tasks = data.tasks.map { if (it.id == taskId) it.copy(status = status) else it }) }
    override suspend fun markAnnouncementRead(announcementId: String) { readCalls++; data = data.copy(announcements = data.announcements.map { if (it.id == announcementId) it.copy(isRead = true) else it }) }
    override suspend fun sendMessage(taskId: String, body: String): TaskConversation {
        sendCalls++
        if (failWrites) throw IOException("Service unavailable")
        beforeMessageResult?.invoke()
        val previous = data.conversations.firstOrNull { it.taskId == taskId } ?: TaskConversation("test_$taskId", taskId, "Coordinator", emptyList(), 0)
        val updated = previous.copy(messages = previous.messages + ChatMessage("sent_$sendCalls", "Volunteer", body, "10:00", true))
        data = data.copy(conversations = data.conversations.filterNot { it.taskId == taskId } + updated)
        return updated
    }
    override suspend fun checkIn(payload: String): CheckInResult = error("Unused")
    override suspend fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?): IncidentReceipt = error("Unused")
}
