package no.dte2503.volunteer.data

data class DeviceTestScenario(val id: String, val title: String, val steps: String, val expected: String)

val deviceTestScenarios = listOf(
    DeviceTestScenario("navigation", "Navigation", "Visit Today, Tasks, Map, Inbox and You. Open and leave a task conversation.", "Every screen opens and Back returns to the expected place."),
    DeviceTestScenario("tasks", "Complete and reopen a task", "Start an open task, complete it, find it under Done, then tap Reopen task.", "The task returns to Open as In progress. My task updates records the changes."),
    DeviceTestScenario("map", "Map and marker interaction", "Open a task on Map, pan/zoom, switch to Facilities and select the volunteer hub.", "The correct point/card is selected. Task numbers, facility symbols and map attribution are visible."),
    DeviceTestScenario("permission", "Location permission recovery", "Use real phone GPS. Deny location access, then allow it through the app's Settings shortcut and return.", "Task markers remain without invented distances after denial. A location fix and accuracy appear after permission is restored."),
    DeviceTestScenario("proximity", "GPS, walking estimates and arrival", "Use Locate me. Then in demo, select Facilities → Volunteer hub → Watch arrival; rehearse Outside hub then Inside hub.", "Real GPS is labelled GPS. Demo walk is labelled Practice position. Entering the 150 m zone produces one arrival notice; repeated inside fixes do not repeat it."),
    DeviceTestScenario("offline", "Offline restart and local storage", "Load data/map online. Disable Wi-Fi and mobile data, change a task, save a message draft, close and reopen the app.", "The offline banner appears. Saved task progress and draft survive restart. Cached weather is labelled Saved reading; unvisited tiles may be missing."),
    DeviceTestScenario("sync", "Backend synchronization", "With a real team account, change a task offline, observe its waiting status, reconnect and refresh. If only using demo, leave this test unrecorded.", "The account's pending update survives offline restart, reaches the service after reconnect and the waiting count returns to zero. Demo storage is not server sync."),
    DeviceTestScenario("appearance", "Appearance and message persistence", "Choose Dark in You. Leave an unfinished task message, restart the app, reopen the thread, then send it.", "Dark appearance and the correct task's draft return. Sending clears the draft only after success."),
)

fun deviceTestReport(version: String, device: String, observations: Map<String, DeviceTestObservation>): String = buildString {
    appendLine("Arena Companion $version — device test report")
    appendLine("Device: $device")
    appendLine("Results are manually recorded observations, not automatic assertions.")
    deviceTestScenarios.forEach { scenario ->
        val result = observations[scenario.id]?.takeIf { it.appVersion == version }
        appendLine()
        appendLine("${scenario.title}: ${result?.let { if (it.passed) "PASS" else "ISSUE" } ?: "NOT RUN"}")
        appendLine("Steps: ${scenario.steps}")
        appendLine("Expected: ${scenario.expected}")
        appendLine("Observed: ${result?.observed ?: "Not recorded on this app version."}")
        result?.let { appendLine("Recorded at: ${java.time.Instant.ofEpochMilli(it.recordedAtMillis)}") }
    }
}
