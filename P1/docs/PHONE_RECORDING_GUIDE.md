# Ten-minute P1 recording — Galaxy S24 Ultra, app 0.5.2

Use this as a speaking guide, not a claim that untested features passed. The second teacher screenshot asks for device/emulator testing of **navigation, map, location, permissions, local storage, synchronization and offline behavior**, with **at least five expected and observed scenarios**. The sequence below deliberately covers those areas.

## Before pressing Record

1. Follow [S24 Ultra update instructions](S24_ULTRA_UPDATE.md). Confirm **You → Arena Companion 0.5.2**. Keep your existing app data.
2. Use the **Narvik demo** for the coherent Narvik presentation. Demo messages/check-ins/reports are local. Existing team accounts use their own server assignments, which may differ. Test account login before deciding to demonstrate it; never display a password while recording.
3. With internet on, open Today and refresh weather. Browse the Narvik map at the zooms you will show so those tiles have a chance to cache. OSM does not supply an offline map package here.
4. Rehearse the sequence once. Pick a task you can start/complete/reopen. If the task is already complete, reopen it first. Existing data is preserved; no reset is required.
5. Go to **You → Check this device**. Actually run at least five scenarios and record the observed result for each. For example: navigation, task undo, map interaction, permission recovery, and offline restart. Add appearance/draft and proximity checks if you perform them. Leave real-account sync **Not run** unless you test it with a real account. A failed test should be recorded as Issue, not Pass.
6. For permission denial: practise opening the map's Settings shortcut or Samsung **Settings → Apps → Arena Companion → Permissions → Location**. Choose the P1 app if A2 is also installed. Denying permission can restart the process; return to the app normally. Do not clear app data.
7. For offline mode: use Quick settings to turn off both Wi-Fi and mobile data. Airplane mode alone can leave Wi-Fi on. For a strong persistence check, open Samsung **Settings → Apps → Arena Companion → Force stop → OK**, then reopen the app. The recorder can keep running. Force stop closes the process; do not use Clear data or uninstall.
8. Set Samsung Screen recorder to portrait, 1080p, Media and mic and visible touches. Use Do not disturb. Have this guide on your laptop so it is not part of the phone recording.

Suggested observed-result wording is a model to replace with what you actually see: “Expected: the reopened task should return to Open. Observed: Restock the volunteer hub returned as In progress, and the change remained after reopening the application.” Do not copy it into the test log unless it happened.

## 00:00–00:45 — Purpose, app version and navigation

**Show:** the welcome screen's existing-account and demo options, then enter the demo. If a saved session opens directly, briefly use You → Leave demo first. Show the five navigation destinations with deliberate taps. Avoid spending time entering credentials.

**Say:**

“This is Arena Companion, our P1 Android application for volunteers at the 2029 Alpine World Ski Championships in Narvik. It extends our A2 project with a consistent Arctic blue interface, persistent data and useful location features. I am showing version 0.5.2 on a Samsung Galaxy S24 Ultra. Existing team accounts can sign in, and this clearly labelled demo lets us rehearse the volunteer workflows. Today, Tasks, Map, Inbox and You are the five main destinations. The volunteers, assignments and meeting points in the demo are fictional.”

## 00:45–01:25 — Today and real external data

**Show:** shift time/meeting point, completion progress, Up next, current Narvik weather with its update timestamp and attribution. Tap refresh once if necessary. Read the actual weather visible on your phone rather than a number from this guide.

**Say:**

“Today brings together the volunteer's shift, meeting point, task progress and next assignment. The weather comes from Open-Meteo and is cached locally with the time it was fetched. This is current weather in Narvik, rather than a forecast for the event in 2029. The app also suggests an appropriate extra layer based on the current conditions. Open-Meteo and OpenStreetMap are real external integrations that work independently of account login.”

## 01:25–02:30 — Tasks, undo and task updates

**Show:** Tasks → Open. Start a To do task, complete an In progress task, open Done and **Reopen task**. Return to Open to show In progress. Then Inbox → My task updates to show the transitions. If using an initially In progress task, explain that it skips the Start step.

**Say:**

