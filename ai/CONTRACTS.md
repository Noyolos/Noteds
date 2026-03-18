# Contracts (Do Not Break)

## Source of Truth Rule (Hard Requirement)
- All project knowledge must live under `ai/`.
- `ai/` is the single source of truth for project details.
- Remove duplicated or overlapping project-detail text outside `ai/`.

## API / external interfaces
- No HTTP API. Do not add network endpoints without updating `ai/PROJECT.md` and this file, and adding required permissions in `app/src/main/AndroidManifest.xml`.
- External file interface is the backup zip format described below.

## Routes / navigation
- Route model is the `Screen` sealed class in `app/src/main/java/com/example/noteds/ui/AppRoot.kt` and must keep these screens and behaviors:
  - `Screen.Main` (bottom tabs: Dashboard, Customers, Reports).
  - `Screen.GroupList(groupCustomerId: Long)`.
  - `Screen.AddCustomer(parentId: Long?)`.
  - `Screen.CustomerDetail(customerId: Long)`.
  - `Screen.EditCustomer(customerId: Long)`.
- Back behavior must continue to pop the manual stack in `AppRoot`.

## UI behavior invariants
- Bottom tab selection must keep the three main tabs wired in `AppRoot`.
- Customer group navigation must keep `isGroup` + `parentId` semantics for folder lists.
- Backup export/import must remain accessible from `ReportsScreen`.
- Legacy `idCardPhotoUri` data must remain editable without being silently remapped into `passportPhotoUri*` slots.

## Data schema invariants (Room)
- `customers` table maps to `app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt`:
  - Required: `name`, `phone`, `note` are non-null strings.
  - Keys: `id` is auto-generated `Long`.
  - Soft delete: `isDeleted` must remain the filter for queries.
  - Grouping: `parentId` forms hierarchy, `isGroup` marks folder rows.
  - Photo paths: `profilePhotoUri*`, `passportPhotoUri*`, `idCardPhotoUri` are nullable strings.
- `ledger_entries` table maps to `app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt`:
  - Required: `customerId`, `type`, `amount`, `timestamp`.
  - `type` must be `DEBT` or `PAYMENT` (`app/src/main/java/com/example/noteds/data/model/TransactionType.kt`).
  - `amount` must be > 0 (validated in `CustomerViewModel.addLedgerEntry`).
  - `customerId` must reference an existing customer.

## Persistence rules
- Room DB name is `noteds-db` (set in `AppContainer`).
- Photo files are stored in `filesDir/customer_photos` and must be cleaned on delete and restored on import.
- `FileProvider` authority is `${applicationId}.fileprovider` with paths in `app/src/main/res/xml/file_paths.xml`.
- Backup import must not delete the existing `customer_photos` directory until restored photos are staged and ready to replace it.

## Migration policy
- Never use destructive migrations.
- Any entity change requires:
  - A new `Migration` in `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt`.
  - Registration in `app/src/main/java/com/example/noteds/di/AppContainer.kt`.
  - Backup format updates in `ReportsViewModel.buildBackupJson`/`parseBackupJson`.

## Backup file contract
- Format: zip containing `data.json` and optional `photos/` directory.
- `data.json` fields (from `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt`):
  - Root: `backupVersion` (int), `customers` (array), `ledgerEntries` (array).
  - Customer fields: `id` (long), `code` (string), `name` (string), `phone` (string), `note` (string),
    `profilePhotoUri`, `profilePhotoUri2`, `profilePhotoUri3`, `idCardPhotoUri`, `passportPhotoUri`,
    `passportPhotoUri2`, `passportPhotoUri3`, `expectedRepaymentDate` (long|null),
    `initialTransactionDone` (bool), `isDeleted` (bool), `isGroup` (bool), `parentId` (long|null).
  - Ledger fields: `id` (long), `customerId` (long), `type` (string), `amount` (double),
    `timestamp` (long), `note` (string|null).
- `photos/` entries are relative paths referenced by the `*PhotoUri` fields in `data.json`.

## Endpoints, DOM IDs, shaders
- Endpoints: none (offline app).
- DOM IDs/selectors: none (Jetpack Compose UI).
- Shaders: none defined. TODO: document here if introduced.

## Do-not-touch zones (unless required)
- `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` migrations and schema helpers.
- `app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt` replace-all transaction logic.
- `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` backup export/import.
- `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` delete and photo persistence/cleanup.
- `app/src/main/java/com/example/noteds/ui/AppRoot.kt` navigation stack.

## Non-goals
- No unrelated refactors or formatting-only churn.
- No renaming of entities, routes, or backup fields unless explicitly required and documented.
