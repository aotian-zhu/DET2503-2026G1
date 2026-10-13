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
            val second = AtomicLocalStore(file)
            second.initialize()
            assertEquals(SessionMode.DEMO, second.state.value.sessionMode)
            assertEquals(TaskStatus.IN_PROGRESS, second.state.value.demoData.tasks.first { it.id == "task_03" }.status)
            assertTrue(second.state.value.demoData.announcements.first { it.id == "announcement_01" }.isRead)
            assertEquals(ThemeMode.DARK, second.state.value.themeMode)
            assertEquals(1, second.state.value.demoCheckIns.size)
            assertEquals("Storage test: confirm the station meeting point.", second.state.value.messageDrafts["task_02"])
        } finally { file.delete() }
    }
}
