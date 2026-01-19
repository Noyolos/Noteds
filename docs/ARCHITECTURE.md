# Architecture

## Runtime architecture
- Android entry points are declared in `app/src/main/AndroidManifest.xml` (`application` name `.NotedsApp`, `activity` `.MainActivity`), which bootstraps `app/src/main/java/com/example/noteds/NotedsApp.kt` `NotedsApp.onCreate` and `app/src/main/java/com/example/noteds/MainActivity.kt` `MainActivity.onCreate`.
- Compose UI layer is rooted at `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `AppRoot`, which owns a `Screen` stack and renders `DashboardScreen`, `CustomerListScreen`, and `ReportsScreen`; it connects UI events to `CustomerViewModel` and `ReportsViewModel` passed from `MainActivity`.
- Data layer is Room-based: `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` `AppDatabase` exposes `CustomerDao` and `LedgerDao`, wrapped by `CustomerRepository` and `LedgerRepository` in `app/src/main/java/com/example/noteds/data/repository/*.kt` and wired in `app/src/main/java/com/example/noteds/di/AppContainer.kt`.
- File storage layer is local-only: photos are copied to `filesDir/customer_photos` in `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `persistPhoto`, and backup temp files are under `cacheDir` in `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `exportBackup`/`importBackup`.
- External services are Android system components (SAF, MediaStore, FileProvider); evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt` uses `ActivityResultContracts.CreateDocument`/`GetContent`, `app/src/main/java/com/example/noteds/ui/customers/CustomerPhotoPicker.kt` uses `FileProvider`, and `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` uses `MediaStore`.

## Critical workflows

### App launch and DI
- `app/src/main/AndroidManifest.xml` launches `.MainActivity`, which builds `CustomerViewModel` and `ReportsViewModel` from `AppContainer`; call chain: `MainActivity.onCreate` -> `AppRoot` -> screens.
- `app/src/main/java/com/example/noteds/NotedsApp.kt` `NotedsApp.onCreate` builds `AppContainer`, which constructs Room `AppDatabase` and repositories used by view models in `MainActivity`.

### Add customer (optional initial debt)
- UI entry: `AppRoot` routes `Screen.AddCustomer` to `app/src/main/java/com/example/noteds/ui/customers/AddCustomerScreen.kt` `AddCustomerScreen`.
- Validation and submit: `AddCustomerScreen` calls `customerViewModel.addCustomer` after basic input checks; data flow: `AddCustomerScreen` -> `CustomerViewModel.addCustomer` -> `CustomerRepository.insertCustomer` -> `CustomerDao.insertCustomer`.
- Initial debt: `CustomerViewModel.addCustomer` inserts a `LedgerEntryEntity` when `initialDebtAmount` is present; call chain: `CustomerViewModel.addCustomer` -> `LedgerRepository.insertEntry` -> `LedgerDao.insertEntry`.

### Add ledger entry
- UI entry: `app/src/main/java/com/example/noteds/ui/customers/CustomerDetailScreen.kt` opens `TransactionFormScreen` to capture amount/note/date.
- Save flow: `TransactionFormScreen` calls `CustomerViewModel.addLedgerEntry`, which validates amount and writes a `LedgerEntryEntity` through `LedgerRepository` and `LedgerDao`.
- UI refresh: `CustomerDetailScreen` observes `CustomerViewModel.getTransactionsForCustomer`, which reads `LedgerDao.getEntriesForCustomer` filtered by customer and `isDeleted` join.

### Delete customer (soft delete)
- UI entry: delete actions in `CustomerDetailScreen` and `CustomerListScreen` call `CustomerViewModel.deleteCustomer`.
- Delete flow: `CustomerViewModel.deleteCustomerRecursive` deletes ledger rows (`LedgerRepository.deleteEntriesForCustomer`), removes photo files, and updates `CustomerEntity.isDeleted=true` so DAOs filter it out; evidence: `CustomerDao.getAllCustomers` and `CustomerDao.getCustomersWithBalance` filter `isDeleted = 0`.

### Backup export
- UI entry: `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt` uses `CreateDocument` and calls `ReportsViewModel.exportBackup`.
- Export flow: `ReportsViewModel.exportBackup` builds JSON via `buildBackupJson`, copies photos to a temp `photos/` folder, zips `data.json` and photos, writes to destination, then cleans temp directory.

### Backup import
- UI entry: `ReportsScreen` uses `GetContent` and calls `ReportsViewModel.importBackup`.
- Import flow: `ReportsViewModel.importBackup` reads zip or JSON, parses into `CustomerEntity`/`LedgerEntryEntity`, and calls `BackupRepository.replaceAllData` to replace DB contents inside a Room transaction.
- Data safety: `BackupRepository.replaceAllData` filters out soft-deleted customers and orphaned entries, then resets SQLite auto-increment sequences; evidence: `app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt`.

### Photo capture and persistence
- Capture flow: `CustomerPhotoPicker` and `PhotoGrid` create temp files via `FileProvider` and return a URI string; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerPhotoPicker.kt` `createTempImageUri`.
- Persistence flow: `CustomerViewModel.persistPhoto` copies the image into `filesDir/customer_photos` and stores the file path in `CustomerEntity` fields.

### Save image to gallery
- UI entry: `FullScreenImageDialog` in `CustomerDetailScreen` triggers `CustomerViewModel.saveImageToGallery`.
- Data flow: `saveImageToGallery` writes into `MediaStore.Images` with `RELATIVE_PATH` set to `Pictures/Noteds`; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `copyImageToGallery`.

## State management approach
- `CustomerViewModel` exposes `StateFlow` (`customersWithBalance`, `allFolders`) built with `stateIn`, and UI screens call `collectAsState`; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` and `app/src/main/java/com/example/noteds/ui/customers/CustomerListScreen.kt`.
- `ReportsViewModel` aggregates `Flow` streams from repositories into `StateFlow` for totals and charts; evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `totalDebt`, `topDebtors`, `last6MonthsStats`.
- Navigation state is held in Compose state in `AppRoot` with a custom stack; evidence: `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `screenStack` and `BackHandler`.

## Error handling approach
- Data export/import uses try/catch and returns user-facing messages via callbacks; evidence: `CustomerViewModel.exportBackup`/`importBackup` and `ReportsViewModel.exportBackup`/`importBackup`.
- UI surfaces errors via snackbar or toast; evidence: `ReportsScreen` uses `SnackbarHostState.showSnackbar`, and `CustomerDetailScreen` uses `Toast.makeText` after gallery save.
- Backup import/export logs and continues with partial data where possible (e.g., photo copy failures); evidence: `ReportsViewModel.addPhotoToBackup` and `restorePhoto` return null without failing the whole import.
