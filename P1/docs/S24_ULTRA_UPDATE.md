# Update Arena Companion on your Galaxy S24 Ultra

Install **ArenaCompanion-v0.5.2-debug.apk** from this project's deliverables folder. It uses application ID `no.dte2503.volunteer.p1`, version code 7 and the same debug signing identity as the previous APK. Install over the existing P1 app to retain your saved tasks, theme and drafts. **Do not uninstall the old app or clear its data as an update step.**

## Transfer and install

1. Unlock your S24 Ultra and connect it to the laptop using a USB data cable.
2. Tap the USB notification on the phone. Choose **Transferring files / Android Auto** or **File transfer** (the wording depends on One UI).
3. On the laptop, press Windows+E. Open `C:\Users\Balint\DET2503-2026G1\P1\deliverables` and copy **ArenaCompanion-v0.5.2-debug.apk**.
4. In File Explorer, open **This PC → your Galaxy S24 Ultra → Internal storage → Download** and paste the APK. The file is about 22 MB.
5. On the phone, open **My Files → Downloads** and tap the new **v0.5.2** APK.
6. If asked, open the installation settings and allow **My Files** to install this APK. Return to the installer and tap **Update / Install**. The installer should identify it as an update to the existing Arena Companion P1 app.
7. Tap **Open**. In the app, go to **You** and scroll to the bottom. Confirm **Arena Companion 0.5.2**. Check that a previous task/draft/theme is still there.

If Samsung specifically reports that **Auto Blocker** is preventing this installation, open **Settings → Security and privacy → Auto Blocker**, temporarily disable it for this known project APK, install the update, then enable it again. Other device protection settings do not need to change. [Samsung installation troubleshooting](https://www.samsung.com/us/support/troubleshoot/TSG10001914/).

If installation reports a package/signature conflict, keep the installed app and capture the exact message before taking further action. A copy built on another teammate's computer can have a different debug signature; deleting the app to work around this would remove local data.

## Check the new features

- **Tasks → Done → Reopen task:** accidental completion returns to In progress.
- **Inbox → My task updates:** newly changed tasks appear in the saved activity history. Earlier versions did not record historical events, so only changes made after this update appear.
- **Map → Locate me:** request location **While using the app**, with **Precise location** if available. The app shows real GPS accuracy and distances. GPS continues on Today/Tasks while the app stays visible.
- **Map → Facilities → Volunteer hub → Watch arrival:** enables a 150 m foreground arrival reminder. When GPS is too imprecise, the app says so instead of claiming arrival.
- **Map → Demo walk:** rehearse Outside hub / Inside hub from anywhere. The persistent Practice position label distinguishes this from real GPS. **Use GPS** returns to the phone's actual position.
- **You → Saved data and connection:** inspect demo/local storage or real-account pending writes and last successful refresh.
- **You → Check this device:** perform at least five scenarios, record your own observations and export the report.

## Prepare screen recording

Open the S24 Ultra's Quick settings panel (on recent One UI, swipe down from the top right; older layouts use two downward swipes). Find **Screen recorder**. Add it using the panel's edit controls if needed.

Long-press **Screen recorder** to select **1080p**, **Media and mic**, and **Show taps and touches**. Record in portrait. Set a comfortable fixed brightness and use Do not disturb so notifications do not interrupt your explanation. Make a 10-second microphone test first and play it back.

Open Arena Companion, start **Screen recorder**, select **Media and mic**, and start after the countdown. Use the [timed phone recording guide](PHONE_RECORDING_GUIDE.md). Stop recording using Samsung's recorder control; the video is available in **Gallery** (or the configured screen-recordings folder in My Files). [Samsung screen recorder instructions](https://www.samsung.com/us/support/answer/ANS10001616/).

## After recording

Watch the complete recording with sound. Confirm the map labels and expected/observed results are readable and the recording is approximately ten minutes. Copy your final MP4 to `P1/deliverables/ArenaCompanion-P1-10min-demo.mp4`, replacing the earlier reference recording only after keeping a backup if you want it. Export your actual device report from the app to a place you can transfer from, then save it under `P1/docs/test-evidence/S24-Ultra-manual-results.txt`.

Recreate the submission ZIP after replacing the video or adding your phone report:

```powershell
cd C:\Users\Balint\DET2503-2026G1\P1
.\tools\package.ps1 -TeamName G1
```

This refreshes the archive with the final video and observations. The APK update does not automatically update a video already recorded on an earlier version.

References: [Android USB file transfer](https://support.google.com/android/answer/9064445?hl=en), [Android APK installation permission](https://developer.android.com/distribute/marketing-tools/alternative-distribution#user-opt-in-for-installing-unknown-apps), [Android signing and app updates](https://developer.android.com/studio/publish/app-signing).
