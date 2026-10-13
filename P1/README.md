# Arena Companion v0.5 — P1

An Android volunteer companion for **Narvik 2029, the FIS Alpine World Ski Championships**. P1 extends the A2 application with a consistent Arctic blue interface, persistent local data, foreground GPS, proximity features and cached current weather.

The current patch build is **0.5.1**. It adds **Reopen task** for accidental completions. The APK supplied on this computer is configured for the existing A2 Supabase project, with account sign-in and demo access available together. Volunteer passwords and accounts were not changed.

Open **this `P1` directory** as the Android Studio project. A2 stays in its original directory. P1 uses the application ID `no.dte2503.volunteer.p1`, so both versions can be installed together.

## Try the app

Run the app and tap **Explore the volunteer demo**. No password or API key is required. The demo contains a volunteer profile, five tasks, a shift, Narvik meeting points, announcements and coordinator conversations. Demo shifts, people and operations are fictional; coordinates are approximate.

- **Today:** shift summary, completion progress, current Narvik weather, next task and closest unfinished task after a GPS fix.
- **Tasks:** All/Open/Done filters, task progression, **Reopen task** to undo a completion, map links and coordinator conversations.
- **Map:** OpenStreetMap, selectable task/facility points, current position, nearest-first lists and straight-line distances.
- **Inbox:** announcements, persistent read state, task-linked conversations and unfinished messages saved per task and volunteer.
- **You:** volunteer roles, work access, offline information and saved system/light/dark appearance.
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

## Emulator location and permissions

Use the emulator's Extended Controls → Location and set **latitude 68.4384, longitude 17.4272**. Open Map and tap **Locate me**. Choose precise or approximate location. The app remains useful if permission is denied or device location is off. With Android's device bridge:

```text
adb emu geo fix 17.4272 68.4384
```

The command order is longitude, then latitude. GPS accuracy is shown and distances are explicitly straight-line. The app does not track in the background or send the volunteer's GPS position to a server.

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
- [Recorded ten-minute walkthrough](deliverables/ArenaCompanion-P1-10min-demo.mp4), with [recording notes](docs/VIDEO_WALKTHROUGH.md).
- Screenshots and build/device-test evidence in `docs`.

To create a clean submission ZIP without local credentials, build caches or emulator files:

```powershell
.\tools\package.ps1 -TeamName G1
```

The result is `deliverables/DTE_2503_26_G1_P1.zip`. Adjust `G1` to your actual group naming convention. The archive includes the Android Studio project, debug APK, README, test documentation and recorded walkthrough. Review the video before handing it in. Its narration is synthesized; use the guide to record your own explanation if your group or teacher prefers a student presentation.

## Known limitations

- The volunteer dataset is a realistic course scenario, not the organizer's actual 2029 assignments or venue plan.
- Only previously browsed OSM tiles are cached. Offline markers and descriptions do not guarantee offline map imagery at every zoom/location.
- GPS can be unavailable, inaccurate or approximate; there is no indoor positioning or route calculation. Tracking stops when the map is not visible.
- Backend sync runs while the app is active, not as a background service. P1 handles its own queued writes; it does not implement multi-user conflict resolution.
- Remote messages/incident submissions need a connection. Their A2 RPCs do not have idempotency keys, so automatic retries could duplicate submissions; the app does not queue them.
- There are no push notifications, live coordinator dashboard, registration flow or general account editor in this milestone. Inbox updates load on refresh.
- Photo access can be revoked outside the app; choose the file again if it cannot be opened. There is no photo resizing.
- The interface is English. The app has not been validated on every OEM device, screen size or accessibility configuration.
- The debug build is a course prototype, not a production deployment. The included video uses synthesized narration and recorded emulator footage; review it before submission.

## Credits

Event information: [Narvik 2029](https://www.narvik2029.com/). Map data: [© OpenStreetMap contributors](https://www.openstreetmap.org/copyright), rendered with osmdroid. Weather data: [Open-Meteo](https://open-meteo.com/) (CC BY 4.0). QR scanning: ZXing Embedded. The visual assets are code-drawn mountain shapes and Material icons.
