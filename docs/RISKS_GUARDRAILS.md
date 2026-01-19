# Risks and Guardrails

## Do-not-touch zones
- Room migrations and schema helpers in `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` (versioned SQL and table rebuilds) because they gate all existing installs; changes must preserve data and indices and are registered in `app/src/main/java/com/example/noteds/di/AppContainer.kt`.
- Backup import/export pipeline in `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` and `app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt` because it replaces the entire database and updates `sqlite_sequence`.
- Customer delete logic in `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `deleteCustomerRecursive` because it clears ledger entries and photo files while preserving soft-delete semantics in `CustomerDao` queries.
- Navigation stack and routing in `app/src/main/java/com/example/noteds/ui/AppRoot.kt` because all screen transitions and back behavior are custom and not handled by Navigation Compose.
- Photo storage and FileProvider paths in `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `persistPhoto` and `app/src/main/res/xml/file_paths.xml` because backups and gallery exports depend on these locations.

## If you must touch these areas
- Add or change DB fields only after adding a migration in `AppDatabase.kt`, registering it in `AppContainer.kt`, and updating backup JSON in `ReportsViewModel.buildBackupJson`/`parseBackupJson`.
- Modify backup format only with a new `backupVersion` in `ReportsViewModel.buildBackupJson`, and add backward parsing in `parseBackupJson` plus smoke tests in `docs/SMOKE_TESTS.md`.
- Change delete semantics only after validating DAO filters (`CustomerDao.getAllCustomers`, `LedgerDao.getAllEntries`) and updating UI flows in `CustomerDetailScreen` and `CustomerListScreen`.
- Adjust navigation only after tracing `AppRoot` call sites and verifying back behavior in `docs/SMOKE_TESTS.md`.

## Security notes
- Customer data and photos are stored locally and exported in backups; treat backup zips as sensitive (PII + images) because `ReportsViewModel.buildBackupJson` includes names, phone numbers, and photo files.
- No network permission is declared, which reduces exposure; evidence: `app/src/main/AndroidManifest.xml` contains only CAMERA permission.
- Photo exports to shared storage (`Pictures/Noteds`) happen via `CustomerViewModel.saveImageToGallery`; warn users that gallery exports are visible to other apps.

## Performance hotspots and safe tuning
- Report calculations scan all ledger entries multiple times in `ReportsViewModel` (`totalDebt`, `last6MonthsStats`, `agingStats`, `averageCollectionPeriod`); large datasets may cause UI lag if not optimized.
- Customer folder totals compute recursive sums in memory in `CustomerViewModel.getCustomers`; large hierarchies increase cost.
- Backup zip operations read/write potentially large photo files in `ReportsViewModel.exportBackup`/`importBackup`; keep IO on `Dispatchers.IO` and avoid running on main thread.
