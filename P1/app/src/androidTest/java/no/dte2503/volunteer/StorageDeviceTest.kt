package no.dte2503.volunteer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import no.dte2503.volunteer.data.*

@RunWith(AndroidJUnit4::class)
class StorageDeviceTest {
    @Test fun realFileOutboxSurvivesOfflineRestartAndRetriesFailedSync() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "sync-storage-test-${UUID.randomUUID()}.json")
        val online = kotlinx.coroutines.flow.MutableStateFlow(true)
        val backend = StorageTestBackend()
        try {
            val first = AtomicLocalStore(file)
            first.initialize()
            val repo = ArenaRepository(first, online, WeatherSource { error("Controlled weather unavailable") }, backend)
            repo.signIn("device-test@example.org", "fixture-password")
            online.value = false
            repo.advanceTask("task_03")
            repo.advanceTask("task_03")
            repo.markAnnouncementRead("announcement_01")
            val restarted = AtomicLocalStore(file)
            restarted.initialize()
            assertEquals(2, restarted.state.value.pendingCount)
            val resumed = ArenaRepository(restarted, online, WeatherSource { error("Controlled weather unavailable") }, backend)
            online.value = true
            backend.failWrites = true
            resumed.refresh()
            assertEquals(2, restarted.state.value.pendingCount)
            assertNotNull(resumed.refreshStatus.value.syncError)
            backend.failWrites = false
            resumed.refresh()
            assertEquals(0, restarted.state.value.pendingCount)
            assertEquals(TaskStatus.DONE, backend.data.tasks.first { it.id == "task_03" }.status)
            assertTrue(backend.data.announcements.first { it.id == "announcement_01" }.isRead)
            val finalRead = AtomicLocalStore(file)
            finalRead.initialize()
            assertEquals(0, finalRead.state.value.pendingCount)
            assertEquals(TaskStatus.DONE, finalRead.state.value.activeData!!.tasks.first { it.id == "task_03" }.status)
        } finally { android.util.AtomicFile(file).delete() }
    }

    @Test fun realAtomicFileRestoresSessionTasksReadStateAndTheme() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "storage-test-${UUID.randomUUID()}.json")
        try {
            val first = AtomicLocalStore(file)
            first.initialize()
            val repo = ArenaRepository(first, kotlinx.coroutines.flow.MutableStateFlow(false), WeatherSource { error("Offline") })
            repo.startDemo()
            repo.advanceTask("task_03")
            repo.advanceTask("task_03")
            repo.reopenTask("task_03")
            repo.markAnnouncementRead("announcement_01")
            repo.setTheme(ThemeMode.DARK)
            repo.checkIn(MockRepository.demoQr)
            repo.saveMessageDraft("task_02", "Storage test: confirm the station meeting point.")
            repo.watchArrival("volunteer_hub")
            repo.saveDeviceCheck("navigation", "Storage test: all screens opened.", true, "0.5.2")
            val second = AtomicLocalStore(file)
            second.initialize()
            assertEquals(SessionMode.DEMO, second.state.value.sessionMode)
            assertEquals(TaskStatus.IN_PROGRESS, second.state.value.demoData.tasks.first { it.id == "task_03" }.status)
            assertTrue(second.state.value.demoData.announcements.first { it.id == "announcement_01" }.isRead)
            assertEquals(ThemeMode.DARK, second.state.value.themeMode)
            assertEquals(1, second.state.value.demoCheckIns.size)
            assertEquals("Storage test: confirm the station meeting point.", second.state.value.messageDrafts["task_02"])
            assertEquals("volunteer_hub", second.state.value.arrivalLocationId)
            assertEquals(3, second.state.value.taskUpdates.size)
            assertEquals("Storage test: all screens opened.", second.state.value.deviceChecks["navigation"]!!.observed)
        } finally { file.delete() }
    }
}

// Controlled service boundary: this test exercises actual Android disk IO, not a live account.
private class StorageTestBackend : VolunteerRepository {
    override val isRemote = true
    var data = MockRepository.data
    var failWrites = false
    override val signedInUserId get() = data.profile.id
    override suspend fun restoreSession() = true
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun loadVolunteerData() = data
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        if (failWrites) throw java.io.IOException("Controlled service failure")
        data = data.copy(tasks = data.tasks.map { if (it.id == taskId) it.copy(status = status) else it })
    }
    override suspend fun markAnnouncementRead(announcementId: String) {
        if (failWrites) throw java.io.IOException("Controlled service failure")
        data = data.copy(announcements = data.announcements.map { if (it.id == announcementId) it.copy(isRead = true) else it })
    }
    override suspend fun sendMessage(taskId: String, body: String): TaskConversation = error("Not part of this test")
    override suspend fun checkIn(payload: String): CheckInResult = error("Not part of this test")
    override suspend fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?): IncidentReceipt = error("Not part of this test")
}
