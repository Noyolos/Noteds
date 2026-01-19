# Project Overview

## One-line purpose
- Noteds is an offline Android app that tracks customers and their debt/payment ledger locally.
- Evidence: `app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt` `CustomerEntity` stores customer records; `app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt` `LedgerEntryEntity` stores ledger rows; UI list is rendered in `app/src/main/java/com/example/noteds/ui/customers/CustomerDetailScreen.kt` `CustomerDetailScreen`.

## Primary users and value
- Target user is a single-device operator who needs quick balances, customer history, and local backup/restore.
- Evidence: `app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt` `DashboardScreen` surfaces totals and top debtors; `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt` `ReportsScreen` provides export/import actions.

## High-level architecture diagram
```text
Android OS
  -> Application: NotedsApp
      -> AppContainer (DI)
          -> Room AppDatabase (customers, ledger_entries)
  -> MainActivity
      -> Compose UI root: AppRoot
          -> CustomerViewModel -> CustomerRepository/LedgerRepository -> DAOs
          -> ReportsViewModel  -> CustomerRepository/LedgerRepository -> DAOs
          -> BackupRepository  -> AppDatabase transaction + DAO writes
          -> File storage (filesDir/customer_photos, cacheDir)
          -> SAF/MediaStore/FileProvider
```
- Evidence: `app/src/main/AndroidManifest.xml` declares `.NotedsApp` and `.MainActivity`; `app/src/main/java/com/example/noteds/NotedsApp.kt` `NotedsApp.onCreate` builds `AppContainer`; `app/src/main/java/com/example/noteds/MainActivity.kt` `MainActivity.onCreate` calls `AppRoot`; `app/src/main/java/com/example/noteds/di/AppContainer.kt` wires Room and repositories.

## Quick commands
- Build debug APK: `./gradlew :app:assembleDebug`.
- Run unit tests: `./gradlew :app:test`.
- Run instrumented tests (device/emulator): `./gradlew :app:connectedAndroidTest`.
- Evidence: Gradle wrapper present at `gradlew` and module config at `app/build.gradle.kts` with Android plugins and test dependencies.

## Key folders map
- `app/src/main/java/com/example/noteds/` contains app code (UI, data, DI); evidence: `app/src/main/java/com/example/noteds/ui/AppRoot.kt`, `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt`.
- `app/src/main/res/` contains resources and manifest config; evidence: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`.
- `app/src/test/` and `app/src/androidTest/` contain unit and instrumented tests; evidence: `app/src/test/java/com/example/noteds/ReportsLogicTest.kt`, `app/src/androidTest/java/com/example/noteds/DatabasePressureTest.kt`.
- `gradle/` and root `build.gradle.kts` define build tooling; evidence: `gradle/wrapper/gradle-wrapper.properties`, `build.gradle.kts`.

## Change safely rules summary
- Keep data migrations in sync with Room entities and register them in DI; evidence: `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` migrations and `app/src/main/java/com/example/noteds/di/AppContainer.kt` `.addMigrations(...)`.
- Treat backup import/export as a strict contract; evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `exportBackup` and `importBackup`.
- Preserve navigation flow in `AppRoot` and `Screen` sealed class; evidence: `app/src/main/java/com/example/noteds/ui/AppRoot.kt`.
- Keep soft-delete semantics intact; evidence: `app/src/main/java/com/example/noteds/data/dao/CustomerDao.kt` filters `isDeleted = 0` and `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` sets `isDeleted = true`.
- Avoid touching photo storage paths without updating backup/restore; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `persistPhoto`, `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `restorePhoto`.
