# Arena Companion v0.5 — P1

An Android volunteer companion for **Narvik 2029, the FIS Alpine World Ski Championships**. P1 extends the A2 application with a consistent Arctic blue interface, persistent local data, foreground GPS, proximity features and cached current weather.

The current build is **0.5.2** (version code 7). It adds walking estimates, custom map markers/accuracy circles, foreground arrival reminders, continuous foreground GPS across the app, task activity, clearer sync status and an on-device expected/observed test log. **Reopen task** remains available for accidental completions. The supplied APK preserves the existing A2 Supabase configuration, with account sign-in and demo access together. No live accounts, passwords, database schema or assignments were changed.

Open **this `P1` directory** as the Android Studio project. A2 stays in its original directory. P1 uses the application ID `no.dte2503.volunteer.p1`, so both versions can be installed together.

## Try the app

Run the app and tap **Explore the volunteer demo**. No password or API key is required. The demo contains a volunteer profile, five tasks, a shift, Narvik meeting points, announcements and coordinator conversations. Demo shifts, people and operations are fictional; coordinates are approximate.

- **Today:** shift summary, completion progress, current Narvik weather/preparation suggestions, meeting-point distance, walking estimate and closest unfinished task after a GPS fix.
- **Tasks:** All/Open/Done filters, task progression, **Reopen task** to undo a completion, map links and coordinator conversations.
- **Map:** OpenStreetMap, numbered task/check markers, lettered facilities, current position/accuracy, nearest-first lists, straight-line distances, walking estimates and a 150 m foreground arrival zone. **Demo walk** uses labelled practice positions so a student outside Narvik can rehearse proximity.
- **Inbox:** announcements, persistent read state, task-linked conversations, per-task/account message drafts and the last 40 **My task updates**.
- **You:** roles, work access, saved appearance, connection/sync state and **Check this device**, where the student records actual expected/observed results and exports a versioned report.
- **On-site essentials:** QR check-in with duplicate protection, validated checkpoint entry, incident reports, saved drafts and optional photos.