“Tasks contain the assignment details, venue point, time and status. The filters let me separate unfinished and completed work. Starting a task changes it to In progress; completing it moves it to Done and updates the progress count. If I tap the wrong task accidentally, Reopen task returns it to In progress. Here is the same task under Open again. The task activity records my changes on this phone. In a real account, the final task status also uses the existing backend service; demo changes stay local.”

**State a test:** “Expected: reopening returns the task to Open. Observed: [name the task and the actual status you see].”

## 02:30–03:20 — OpenStreetMap and marker interaction

**Show:** Tasks → View map for an assignment. Pan and pinch to zoom. Tap another marker. Switch to Facilities, select Volunteer hub and show the selected card. Point out numbered task/check markers, lettered facilities and the OpenStreetMap attribution.

**Say:**

“The map uses OpenStreetMap through osmdroid. Selecting a task opens its point and details, and the map supports pan, zoom and marker selection. Task numbers and completed markers are visually different from square facility markers, so the map is useful without relying on colour alone. Facilities show meeting and support points. This task-to-map navigation is one of the device test scenarios.”

## 03:20–04:10 — Real phone location and permission handling

**Show:** leave practice mode with Use GPS if it is active. Tap Locate me, deny location if prompted, and show that markers remain. Allow location through Settings, return and tap Locate me with While using the app/Precise selected. Show the actual GPS accuracy/position. You are outside Narvik, so real GPS may be far from the Narvik points; that is expected.

**Say:**

“Location access is requested when I ask to locate myself. After denial the map keeps the saved task markers and gives a recovery path, rather than inventing my position. After allowing it, this is the S24 Ultra's real position and reported accuracy. I am not physically in Narvik, so the real distances can be large. Location updates continue across the app while it is visible and stop when it goes into the background. Stale fixes are cleared. Approximate permission is supported, but it cannot reliably confirm a small arrival zone.”

**If the permission was already granted:** show the Settings change rather than claiming a permission dialog appeared. A slow indoor fix is not a pass: record the actual outcome and use an outdoor fix before the final take if necessary.

## 04:10–05:05 — Proximity, walking estimates and arrival geofencing

**Show:** Map → Facilities → Volunteer hub → Watch arrival. Tap Demo walk → Outside hub, then Demo walk → Inside hub. Show the 150 m circle, the Practice position banner and arrival notice/status. Switch to Today and scroll to Closest unfinished task, then Tasks/Nearest if time permits.

**Say:**

“To rehearse a Narvik walk from here, I am deliberately switching to labelled practice positions. These are not the phone's GPS. The arrival reminder watches a 150-metre circle while the app is open. Moving inside confirms arrival; repeated fixes inside do not repeatedly alert. A wider exit boundary helps avoid GPS jitter. The same position updates nearest-task ordering and meeting-point distances. Walking times are approximate estimates at four kilometres per hour from straight-line distance; hills, snow and restricted access can make a real walk longer. This does not claim background tracking or routed navigation.”

## 05:05–06:00 — Announcements and persistent coordinator drafts

**Show:** Inbox → Team updates, open/read an announcement. Tasks → Contact coordinator. Type “Please confirm the meeting point.” Leave the conversation and reopen it to show the draft. Send it, then show that the draft cleared and the message appeared.

**Say:**

“The announcement board keeps briefings and priority messages together, and read state is saved. Each coordinator conversation belongs to a particular task. An unfinished message is saved per task and account, so I can leave the screen and continue later. It is only cleared after sending succeeds. This is a practice conversation saved on the phone. In a real account, messages go through the existing service and require internet.”

## 06:00–07:30 — Offline updates, restart and storage

**Show:** turn off Wi-Fi and mobile data. Show the Offline banner. Change a task status and leave a distinct message draft, for example “Offline draft: where is the radio pickup?”. Use Samsung Settings → Apps → Arena Companion → Force stop, then reopen. Show the saved task status, the restored draft and the weather's Saved reading label. Visit Map: cached tiles may appear, while points and calculations remain available. Practice positions must remain labelled if you re-enable Demo walk after the restart.

**Say:**

