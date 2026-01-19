# Data Model

## Storage overview
- Primary storage is a Room database named `noteds-db` created in `app/src/main/java/com/example/noteds/di/AppContainer.kt` `AppContainer` with schema version 6 defined in `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` `AppDatabase`.
- Secondary storage is on-disk photos under `filesDir/customer_photos`, referenced by string paths in `CustomerEntity`; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt` `persistPhoto` and `removeCustomerFiles`.

## Tables and entities
- `customers` table is defined by `app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt` `CustomerEntity`; key fields: `id` (auto primary key), `name`, `phone`, `note` (non-null), `isDeleted` (soft delete), `isGroup` and `parentId` (folder hierarchy), photo URI fields, and `expectedRepaymentDate` (nullable).
- `ledger_entries` table is defined by `app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt` `LedgerEntryEntity`; key fields: `id` (auto primary key), `customerId`, `type` (string, expected values `DEBT` or `PAYMENT`), `amount`, `timestamp`, `note` (nullable).
- Invariants enforced in code: amounts are > 0 in `CustomerViewModel.addLedgerEntry` and `AddCustomerScreen` validation; `TransactionType.fromString` normalizes `LedgerEntryEntity.type` during updates; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt`.

## Relationships and derived models
- `LedgerEntryEntity.customerId` is a foreign key to `CustomerEntity.id` with an index on `customerId`; evidence: `app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt` and `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` creates index in migration.
- Group hierarchy uses `CustomerEntity.parentId` and `isGroup` to represent folders; list queries filter root vs child via `CustomerDao.getRootCustomersWithBalance` and `getSubordinatesWithBalance`.
- `CustomerWithBalance` is a derived model combining customer data with aggregated totals, computed in SQL; evidence: `app/src/main/java/com/example/noteds/data/model/CustomerWithBalance.kt` and `app/src/main/java/com/example/noteds/data/dao/CustomerDao.kt` `getCustomersWithBalance`.

## Migrations and schema evolution
- Migrations are defined in `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` (`MIGRATION_1_5` through `MIGRATION_5_6`) and registered in `app/src/main/java/com/example/noteds/di/AppContainer.kt` via `.addMigrations(...)`.
- Version 5 rebuilds tables to match fields present at that time; version 6 adds `parentId` and `isGroup` by rebuilding `customers` and recreating the `parentId` index; evidence: `AppDatabase.kt` SQL in `MIGRATION_5_6`.

## Backup data contract (file-level)
- Backup JSON schema mirrors `CustomerEntity` and `LedgerEntryEntity` fields; see `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt` `buildBackupJson` and `parseBackupJson` for field names and defaults.