The demo clearly identifies operations that stay on the device. The weather integration is real: it requests current Narvik conditions from [Open-Meteo](https://open-meteo.com/en/docs). It is not a weather forecast for the event in 2029.

## Build and run

1. Open `P1` in Android Studio and allow Gradle to sync.
2. Install Android SDK **35** and Build Tools **35.0.0** when prompted.
3. Select **JDK 17 or 21** for Gradle. The project's A2 wrapper is Gradle 8.11.1 and does not work with the newer Android Studio JDK 25. Validation on the provided computer used its installed JDK 23.
4. Use an emulator or Android device running API 26 or newer. Run the `app` configuration.
5. Android Studio writes `local.properties` with your SDK location. Do not submit that machine-specific file. A documented example is provided.

If the Pixel 10 emulator opens a black screen or reports an unresponsive Android System UI, edit its settings in Device Manager: **Additional Settings → Emulated Performance → Graphics acceleration → Software**, then use **Cold Boot**. The problematic device used an Android API 37 preview image; Android 15/API 35 is also suitable for this project. Software rendering and a cold boot preserve the emulator's installed apps/data. [Android graphics settings](https://developer.android.com/studio/run/emulator-acceleration).

On Windows, after setting `JAVA_HOME` to a compatible JDK:

```powershell
.\tools\build.ps1
# Add device tests when an emulator/phone is connected:
.\tools\build.ps1 -DeviceTests
```

Equivalent Gradle checks:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
gradlew.bat :app:connectedDebugAndroidTest
```

The debug APK is built at `app/build/outputs/apk/debug/app-debug.apk`. A convenience copy is included in `deliverables` after validation.

The current installable copy is **[ArenaCompanion-v0.5.2-debug.apk](deliverables/ArenaCompanion-v0.5.2-debug.apk)**. For the student's Galaxy S24 Ultra, follow [update and recording setup](docs/S24_ULTRA_UPDATE.md), then the [timed ten-minute phone guide with narration](docs/PHONE_RECORDING_GUIDE.md). Install it over the existing P1 app to retain saved data. Version 0.5.2 keeps schema-1 data readable through default values for the new fields.

## Emulator location and permissions

Use the emulator's Extended Controls → Location and set **latitude 68.4384, longitude 17.4272**. Open Map and tap **Locate me**. Choose precise or approximate location. The app remains useful if permission is denied or device location is off. With Android's device bridge:

```text
adb emu geo fix 17.4272 68.4384
```

The command order is longitude, then latitude. GPS accuracy is shown and distances are explicitly straight-line. The app does not track in the background or send the volunteer's GPS position to a server.

After permission is granted, one GPS subscription runs across the app while the activity is visible and stops on pause. Last-known fixes older than 30 seconds are rejected; a position without an update for 60 seconds is cleared. Map → Facilities → select a point → **Watch arrival** saves one arrival target per account/demo. Entry requires distance plus accuracy ≤150 m; exit requires distance minus accuracy ≥200 m, avoiding repeated boundary alerts. Accuracy worse than 100 m cannot confirm entry. This is foreground geofencing with an in-app reminder, not a background GeofencingClient service.

Walking estimates assume 4 km/h along the straight-line distance (maximum 20 km), with a suitably precise fix. They are not routed directions; terrain, snow and access restrictions can increase travel time. **Demo walk** pauses real GPS display and uses clearly marked practice coordinates. It is available only in demo and does not persist a movement history.

The camera permission is requested only when scanning a QR code. Selecting an incident photo uses Android's document picker, without broad storage permission. JPEG, PNG and WebP up to 5 MB are accepted.

The demonstration checkpoint payload is:

```text
arena-checkin:v1:narvik-morning:00000000-0000-4000-8000-000000000001:narvik-demo
```

Use the built-in **Try the demo checkpoint** button, or encode this payload into a QR code for camera testing.
The printable [demo checkpoint QR](docs/demo-checkpoint.png) is included for the video demonstration.

## Offline storage and external integration

The persistent snapshot is `files/arena-state.json` in private app storage. Android `AtomicFile` provides interrupted-write recovery; a coroutine mutex serializes writes, and the saved state is published only after a successful commit. Photos are stored in a private `incident-photos` directory. Backups are disabled for the prototype's operational data. Clearing app data/uninstalling removes local demo data and caches.

Without internet, you can reopen your saved shift, view and progress tasks, read announcements/conversations, calculate GPS distances and use demo workflows. Message drafts, incident drafts and attached document URIs survive restarts. Message drafts are separated by task, backend account and demo session; a successful send clears only the submitted draft. Current weather is cached and labeled with its fetch time. Browsed OSM tiles may be cached; unvisited map imagery needs a connection. There is no bulk tile download.

The optional backend has an account-specific cache and a persistent outbox for task status changes and announcement reads. Reconnection, launch, returning to the foreground and manual refresh trigger foreground sync. Repeated offline changes to a task coalesce to its latest status. Failed sync retains pending writes. Remote messages, incidents and QR confirmations require internet; those non-idempotent operations are not automatically replayed.

## Existing A2 Supabase accounts

The self-contained demo and weather already meet P1's required external-source/offline functionality. The existing A2 Supabase adapter is preserved for your group's account-based workflows.

The current local P1 configuration and supplied APK use A2's existing service. From the welcome screen, enter an existing volunteer email/password and tap **Sign in to your team**. If a saved demo session opens directly, go to **You → Leave demo** first. **Explore the volunteer demo** remains available without an account.

Backend settings stay in ignored `local.properties`, so a fresh clone/source ZIP needs the following configuration before rebuilding for account access. The Supabase client key embedded in the configured APK is a public publishable key; no password, admin key or service-role key is bundled.

1. Copy `SUPABASE_URL` and the **publishable/anon client key** from your own A2 `local.properties` into P1's ignored `local.properties`. Never put a service-role key in an Android app. Rebuild to enable the volunteer sign-in form.
2. For a new development database, apply migrations in this order: `initial_schema.sql`, `20260929_secure_task_status_and_profiles.sql`, `20260929_volunteer_workflows.sql`.
3. Create a test user in Supabase Authentication. Edit `volunteer_email` in `supabase/seed.sql` and run that development seed. It adds the fictional Narvik scenario and prints a checkpoint payload.
4. For an existing A2 database, do not reapply table-creation migrations. Review the P1 seed on a development copy. Existing accounts keep their existing backend assignments and coordinates until you deliberately update the data.
5. Sign in with that account. Complete a first online load, then test offline/reconnect behavior. No live database was changed during P1 implementation.

Automated backend synchronization tests use a controlled fake service. Live Supabase authentication/storage/RPC behavior requires your group's test account and deployed schema; it is not claimed as tested here.

The existing Supabase email-authentication settings endpoint was checked and is reachable. Actual sign-in with the two volunteer accounts still needs their passwords and a manual check by the group. The existing `update_own_task_status` migration permits returning DONE tasks to IN_PROGRESS, so reopening does not require a new migration. Existing backend assignments and coordinates are retained.

## Tests and submission

- [Requirements checklist](docs/P1_REQUIREMENTS.md)
- [Architecture and design](docs/ARCHITECTURE.md)
- [A2 repository update and feature reconciliation](docs/REPOSITORY_UPDATE.md)
- [Expected and observed test results](docs/TEST_REPORT.md)
- [Ten-minute demonstration guide](docs/DEMO_GUIDE.md)
- **[S24 Ultra update instructions](docs/S24_ULTRA_UPDATE.md)** and **[timed phone recording guide: what to show and say](docs/PHONE_RECORDING_GUIDE.md)**.
- [Earlier reference walkthrough](deliverables/ArenaCompanion-P1-10min-demo.mp4), with [recording notes](docs/VIDEO_WALKTHROUGH.md). It predates 0.5.2; replace it with the student's final phone recording before hand-in.
- Screenshots and build/device-test evidence in `docs`.

To create a clean submission ZIP without local credentials, build caches or emulator files:

```powershell
.\tools\package.ps1 -TeamName G1
```

The result is `deliverables/DTE_2503_26_G1_P1.zip`. Adjust `G1` to your group naming convention. The archive includes the Android Studio project, current APK, README, test documentation and video present in deliverables when packaging runs. The retained video is an earlier emulator reference with synthesized narration. Use the new phone guide to record the final 0.5.2 presentation, export the phone's actual test observations, then regenerate the ZIP so it contains both. GitHub sharing is separate from course submission.

## Known limitations

- The volunteer dataset is a realistic course scenario, not the organizer's actual 2029 assignments or venue plan.
- Only previously browsed OSM tiles are cached. Offline markers and descriptions do not guarantee offline map imagery at every zoom/location.
- GPS can be unavailable, inaccurate or approximate; there is no indoor positioning or route calculation. Tracking and arrival reminders stop when the app activity is not visible. They do not use background location or system push notifications.
- Backend sync runs while the app is active, not as a background service. P1 handles its own queued writes; it does not implement multi-user conflict resolution.
- Remote messages/incident submissions need a connection. Their A2 RPCs do not have idempotency keys, so automatic retries could duplicate submissions; the app does not queue them.
- There are no push notifications, live coordinator dashboard, registration flow or general account editor in this milestone. Inbox updates load on refresh.
- Photo access can be revoked outside the app; choose the file again if it cannot be opened. There is no photo resizing.
- The interface is English. The app has not been validated on every OEM device, screen size or accessibility configuration.
- Walking times are rough straight-line estimates, not pedestrian routes. Demo walk positions must be identified as practice in any presentation.
- The debug build is a course prototype. The retained earlier video does not show 0.5.2; the student still needs to record the final phone video and physical-device results using the provided guide.

## Credits

Event information: [Narvik 2029](https://www.narvik2029.com/). Map data: [© OpenStreetMap contributors](https://www.openstreetmap.org/copyright), rendered with osmdroid. Weather data: [Open-Meteo](https://open-meteo.com/) (CC BY 4.0). QR scanning: ZXing Embedded. The visual assets are code-drawn mountain shapes and Material icons.
