# Smoke Tests

## Commands
- Build debug: `./gradlew :app:assembleDebug` (expect Gradle success); evidence: `app/build.gradle.kts` Android module.
- Run unit tests: `./gradlew :app:test` (expect `ReportsLogicTest` and `ExampleUnitTest` to pass); evidence: `app/src/test/java/com/example/noteds/`.
- Run instrumented tests: `./gradlew :app:connectedAndroidTest` (requires device/emulator); evidence: `app/src/androidTest/java/com/example/noteds/DatabasePressureTest.kt`.

## Manual checklist (expected results)
- Launch app and verify bottom navigation with 3 tabs (dashboard, customers, reports) loads; evidence: `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `NavigationBar`.
- Open Customers tab and confirm list loads from local DB; evidence: `CustomerListScreen.kt` `collectAsState` on `CustomerViewModel.getCustomers`.
- Create a folder from Customers tab and confirm a folder item appears with a folder icon; evidence: `CustomerListScreen.kt` `CreateFolderDialog` -> `CustomerViewModel.createFolder`.
- Add a customer from Customers tab and confirm return to list and new customer entry visible; evidence: `AddCustomerScreen.kt` save handler -> `CustomerViewModel.addCustomer`.
- Open the customer detail view and confirm balance card and transaction list render; evidence: `CustomerDetailScreen.kt` uses `CustomerViewModel.getTransactionsForCustomer`.
- Add a debt transaction and confirm it appears in the list and balance increases; evidence: `TransactionFormScreen.kt` -> `CustomerViewModel.addLedgerEntry` -> `LedgerDao.insertEntry`.
- Add a payment transaction and confirm it appears with payment styling and decreases balance; evidence: `TransactionFormScreen.kt` payment path and `TransactionType.PAYMENT`.
- Edit a transaction (long press) and confirm amount/note updates; evidence: `CustomerDetailScreen.kt` `EditTransactionDialog` -> `CustomerViewModel.updateLedgerEntry`.
- Edit customer details and confirm list/detail reflect the changes; evidence: `EditCustomerScreen.kt` -> `CustomerViewModel.updateCustomer`.
- Delete customer and confirm it disappears from lists and reports; evidence: `CustomerViewModel.deleteCustomerRecursive` and `CustomerDao.getAllCustomers` filter `isDeleted = 0`.
- Open Reports tab and confirm charts and stats render when data exists; evidence: `ReportsScreen.kt` and `ReportsViewModel.kt` `StateFlow` values.
- Export a backup zip and confirm file creation in chosen location; evidence: `ReportsScreen.kt` `CreateDocument` -> `ReportsViewModel.exportBackup`.
- Import the same backup and confirm data is restored and counts match; evidence: `ReportsViewModel.importBackup` -> `BackupRepository.replaceAllData`.
- View a customer photo full-screen and save to gallery; confirm image appears in Pictures/Noteds; evidence: `FullScreenImageDialog.kt` -> `CustomerViewModel.saveImageToGallery`.
- For deeper manual tests, follow the checklist in `TESTING.md` (backup integrity, migrations, navigation backstack).
