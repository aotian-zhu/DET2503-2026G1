# P1 brief and implementation scope

The two assignment screenshots supplied by the student are the source of this checklist. P1 is **Arena Companion v0.5**, a coherent Android application supporting volunteers, staff or event operations. It must extend A2 and show progress in functionality, architecture, usability and robustness. The teacher does not require a finished commercial product.

## Required features

| Assignment requirement | P1 implementation | Main evidence |
|---|---|---|
| At least three meaningful screens, navigation, consistent UI | Today, Tasks, Map, Inbox, You, plus check-in/report workflows | Compose screens and bottom navigation |
| Modern Android UI practices | Jetpack Compose and Material 3; Arctic blue theme | `ui/theme/Theme.kt` |
| OpenStreetMap, current position, markers, interaction, permissions | osmdroid map, GPS marker, task/facility markers, selection, runtime permission handling | `MapScreen.kt`, `LocationTracker.kt` |
| A distance or proximity feature | Straight-line task/facility distances, nearest-first map lists and nearest unfinished task on Today | `LocationMath.kt` |
| Repository and ViewModel architecture | UI → MainViewModel → ArenaRepository → local/network sources | `MainViewModel.kt`, `data/` |
| Relevant information stored locally | Atomic JSON snapshot in private app storage; local incident photos | `LocalStore.kt` |
| Core functionality without internet | Saved shift, tasks, locations, announcements, demo workflows and GPS-based distances | Offline and restart scenarios |
| At least one external data source | Live Narvik weather from Open-Meteo, cached with its fetch time; OSM imagery; optional A2 Supabase backend | `WeatherSource.kt`, `SupabaseVolunteerRepository.kt` |
| Core functionality tested on a device/emulator | Compose device tests plus manual emulator checks | `TEST_REPORT.md` |
| At least five test scenarios with expected and observed results | Navigation, tasks, map, permissions, storage, offline, messages and workflows | `TEST_REPORT.md` |

## Optional or suggested features included

- System, light and dark appearance, persisted between launches.
- Announcements and task-linked coordinator conversations.
- StateFlow, coroutines and lifecycle-aware collection.
- Foreground GPS updates while viewing the map.
- Visible offline and synchronization state.
- A persistent outbox for backend task changes and announcement reads.
- QR check-in, duplicate protection in demo mode, incident reporting and photos inherited from A2 and improved.
- Unit and device UI tests.

Geofencing, background tracking, routing/travel-time estimates, push notifications, a coordinator dashboard and general conflict resolution are outside this milestone. They are optional, not required by the screenshots.

## Submission

- Android Studio project in the required ZIP naming pattern: `DTE_2503_26_TEAMNAME_P1.zip`.
- Demonstration video: approximately ten minutes.
- README covering setup, known bugs and limitations.

The pictured grading split is 10 points for project management, 35 for application design and 55 for application features. The implementation therefore focuses on usable workflows and required mobile capabilities.

## Product assumptions

- The official event is the **2029 FIS Alpine World Ski Championships in Narvik**. The student used “world cup”; the app uses the organizer's event name. [Organizer](https://www.narvik2029.com/).
- Volunteer schedules, venue operations, people and coordinates in the demo are fictional and approximate. They are not the official event plan.
- Current weather is current weather in Narvik, not a forecast for 2029. [Open-Meteo API](https://open-meteo.com/en/docs).
- A2 is the baseline and remains in its original folder. All P1 source, documentation and artifacts are inside `P1`.
