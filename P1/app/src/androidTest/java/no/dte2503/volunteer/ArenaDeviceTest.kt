package no.dte2503.volunteer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import android.util.AtomicFile
import java.io.File
import no.dte2503.volunteer.data.RepositoryProvider
import no.dte2503.volunteer.data.LocalState
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.rules.ExternalResource

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ArenaDeviceTest {
    private val testFile get() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "arena-device-test.json")
    @get:Rule(order = 0) val isolatedStorage = object : ExternalResource() {
        override fun before() { AtomicFile(testFile).delete(); RepositoryProvider.testStateFile = testFile }
        override fun after() { RepositoryProvider.testStateFile = null; AtomicFile(testFile).delete() }
    }
    @get:Rule(order = 1) val ui = createAndroidComposeRule<MainActivity>()

    @Before fun openDemo() {
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("explore_demo").fetchSemanticsNodes().isNotEmpty() || ui.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
        if (ui.onAllNodesWithTag("explore_demo").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithTag("explore_demo").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("nav_home").performClick()
    }

    @Test fun a_allFiveDestinationsAreReachable() {
        listOf("tasks", "map", "inbox", "profile", "home").forEach { destination ->
            ui.onNodeWithTag("nav_$destination").performClick()
            ui.onNodeWithTag("${destination}_screen").assertIsDisplayed()
        }
    }

    @Test fun b_taskProgressAndFiltersWork() {
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("filter_Open").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("advance_task_04"))
        ui.onNodeWithTag("advance_task_04").performClick()
        ui.waitUntil(15_000) { ui.onAllNodes(hasText("In progress") and hasAnyAncestor(hasTestTag("task_task_04"))).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("advance_task_04"))
        ui.onNodeWithTag("advance_task_04").performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("task_task_04").fetchSemanticsNodes().isEmpty() }
        ui.onNodeWithTag("tasks_screen").performScrollToIndex(0)
        ui.onNodeWithTag("filter_Done").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("task_task_04"))
        ui.onNodeWithTag("task_task_04").assertIsDisplayed()
        ui.onNodeWithTag("reopen_task_04").performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("task_task_04").fetchSemanticsNodes().isEmpty() }
        ui.onNodeWithTag("tasks_screen").performScrollToIndex(0)
        ui.onNodeWithTag("filter_Open").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("task_task_04"))
        ui.onNodeWithTag("task_task_04").assertIsDisplayed()
        ui.onNode(hasText("In progress") and hasAnyAncestor(hasTestTag("task_task_04"))).assertIsDisplayed()
    }

    @Test fun c_taskToMapAndFacilitySelectionWork() {
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("filter_All").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("map_task_02"))
        ui.onNodeWithTag("map_task_02").performClick()
        ui.onNodeWithTag("map_screen").assertIsDisplayed()
        ui.onNodeWithTag("map_task_task_02").assertIsDisplayed()
        ui.onNodeWithTag("facilities_filter").performClick()
        ui.onNodeWithTag("facility_volunteer_hub").performClick()
        ui.onNodeWithTag("facility_volunteer_hub").assertIsDisplayed()
    }

    @Test fun d_coordinatorConversationSavesMessage() {
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("filter_All").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("contact_task_02"))
        ui.onNodeWithTag("contact_task_02").performClick()
        ui.onNodeWithTag("conversation_screen").assertIsDisplayed()
        ui.onNodeWithTag("message_input").performTextInput("Device test: ready at the station.")
        ui.onNodeWithTag("send_message").performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithText("Device test: ready at the station.").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Device test: ready at the station.").assertIsDisplayed()
        ui.onNodeWithTag("message_input").assert(editableText(""))
    }

    @Test fun e_duplicateCheckpointDoesNotCreateAnotherCheckIn() {
        ui.onNodeWithTag("open_actions").performClick()
        ui.onNodeWithTag("demo_checkin").performScrollTo().performClick()
        ui.waitForIdle()
        ui.onNodeWithTag("demo_checkin").performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithText("Already checked in at this checkpoint.").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Already checked in at this checkpoint.").assertIsDisplayed()
    }

    @Test fun f_incidentDraftSurvivesLeavingTheScreen() {
        ui.onNodeWithTag("open_actions").performClick()
        ui.onNodeWithTag("report_tab").performClick()
        ui.onNodeWithTag("incident_description").performScrollTo().performTextReplacement("Device test: missing sign at the entrance.")
        val savedFile = testFile
        ui.waitUntil(15_000) { savedFile.exists() && savedFile.readText().contains("Device test: missing sign at the entrance.") }
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithTag("open_actions").performClick()
        ui.onNodeWithTag("report_tab").performClick()
        ui.onNodeWithTag("incident_description").performScrollTo().assertTextContains("Device test: missing sign at the entrance.")
        ui.onNodeWithTag("submit_incident").performScrollTo().performClick()
        ui.waitUntil(15_000) { ui.onAllNodesWithText("Demo report saved on this device.").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Demo report saved on this device.").assertIsDisplayed()
    }

    @Test fun g_messageDraftSurvivesLeavingAndRecreatingTheScreen() {
        val text = "Device test: please confirm the meeting point."
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("contact_task_02"))
        ui.onNodeWithTag("contact_task_02").performClick()
        ui.onNodeWithTag("message_input").performTextInput(text)
        ui.waitUntil(15_000) { savedState()?.messageDrafts?.get("task_02") == text }
        ui.onNodeWithContentDescription("Back to inbox").performClick()
        ui.onNodeWithTag("nav_home").performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("contact_task_02"))
        ui.onNodeWithTag("contact_task_02").performClick()
        ui.onNodeWithTag("message_input").assert(editableText(text))
        ui.onNodeWithTag("send_message").performClick()
        ui.waitUntil(15_000) { savedState()?.messageDrafts?.containsKey("task_02") == false }
        ui.onNodeWithTag("message_input").assert(editableText(""))
        ui.onNodeWithContentDescription("Back to inbox").performClick()
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("contact_task_02"))
        ui.onNodeWithTag("contact_task_02").performClick()
        ui.onNodeWithTag("message_input").assert(editableText(""))
    }

    @Test fun h_practiceArrivalAndNearestTaskWorkAcrossScreens() {
        ui.onNodeWithTag("nav_map").performClick()
        ui.onNodeWithTag("facilities_filter").performClick()
        ui.onNodeWithTag("map_points_list").performScrollToNode(hasTestTag("facility_volunteer_hub"))
        ui.onNodeWithTag("facility_volunteer_hub").performClick()
        ui.onNodeWithTag("watch_volunteer_hub").performScrollTo().performClick()
        ui.waitUntil(15_000) { savedState()?.arrivalLocationId == "volunteer_hub" }
        ui.onNodeWithTag("demo_walk").performClick()
        ui.onNodeWithTag("practice_outside").performClick()
        ui.onNodeWithTag("arrival_status").assertTextContains("Outside the 150 m arrival zone", substring = true)
        ui.onNodeWithTag("demo_walk").performClick()
        ui.onNodeWithTag("practice_inside").performClick()
        ui.onNodeWithTag("arrival_status").assertTextContains("Inside the 150 m arrival zone", substring = true)
        ui.onNodeWithTag("position_source").assertTextContains("Not your phone GPS", substring = true)
        ui.onNodeWithTag("nav_home").performClick()
        ui.onNodeWithTag("practice_banner").assertIsDisplayed()
        ui.onNodeWithTag("home_screen").performScrollToNode(hasTestTag("nearest_task_card"))
        ui.onNode(hasText("Support the fan meeting point") and hasAnyAncestor(hasTestTag("nearest_task_card")), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("nav_map").performClick()
        ui.onNodeWithTag("arrival_status").assertTextContains("Inside the 150 m arrival zone", substring = true)
    }

    @Test fun i_taskActivitySurvivesActivityRecreation() {
        ui.onNodeWithTag("nav_tasks").performClick()
        ui.onNodeWithTag("tasks_screen").performScrollToNode(hasTestTag("advance_task_04"))
        ui.onNodeWithTag("advance_task_04").performClick()
        ui.waitUntil(15_000) { savedState()?.taskUpdates?.size == 1 }
        ui.onNodeWithTag("nav_inbox").performClick()
        ui.onNodeWithTag("task_updates_filter").performClick()
        ui.onNodeWithText("To do → In progress").assertIsDisplayed()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("task_updates_list").assertIsDisplayed()
        ui.onNodeWithText("To do → In progress").assertIsDisplayed()
    }

    @Test fun j_deviceTestLogSavesAnActualObservation() {
        ui.onNodeWithTag("nav_profile").performClick()
        ui.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("open_device_checks"))
        ui.onNodeWithTag("open_device_checks").performClick()
        ui.onNodeWithTag("device_check_summary").assertTextContains("0 of 8 scenarios recorded")
        ui.onNodeWithTag("device_checks_list").performScrollToNode(hasTestTag("observed_navigation"))
        ui.onNodeWithTag("observed_navigation").performTextInput("Device test: all five destinations opened.")
        ui.onNodeWithTag("save_check_navigation").performScrollTo().performClick()
        ui.waitUntil(15_000) { savedState()?.deviceChecks?.get("navigation")?.observed == "Device test: all five destinations opened." }
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("device_checks_list").performScrollToIndex(0)
        ui.onNodeWithTag("device_check_summary").assertTextContains("1 of 8 scenarios recorded")
    }

    @Test fun k_darkAppearancePersistsAfterActivityRecreation() {
        ui.onNodeWithTag("nav_profile").performClick()
        ui.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("theme_DARK"))
        ui.onNodeWithTag("theme_DARK").performClick()
        ui.waitUntil(15_000) { savedState()?.themeMode == no.dte2503.volunteer.data.ThemeMode.DARK }
        ui.activityRule.scenario.recreate()
        ui.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("theme_DARK"))
        ui.onNodeWithTag("theme_DARK").assertIsSelected()
    }

    private fun savedState(): LocalState? = runCatching { Json.decodeFromString<LocalState>(testFile.readText()) }.getOrNull()
    private fun editableText(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
}
