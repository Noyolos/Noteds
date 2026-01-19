# TODO Backlog

## P1 (high)
- Docs mismatch for DB and backup versions: repro by comparing `MAINTENANCE.md` (schema version 5, backup version 2) with `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` (`version = 6`) and `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` (`backupVersion = 3`); impact is engineers applying incorrect migration or backup assumptions; fix by updating `MAINTENANCE.md` and adding a brief changelog in `docs/DATA_MODEL.md`.
- Navigation stack not preserved across configuration changes: repro by rotating the device while in `Screen.CustomerDetail` and observing reset to `Screen.Main`; evidence is `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `screenStack` stored with `remember` only; impact is user context loss; fix by using `rememberSaveable` with a custom saver or migrating to Navigation Compose.
- Dual backup formats coexist: repro by inspecting `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `exportBackup`/`importBackup` (JSON `BackupData`) versus `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` (zip + `backupVersion`); impact is confusion and risk of stale logic; fix by consolidating to one format and deprecating the other.

## P2 (medium)
- Theme customization is defined but not applied: repro by noticing `app/src/main/java/com/example/noteds/ui/theme/Theme.kt` `NotedsTheme` is never called in `app/src/main/java/com/example/noteds/MainActivity.kt` (uses `MaterialTheme` directly); impact is inconsistent colors/status bar behavior; fix by wrapping `AppRoot` with `NotedsTheme`.
- Backup restore logs include customer names: repro in `app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt` `Log.d("BackupRestore", ...)` with `it.name`; impact is potential PII exposure in logcat; fix by removing names or logging counts only.
