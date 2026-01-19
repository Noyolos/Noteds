# Features

## Dashboard and insights
- Dashboard totals and trend: UI entry is `AppRoot` main tab index 0 -> `app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt` `DashboardScreen`; code reads `ReportsViewModel.totalDebt`, `debtThisMonth`, `repaymentThisMonth`, and `totalDebtTrend` from `app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt`; data source is `LedgerRepository.getAllEntries`; edge cases: empty data yields zeros and empty chart lists due to `StateFlow` `initialValue` defaults.
- Top debtors list: UI entry is `DashboardScreen` `TopDebtorItem` rows; clicking calls `onCustomerClick` which `AppRoot` maps to `Screen.CustomerDetail`; data comes from `ReportsViewModel.topDebtors` derived from ledger balances; edge cases: empty list shows no rows.

## Customer and group management
- Customer list and search: UI entry is `AppRoot` main tab index 1 -> `app/src/main/java/com/example/noteds/ui/customers/CustomerListScreen.kt` `CustomerListScreen`; code filters `CustomerViewModel.getCustomers(parentId)` plus search query; data reads `CustomerRepository.getCustomersByParent` and `CustomerDao.getRootCustomersWithBalance`/`getSubordinatesWithBalance`; edge cases: `parentId` null shows root list, `parentId` not null shows group contents.
- Add customer and optional initial debt: UI entry is `AppRoot` -> `Screen.AddCustomer` -> `app/src/main/java/com/example/noteds/ui/customers/AddCustomerScreen.kt` `AddCustomerScreen`; submit calls `CustomerViewModel.addCustomer`, which writes `CustomerEntity` and optionally inserts `LedgerEntryEntity`; edge cases: blank name or invalid amount blocks save in UI, and zero/negative amounts are ignored in `CustomerViewModel`.
- Edit and soft delete customer: UI entry is `Screen.EditCustomer` -> `app/src/main/java/com/example/noteds/ui/customers/EditCustomerScreen.kt` `EditCustomerScreen`; updates call `CustomerViewModel.updateCustomer` and delete calls `CustomerViewModel.deleteCustomer`; data writes update `CustomerEntity` fields and set `isDeleted=true` with photo cleanup; edge cases: deleted customers are filtered from DAO queries (`CustomerDao.getAllCustomers`).
- Group/folder support and moves: UI entry is `CustomerListScreen` folder creation and long-press move actions; code calls `CustomerViewModel.createFolder` (`isGroup=true`) and `CustomerViewModel.moveCustomer` (`parentId` change); data reads group lists via `CustomerViewModel.allFolders` and `CustomerDao.getSubordinatesWithBalance`; edge case: move to self is blocked in `moveCustomer`.

## Transactions (ledger)
- Add debt or payment: UI entry is `CustomerDetailScreen` -> `TransactionFormScreen`; `TransactionFormScreen` calls `CustomerViewModel.addLedgerEntry` with type and amount; data writes `LedgerEntryEntity` via `LedgerRepository.insertEntry`; edge cases: amount must be > 0 and `TransactionType` is normalized on update.
- Edit or delete transaction: UI entry is long-press on a transaction row in `CustomerDetailScreen` -> `EditTransactionDialog`; update calls `CustomerViewModel.updateLedgerEntry`, delete calls `CustomerViewModel.deleteLedgerEntry`; data writes update or delete in `LedgerDao` and UI refreshes via `Flow` from `getEntriesForCustomer`.

## Reports and analytics
- Monthly stats, aging, and averages: UI entry is `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt` `ReportsScreen`; data comes from `ReportsViewModel.last6MonthsStats`, `agingStats`, `averageCollectionPeriod`, and `totalTransactions` built from `LedgerRepository.getAllEntries`; edge cases: empty lists return no chart rendering and averages return 0.

## Backup and restore
- Backup export (zip): UI entry is `ReportsScreen` export button; data flow is `ReportsViewModel.exportBackup` -> `buildBackupJson` -> zip write; includes `data.json` and `photos/` in a SAF-selected destination; edge cases: IO failures return error messages and clean temp directory.
- Backup import (zip or JSON): UI entry is `ReportsScreen` import button; data flow is `ReportsViewModel.importBackup` -> `parseBackupJson` -> `BackupRepository.replaceAllData` transaction; edge cases: empty or corrupted files return user-friendly errors and avoid partial writes.
- Legacy JSON export/import exists in `CustomerViewModel.exportBackup`/`importBackup` using `BackupData`; if used, it writes a plain JSON file with customers and entries and no photo packaging.

## Photo management
- Capture and select photos: UI entry is `CustomerPhotoPicker`/`PhotoGrid` in add/edit screens; code uses `FileProvider` and SAF gallery pickers to return URIs, then `CustomerViewModel.persistPhoto` copies into app storage; edge cases: missing permissions or IO failure results in null or unchanged paths.
- View and save photos: UI entry is `FullScreenImageDialog` in `CustomerDetailScreen`; save calls `CustomerViewModel.saveImageToGallery` which writes to `MediaStore`; edge cases: save failures show a toast and do not crash.
