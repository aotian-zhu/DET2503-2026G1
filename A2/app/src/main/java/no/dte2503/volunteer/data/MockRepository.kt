package no.dte2503.volunteer.data

object MockRepository {
    val scenarios = listOf(
        SupportScenario("welcome", "Visitor welcome", "Arrival guidance and first contact support."),
        SupportScenario("mobility", "Mobility support", "Route and accessibility assistance."),
        SupportScenario("logistics", "Event logistics", "Supplies, setup and operational support."),
    )

    val permissions = listOf(
        Permission("visitor_checkin", "Check in visitors"),
        Permission("task_update", "Update assigned tasks"),
        Permission("supply_access", "Access supply points"),
        Permission("incident_report", "Submit incident reports"),
    )

    val functions = listOf(
        VolunteerFunction(
            "arrival_guide", "Arrival guide", "Welcomes visitors and helps them reach the right service.",
            setOf("welcome", "mobility"), setOf("visitor_checkin", "task_update", "incident_report")
        ),
        VolunteerFunction(
            "route_support", "Route support", "Provides wayfinding and accessibility support along the route.",
            setOf("mobility"), setOf("task_update", "incident_report")
        ),
        VolunteerFunction(
            "logistics_helper", "Logistics helper", "Keeps volunteer stations equipped and ready.",
            setOf("logistics"), setOf("task_update", "supply_access")
        ),
    )

    val locations = listOf(
        ServiceLocation(
            "central_station", "Oslo Central Station", "Main arrival and welcome point.",
            GeoPointData(59.9109, 10.7522), setOf("welcome", "mobility")
        ),
        ServiceLocation(
            "opera_house", "Opera House support point", "Water, directions and accessible route support.",
            GeoPointData(59.9075, 10.7531), setOf("mobility")
        ),
        ServiceLocation(
            "city_hall", "City Hall volunteer desk", "Coordination desk and supply collection.",
            GeoPointData(59.9111, 10.7336), setOf("welcome", "logistics")
        ),
        ServiceLocation(
            "akershus", "Akershus information point", "Outdoor information and route checkpoint.",
            GeoPointData(59.9077, 10.7365), setOf("welcome", "mobility")
        ),
    )

    val profile = VolunteerProfile(
        "volunteer_104", "Alex Morgan", "alex.morgan@example.org", "+47 900 12 345",
        setOf("arrival_guide", "route_support")
    )

    val shift = Shift("shift_01", "City welcome shift", "09:00", "14:00", "central_station")

    val announcements = listOf(
        Announcement(
            "announcement_01", "Meeting point updated",
            "Use the east entrance at Oslo Central Station for the 10:30 briefing.",
            "Operations team", "08:42", MessagePriority.IMPORTANT, false
        ),
        Announcement(
            "announcement_02", "Weather reminder",
            "Light rain is expected after 12:00. Bring the supplied rain jacket.",
            "Shift coordinator", "08:15", MessagePriority.NORMAL, true
        ),
    )

    val tasks = listOf(
        VolunteerTask(
            "task_01", "Open the welcome point", "Check signage and prepare visitor information.",
            "central_station", TaskPlace(GeoPointData(59.91116, 10.75082), "East entrance welcome desk", "Ground floor"),
            "welcome", setOf("visitor_checkin"), "09:00", TaskStatus.DONE
        ),
        VolunteerTask(
            "task_02", "Guide mobility group", "Meet the group and use the accessible route to the Opera House.",
            "central_station", TaskPlace(GeoPointData(59.91063, 10.75263), "Platform 4 accessible meeting point", "Lower level"),
            "mobility", setOf("task_update"), "10:30", TaskStatus.IN_PROGRESS
        ),
        VolunteerTask(
            "task_03", "Check route signage", "Confirm all route signs are visible and report issues.",
            "opera_house", TaskPlace(GeoPointData(59.90731, 10.75418), "West foyer route checkpoint", "Ground floor"),
            "mobility", setOf("incident_report"), "12:00", TaskStatus.TODO
        ),
        VolunteerTask(
            "task_04", "Collect information packs", "Pick up additional packs for the afternoon team.",
            "city_hall", TaskPlace(GeoPointData(59.91145, 10.73308), "Volunteer supply room", "1st floor"),
            "logistics", setOf("supply_access"), "13:15", TaskStatus.TODO
        ),
    )

    val conversations = listOf(
        TaskConversation(
            "conversation_02", "task_02", "Maya, shift coordinator",
            listOf(
                ChatMessage("message_01", "Maya", "The group arrives near platform 4. Let me know when you are in position.", "09:48", false),
                ChatMessage("message_02", "Volunteer", "Understood. I am heading there now.", "09:51", true),
            ),
            1,
        ),
        TaskConversation(
            "conversation_03", "task_03", "Jonas, route lead",
            listOf(
                ChatMessage("message_03", "Jonas", "Please photograph any damaged or missing route signs.", "10:05", false),
            ),
            0,
        ),
    )
}
