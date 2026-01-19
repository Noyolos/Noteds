# AI Project Summary

## Source of Truth Rule (Hard Requirement)
- From now on, all project knowledge (architecture, repo map, entry points, data model, persistence, API contracts, UI behavior, smoke tests, do-not-touch contracts) must live under `ai/`.
- `ai/` is the single source of truth for project details.
- Any duplicated or overlapping project-detail text outside `ai/` must be removed to avoid contradictions.

## One-liner
- Noteds is an offline Android app for tracking customers, debts, and payments with local backup/restore.

## Tech stack
- Kotlin + Android SDK (Jetpack Compose UI).
- Room (SQLite) + KSP for data persistence.
- Coroutines/Flow for async state.
- Gson for backup JSON, Coil for image loading.
- Gradle (wrapper) for builds.

## Architecture (high level)
- App bootstrap: `AndroidManifest.xml` -> `NotedsApp` -> `MainActivity` -> `AppRoot`.
- Data: `AppContainer` builds Room `AppDatabase` with `CustomerDao` and `LedgerDao`.
- State: `CustomerViewModel` and `ReportsViewModel` expose `Flow`/`StateFlow` to Compose UI.

## How to run (local)
- Prereqs: JDK 17, Android SDK Platform 35.
- Env var names (no values): `JAVA_HOME`, `ANDROID_SDK_ROOT` or `ANDROID_HOME`.
- Build: `./gradlew :app:assembleDebug`.
- Install/run on device: `./gradlew :app:installDebug` or launch from Android Studio.
- Tests: `./gradlew :app:test`, `./gradlew :app:connectedAndroidTest`.

## Entry points (mobile)
- App bootstrap chain: `app/src/main/AndroidManifest.xml` -> `app/src/main/java/com/example/noteds/NotedsApp.kt` -> `app/src/main/java/com/example/noteds/MainActivity.kt` -> `app/src/main/java/com/example/noteds/ui/AppRoot.kt`.

## Repo map (key modules)
- Dependency wiring: `app/src/main/java/com/example/noteds/di/AppContainer.kt` (Room DB + repositories).
- Data layer: `app/src/main/java/com/example/noteds/data/` (entities, DAO, DB, repositories).
- Customer UI: `app/src/main/java/com/example/noteds/ui/customers/` (list, add, detail, edit, transactions, photo picker).
- Reports/backup UI: `app/src/main/java/com/example/noteds/ui/reports/` (charts, export/import, analytics).
- Dashboard UI: `app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt`.

## Data model + persistence
- Room DB `noteds-db` stores `customers` and `ledger_entries` (see `CustomerEntity`, `LedgerEntryEntity`).
- Photo files are stored under `filesDir/customer_photos` and referenced by string paths in `CustomerEntity`.
- Backup/restore uses a zip file with `data.json` and optional `photos/` entries.

## UI behavior summary
- Bottom tabs are Dashboard, Customers, Reports; tab selection is managed in `AppRoot`.
- Navigation uses a manual `Screen` stack; back pops the stack.
- Customer groups are folders via `isGroup` and `parentId`; group tap opens `Screen.GroupList`.

## Core flows (2-3)
- Create customer (+ optional initial debt): `AddCustomerScreen` -> `CustomerViewModel.addCustomer` -> `CustomerRepository`/`CustomerDao`, and optional `LedgerRepository.insertEntry`.
- Record debt/payment: `CustomerDetailScreen` -> `TransactionFormScreen` -> `CustomerViewModel.addLedgerEntry` -> `LedgerDao.insertEntry`.
- Backup export/import: `ReportsScreen` -> `ReportsViewModel.exportBackup`/`importBackup` -> `BackupRepository.replaceAllData`.

## Contracts summary (must not break)
- Data schema invariants, backup format, navigation routes, and do-not-touch zones are defined in `ai/CONTRACTS.md`.

## Smoke tests
- Commands: `./gradlew :app:assembleDebug`, `./gradlew :app:test`, `./gradlew :app:connectedAndroidTest`.
- Manual quick check:
  - Launch app and confirm bottom nav tabs load.
  - Add a customer and confirm list/detail shows it.
  - Add a debt and a payment; confirm balance changes.
  - Export and import a backup zip; confirm data restored.
