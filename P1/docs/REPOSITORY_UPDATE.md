# A2 repository update and P1 reconciliation

On 12 October 2026, the repository was updated from A2 commit `1c1efdb` to `6c330738ba654364b37733cd5fcef14286453642` using a fast-forward pull from `origin/main`. The incoming commit changes only A2. P1 remains a separate Android Studio project and keeps its Arctic blue interface, application ID and Narvik scenario.

A clean P1 archive and local configuration were backed up before updating, under the ignored `.tools/repository-update-backups/20261012-212702` directory. Backups and machine-specific configuration are excluded from the submission archive.

## Incoming features and their P1 equivalents

| Latest A2 change | P1 behavior |
|---|---|
| Serializable models and per-user offline snapshots | Serializable `LocalState` and `CachedAccount`, persisted through `AtomicLocalStore`; existing P1 implementation retained |
| Queued task status and announcement-read writes | Existing P1 account-specific outbox, coalesced task updates, identity checks and foreground retry retained |
| Location errors, user-triggered permission request and map focus | Existing P1 lifecycle-bound tracker, Locate me control, provider/permission guidance and map focus retained |
| Cached tiles and offline map indication | Existing P1 network monitor, osmdroid cache, offline map banner, markers and distance lists retained |
| Persistent coordinator-message drafts | Added to P1's repository and file store; drafts survive navigation/restart, stay separate by account/task, and clear after successful sending |
| Queueing messages, check-ins and incident submissions | P1 retains its documented online requirement for these remote operations. The inherited backend does not provide message/incident idempotency keys, so automatic replay can duplicate submissions. Drafts stay available locally; task/read writes sync automatically |

The Supabase adapter, database migrations and backend configuration contract were unchanged by the incoming commit. No live database was modified and no repository changes were pushed to GitHub during this reconciliation.

## Validation

The refreshed build, unit tests, emulator workflow/storage tests and lint results are documented in [TEST_REPORT.md](TEST_REPORT.md). The updated APK and ZIP are in `deliverables`.

- Build passed; 18 unit tests and 8 emulator tests passed.
- Android lint: 0 errors, 33 warnings.
- The saved original P1 archive passed its integrity check. No pre-existing submission file was lost during the pull.
- The tracked A2 working tree matches `origin/main`; local additions remain confined to P1.
