package no.dte2503.volunteer.data

// Fictional volunteer operations at approximate Narvik points, not an official 2029 plan.
object MockRepository {
    val centre = GeoPointData(68.4384, 17.4272)
    const val demoQr = "arena-checkin:v1:narvik-morning:00000000-0000-4000-8000-000000000001:narvik-demo"
    val scenarios = listOf(
        SupportScenario("welcome", "Guest services", "Welcome visitors and help them find their way."),
        SupportScenario("mobility", "Accessibility", "Keep step-free routes clear and support visitor mobility."),
        SupportScenario("logistics", "Venue operations", "Prepare supplies and keep the volunteer hub ready."),
        SupportScenario("visitor-guidance", "Visitor guidance", "Assist guests at the arena."),
        SupportScenario("crowd-support", "Crowd support", "Monitor visitor flow and report obstructions."),
    )
    val permissions = listOf(Permission("visitor_checkin", "Checkpoint check-in"), Permission("task_update", "Update assigned tasks"), Permission("supply_access", "Collect volunteer supplies"), Permission("incident_report", "Report on-site issues"))
    val functions = listOf(
        VolunteerFunction("arrival_guide", "Guest services", "A warm welcome, even on a cold day. Guide arriving fans and answer questions.", setOf("welcome", "mobility"), setOf("visitor_checkin", "task_update", "incident_report")),
        VolunteerFunction("route_support", "Accessibility team", "Help every visitor get around comfortably. Check signs and step-free access.", setOf("mobility"), setOf("task_update", "incident_report")),
        VolunteerFunction("logistics_helper", "Venue operations", "Keep the hub stocked and prepare it for the next team.", setOf("logistics"), setOf("task_update", "supply_access", "incident_report")),
        VolunteerFunction("guest-service", "Guest services", "Welcome visitors and support registration.", setOf("visitor-guidance"), setOf("visitor_checkin", "task_update", "incident_report")),
        VolunteerFunction("crowd-support", "Crowd support", "Assist spectators and keep aisles clear.", setOf("crowd-support"), setOf("task_update", "incident_report")),
    )
    val locations = listOf(
        ServiceLocation("volunteer_hub", "Volunteer hub", "Briefing, warm drinks and shift coordination near the town centre.", centre, setOf("welcome", "logistics")),
        ServiceLocation("station", "Narvik station", "Arrival meeting point for fans coming by train.", GeoPointData(68.4417, 17.4411), setOf("welcome", "mobility")),
        ServiceLocation("base_camp", "Narvikfjellet base", "Venue access and mountain-side visitor support.", GeoPointData(68.4324, 17.4454), setOf("welcome", "mobility")),
        ServiceLocation("town_square", "Town square", "Fan information and a city meeting point.", GeoPointData(68.4386, 17.4278), setOf("welcome")),
        ServiceLocation("rest_point", "Volunteer rest point", "An indoor break and supply point near the centre.", GeoPointData(68.4361, 17.4250), setOf("logistics")),
    )
    val profile = VolunteerProfile("volunteer_104", "Alex Morgan", "alex.morgan@example.org", "", setOf("arrival_guide", "route_support", "logistics_helper"))
    val shift = Shift("narvik-morning", "Morning · Guest services", "08:00", "14:00", "volunteer_hub")
    val tasks = listOf(
        VolunteerTask("task_01", "Prepare the welcome desk", "Lay out visitor maps, check your radio and collect your volunteer badge.", "volunteer_hub", TaskPlace(centre, "Welcome desk", "Ground floor"), "welcome", setOf("visitor_checkin"), "08:00", TaskStatus.DONE),
        VolunteerTask("task_02", "Welcome arriving fans", "Meet the arriving group outside Narvik station. Help visitors find the venue shuttle meeting point.", "station", TaskPlace(GeoPointData(68.4415, 17.4410), "Station forecourt", null), "welcome", setOf("task_update"), "09:15", TaskStatus.IN_PROGRESS),
        VolunteerTask("task_03", "Check the accessible route", "Inspect the entrance near the mountain base. Report blocked paths, missing signs or icy surfaces.", "base_camp", TaskPlace(GeoPointData(68.4326, 17.4451), "Step-free entrance", null), "mobility", setOf("incident_report"), "10:30", TaskStatus.TODO),
        VolunteerTask("task_04", "Restock the volunteer hub", "Collect information packs and drinking water. Leave the handover area ready for the afternoon team.", "rest_point", TaskPlace(GeoPointData(68.4361, 17.4250), "Supply collection", "Ground floor"), "logistics", setOf("supply_access"), "12:00", TaskStatus.TODO),
        VolunteerTask("task_05", "Support the fan meeting point", "Help visitors find their way from the town square and share the latest venue information.", "town_square", TaskPlace(GeoPointData(68.4386, 17.4278), "Information point", null), "welcome", setOf("task_update"), "13:00", TaskStatus.TODO),
    )
    val announcements = listOf(
        Announcement("announcement_01", "Your morning briefing", "Meet at the volunteer hub before heading to your station. Bring your badge and collect a radio.", "Operations team", "07:40", MessagePriority.IMPORTANT, false),
        Announcement("announcement_02", "A little Arctic preparation", "Wear layers and bring your event jacket. Check the live Narvik weather before an outdoor assignment.", "Maya · Shift coordinator", "07:30", MessagePriority.NORMAL, false),
        Announcement("announcement_03", "Welcome to the team", "Thanks for being part of Narvik 2029. Your tasks, meeting points and team messages are all here.", "Volunteer team", "07:15", MessagePriority.NORMAL, true),
    )
    val conversations = listOf(
        TaskConversation("conversation_02", "task_02", "Maya · Shift coordinator", listOf(ChatMessage("message_01", "Maya", "Meet the group on the station forecourt. Let me know when you are in position.", "08:50", false)), 1),
        TaskConversation("conversation_03", "task_03", "Jonas · Accessibility lead", listOf(ChatMessage("message_02", "Jonas", "Please report any obstacles at the step-free entrance. A photo helps us locate the issue.", "08:40", false)), 0),
    )
    val data get() = VolunteerData(profile, shift, locations, tasks, announcements, conversations)
}
