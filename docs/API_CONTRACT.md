# API Contract

## Backend/API availability
- There is no HTTP backend or network API in this project; the app is offline and only requests `android.permission.CAMERA`.
- Evidence: `app/src/main/AndroidManifest.xml` only declares CAMERA permission; `app/build.gradle.kts` contains no networking client dependencies.

## File-based backup contract (treated as external interface)
- Export format is a zip created via SAF `CreateDocument`, containing `data.json` and optional `photos/` entries; evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `exportBackup` and `buildBackupJson`.
- `data.json` schema includes `backupVersion` (currently 3), `customers` array, and `ledgerEntries` array; customer fields mirror `CustomerEntity` and ledger fields mirror `LedgerEntryEntity`; evidence: `ReportsViewModel.buildBackupJson` and `ReportsViewModel.parseBackupJson`.
- Import accepts zip or legacy JSON-only files and rejects empty/corrupted content with a user message; evidence: `ReportsViewModel.importBackup` and `isZipFile`.

## Auth rules
- No auth; access is local only. Export/import requires user-chosen file URIs via SAF and does not authenticate.
- Evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt` uses `ActivityResultContracts.CreateDocument` and `GetContent`.

## Error signaling and retry behavior
- Backup export/import report success as `(Boolean, String?)` callbacks; UI shows snackbar messages; evidence: `ReportsViewModel.exportBackup`/`importBackup` and `ReportsScreen` snackbar handling.

## Legacy backup JSON contract
- Legacy JSON export/import uses `BackupData` with `version`, `timestamp`, `customers`, and `entries`; evidence: `app/src/main/java/com/example/noteds/data/model/BackupData.kt` and `CustomerViewModel.exportBackup`/`importBackup`.