“I am switching off both network connections. The app now shows that it is offline. Task changes and drafts commit locally before the interface treats them as saved. I will force-stop and reopen the app to test persistence beyond the running process. The saved task status is still [read the actual status], and this is the unfinished message I entered before closing. The weather is explicitly labelled as a saved reading. Previously browsed tiles may be cached, but new map areas cannot be guaranteed without internet. The saved markers, assignments, GPS distance calculations and local demo operations remain useful.”

**State a test:** “Expected: the task change and draft survive an offline restart. Observed: [describe what the phone actually restored].”

## 07:30–08:10 — Reconnect and synchronization, honestly demonstrated

**Show:** reconnect internet; open You → Saved data and connection. There are two valid recordings:

- **If a real account is tested and available:** while signed in, perform an offline task/read change, show the waiting count/task label, reconnect and tap Refresh and retry sync. Show the count returning to zero and last successful account refresh. This can be a short separately recorded segment if changing accounts would consume the previous chapter. Keep it clearly labelled and do not show credentials.
- **If demonstrating demo only:** show Saved on this phone and explain the real-account sync tests as controlled test evidence. Do not describe a local demo save as server synchronization, or mark the real-account sync scenario passed.

**Say for the real-account case:** “The offline task update is waiting in this account's persistent outbox. On reconnect the repository retries it, fetches the service snapshot and clears the pending count after success. Repeated changes to one task queue its latest status. Failed writes stay queued, and another account cannot receive this account's pending changes.”

**Say for the demo case:** “The demo correctly states that its changes are local. The real-account repository has a persistent task/read outbox and retry indicators. Its failure/reconnect and account-isolation behavior is covered by the controlled automated tests in our test report. I am not claiming that this local demo has synchronized with a server.”

## 08:10–09:00 — Check-in and incident draft

**Show:** top-bar checkpoint button → Try the demo checkpoint twice, showing duplicate protection. Open Report issue, type “Missing direction sign at the entrance”, leave/reopen to show the saved draft, then save the demo report. A photo is optional; only include it if you rehearsed the picker on your S24 Ultra.

**Say:**

“The on-site workflows continue our A2 functionality. Check-in validates the checkpoint and prevents recording the same demo arrival twice. The incident form validates the description and can associate a saved place or task. A draft survives leaving the form, and an optional photo can help a coordinator locate a problem. These demo confirmations and reports stay on this device. Remote check-ins and reports need a connection; they are not automatically replayed because duplicate submissions would be undesirable.”

## 09:00–09:35 — Dark mode and expected/observed evidence

**Show:** You → Dark appearance, then Check this device. Show your genuinely completed scenarios and open one saved expected/observed entry. Tap Export test report and show the share chooser if useful; do not spend time sending a message during the video.

**Say:**

“Dark appearance is saved between launches. This device log records each scenario, its expected result and what I actually observed. I documented [name five tests you performed]. Untested checks remain Not run. Export includes this S24 Ultra and the app version, so these observations complement the automated results.”

## 09:35–10:00 — Architecture, test evidence and delivery

**Show:** device-log summary or Today, with 0.5.2 visible earlier in the recording. Finish calmly rather than adding fast taps.

**Say:**

“The application uses a repository and ViewModel with StateFlow, coroutines and local AtomicFile storage. All 31 unit tests and 14 emulator tests passed. The submission includes the source, APK, requirements audit and test report. Foreground arrival reminders, estimated walking times and offline map limits are documented.”

## Final checks and submission

Play the recording all the way through. Check your voice, readable labels, real GPS versus practice labels, actual offline restart and your expected/observed results. Use the exact automated counts from the final test report if you choose to mention numbers; do not reuse counts from the older reference video.

Export your phone observations and save them in `docs/test-evidence/S24-Ultra-manual-results.txt`. Replace the older reference MP4 with this final recording in `deliverables/ArenaCompanion-P1-10min-demo.mp4`. Recreate the submission ZIP using `tools/package.ps1 -TeamName G1` after adding both files. Upload the course submission through the teacher's required channel; GitHub sharing alone is not the course hand-in.
