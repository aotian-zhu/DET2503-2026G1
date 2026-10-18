# P1 assignment audit — version 0.5.2

The teacher's supplied screenshots are the source of this checklist. Required items are implemented. Every Optional and Suggested row has an implementation, with the location/geofencing scope and live-service test limits stated below. This is a course prototype, not a promise of a particular grade.

| Area | Required | Optional | Suggested | Where to demonstrate it |
|---|---|---|---|---|
| Design | Five useful screens, bottom navigation, consistent Compose/Material 3 UI | Persisted System/Light/Dark appearance | Custom Arctic blue, navy and glacier theme | Today, Tasks, Map, Inbox, You; You → appearance |
| OpenStreetMap & GPS | OSM, current position, selectable markers, location permissions | Numbered task/check markers, lettered facility markers, separate position marker and accuracy circle | Continuous GPS updates across the foreground app | Map → Locate me; select task/facility; then open Today or Tasks |
| Location-aware features | Haversine distance, nearest unfinished task and nearest-first facilities | A 150 m arrival geofence with an in-app entry reminder, accuracy checks and 200 m exit hysteresis | Approximate walking times from straight-line distance at 4 km/h | Map → Facilities → select point → Watch arrival; Today meeting distance; task/facility walking labels |
| Communication | No Required cell in the brief | Announcements as a message board; task-linked coordinator threads | Task progression/reopening; persisted My task updates history, with queued/synchronized status | Inbox → Team updates / My task updates; Tasks → Contact coordinator |
| Data flow | Repository + ViewModel | Persistent/transient state separation, busy guards, validation, error/retry state, per-account caches and outboxes | StateFlow, coroutines, lifecycle-aware collection and IO dispatching | Source: MainViewModel, ArenaRepository, AtomicLocalStore |
| Local storage | Private file-system persistence with AtomicFile | Multiple entities: profile, shift, tasks, locations, messages, reads, drafts, check-ins, reports, task history, preferences and test observations | Cached operational data, weather and visited map tiles | Change task/theme/draft, close and reopen offline |
| Offline-first | Saved core functionality without internet | Local task updates; real-account task/read outbox; local demo workflows | Offline/waiting badges, task-level pending labels, retry action and last successful refresh time | Airplane mode with Wi-Fi off; You → Saved data and connection |
| Integration | At least one external source | OpenStreetMap, Open-Meteo and the existing A2 Supabase adapter | Real current Narvik weather and real map imagery; existing team-account data when authenticated | Today → Narvik weather; Map; configured team sign-in |
| Testing | Fictional demo data; documented expected/observed scenarios; emulator/device validation | Geometry, geofence, repository, storage and report-generation unit tests | Compose UI/instrumentation tests; on-device manual test log and text export | docs/TEST_REPORT.md; You → Check this device |

## Precise scope of the optional features

- **Foreground geofencing:** the app evaluates an arrival circle from GPS fixes while the activity is visible. It is not Android's background GeofencingClient service. It does not request background location or notify after the app is closed. Entry requires distance plus accuracy ≤150 m; confirmed exit requires distance minus accuracy ≥200 m. Accuracy worse than 100 m cannot confirm arrival. Boundary/uncertain fixes do not create repeat alerts. Only recent fixes are accepted; stale positions are cleared.
- **Walking times:** estimates at 4 km/h using straight-line distance, limited to 20 km and suitably precise fixes. They are not routed pedestrian directions or promises about a steep/icy route. The map explains hills, snow and access restrictions.
- **Demo walk:** explicitly labelled practice coordinates let a student outside Narvik rehearse arrival and proximity. They do not change the phone's real GPS, are not persisted as a location history, and cannot be enabled in a real team-account session. Show real GPS separately in the recording.
- **Communication:** demo messages remain on the device. Real task conversations use the existing service. My task updates is this device's last 40 task-status changes, not a live coordinator-wide feed. Remote task statuses synchronize through A2's authenticated RPC.
- **Offline sync:** task status/read writes are coalesced and replayed in the foreground. Messages and incident submissions require internet; automatic replay could duplicate these non-idempotent operations. No background worker, general multi-user conflict-resolution protocol or push-notification service is claimed.
- **Multiple integrations:** OSM and Open-Meteo work independently of Supabase login. The existing backend configuration is preserved. No live database, accounts, assignments or schema were changed for this update.
- **Testing:** controlled backend tests are identified as controlled. The student's physical S24 Ultra results must be observed and recorded by the student. The app never marks a manual test passed automatically; untouched tests remain Not run.

## Additional usability features

- Done → Reopen task returns an accidentally completed task to In progress.
- Coordinator message drafts persist per task and account; failed sends retain them.
- Current-weather suggestions help volunteers prepare for outdoor assignments.
- Checkpoint validation/duplicate protection, saved incident drafts and optional photos continue from A2.
- Manual device reports include version/device metadata, expected results and the student's observations, and can be exported through Android's share chooser.

## Submission and recording

- Android Studio project: the P1 directory; A2 remains separate.
- Submission ZIP: deliverables/DTE_2503_26_G1_P1.zip (confirm the course's group-name convention).
- Updated APK: deliverables/ArenaCompanion-v0.5.2-debug.apk.
- Use the new [S24 Ultra phone recording guide](PHONE_RECORDING_GUIDE.md) for the final ten-minute student recording. It covers navigation, map, permissions, location, storage, sync and offline behavior from the second screenshot.
- ArenaCompanion-P1-10min-demo.mp4 is retained as an earlier reference recording, not a recording of 0.5.2. Replace it with your own final phone video before course submission.
- README: setup, compatibility, integrations, limitations and test evidence.

The pictured grading split is 10 points for project management, 35 for design and 55 for application features. The official event name is the 2029 FIS Alpine World Ski Championships in Narvik. Demo operations/people/points are fictional and approximate; weather is current Narvik weather, not a forecast for 2029.
