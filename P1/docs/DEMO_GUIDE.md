# Ten-minute demonstration guide

**For the final 0.5.2 S24 Ultra recording, use [PHONE_RECORDING_GUIDE.md](PHONE_RECORDING_GUIDE.md).** It includes the exact segment timing, what to show and say, permission/offline/sync checks, and actual expected/observed device results. The older sequence below is retained as background for the earlier emulator reference recording.

Use a fresh demo session on an emulator or phone. Enable internet initially. For a useful local GPS demonstration, set the emulator position to **68.4384, 17.4272** (Narvik town centre). Explain that demo operations are fictional and that the weather is current, not a 2029 forecast.

| Time | Show | Explain |
|---|---|---|
| 0:00–0:45 | Launch screen, enter the volunteer demo | Arena Companion supports volunteers at Narvik 2029. P1 extends A2. Show the Arctic blue design and the explicit demo entry. |
| 0:45–1:45 | Today: shift, progress, weather, next task | Each screen has a practical purpose. Weather comes from a real HTTPS API and is cached with a time. |
| 1:45–3:00 | Tasks: All/Open/Done, start a task, complete it | Task changes are persisted immediately. Open filtering removes completed tasks. Map and coordinator actions relate to the selected task. |
| 3:00–4:45 | View a task on the map, select its point, Locate me, grant location | OSM is a core feature. Your marker differs from task/facility markers. Distances use the current fix; select Facilities and compare nearby points. |
| 4:45–5:45 | Inbox: announcement read state; task coordinator conversation | Announcements and conversations are task-relevant. Send a demo message and explain that demo messages remain local. |
| 5:45–6:45 | On-site check-in; scan the provided demo QR or use the demo checkpoint button twice | The first check-in creates a persistent record. The second returns the existing check-in. Try an unrelated code to demonstrate validation. |
| 6:45–7:45 | Report issue: category, text, location, related task, optional photo | Drafts save while typing. Leave the form and reopen it to show that text persists. Save a demo report. |
| 7:45–8:45 | Disable Wi-Fi/mobile data, reopen app, inspect Tasks/Inbox/Map | Core saved data still works. GPS can work without internet. Only visited map tiles may be cached. Weather is marked as a saved reading. |
| 8:45–9:20 | You: roles, permissions and dark/light appearance | Appearance persists. Explain private file storage and account-isolated backend caches. |
| 9:20–10:00 | Reconnect; briefly show architecture/test report | Automatic foreground refresh restores live weather. Optional backend task/read changes replay safely. Mention test results and limitations honestly. |

## Recording preparation

1. Open the `P1` folder in Android Studio and run the app.
   A printable checkpoint code is included at `docs/demo-checkpoint.png`.
2. Use Android Studio's Running Devices **Record Screen** control or a phone screen recorder. Record your explanation along with the screen, if your recorder supports microphone audio.
3. Keep the text readable. Record a portrait emulator with a visible cursor/touch indicator if helpful.
4. Include the video in the final submission under the teacher's naming convention. Rehearse once and aim for approximately ten minutes.

A recorded walkthrough is included at `deliverables/ArenaCompanion-P1-10min-demo.mp4`. It uses actual emulator footage, a synthetic voice and chapter cards. [Recording notes](VIDEO_WALKTHROUGH.md) explain its scope. Review it before submission, or use this guide to record your own student presentation.

## Backend demonstration, if your group uses its A2 service

Enable the optional Supabase configuration, sign in to a seeded test account, go offline, advance a task, and show the pending-update banner. Reconnect, refresh, and confirm that the update disappears from the outbox and remains on the server. The documented automated sync tests use a controlled backend, not the group's live service. Do not claim a live backend demonstration unless you actually run it.
# Patch 0.5.1 additions

For a new recording of the current build, show the account and demo choices on the welcome screen. If a saved demo opens directly, use **You → Leave demo**. Existing volunteer accounts use **Sign in to your team**; the demo remains available without credentials.

During the task section, complete a task, choose the **Done** filter and tap **Reopen task**. Return to **Open** to show its restored **In progress** status. This also works offline and survives a restart; backend sessions sync the final status on reconnection.

