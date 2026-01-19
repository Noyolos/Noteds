# UI Map

## Navigation map
- Root navigation is a manual stack in `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `AppRoot` using `Screen` sealed class (`Main`, `GroupList`, `AddCustomer`, `CustomerDetail`, `EditCustomer`) and `BackHandler` for pop behavior.
- Main tab routing uses `selectedIndex` inside `AppRoot` to select `DashboardScreen`, `CustomerListScreen`, and `ReportsScreen`; evidence: `AppRoot.kt` `NavigationBar` and `when (selectedIndex.intValue)`.

## Screens and component hierarchy
- Dashboard (Main tab index 0): `DashboardScreen` -> header section, hero card with `TrendLineChart`, `StatCard` row, and `TopDebtorItem` list; evidence: `app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt`.
- Customer list (Main tab index 1): `CustomerListScreen` -> top bar with search, `LazyColumn` of `CustomerCard` items, and create/move dialogs; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerListScreen.kt`.
- Reports (Main tab index 2): `ReportsScreen` -> export/import buttons, `ChartCard` sections (`MonthlyBarChart`, `AgingDonutChart`, `TopDebtorsHorizontalChart`), `InsightCard` summary; evidence: `app/src/main/java/com/example/noteds/ui/reports/ReportsScreen.kt`.
- Group list: `CustomerListScreen` with `parentId` set to `Screen.GroupList.groupCustomerId` and a back action; evidence: `AppRoot.kt` branch `is Screen.GroupList`.
- Add customer: `AddCustomerScreen` -> `PhotoGrid`, basic info inputs, initial debt section, save button; evidence: `app/src/main/java/com/example/noteds/ui/customers/AddCustomerScreen.kt`.
- Customer detail: `CustomerDetailScreen` -> profile header, balance card, photo row, transaction list, and action bar for debt/payment; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerDetailScreen.kt`.
- Edit customer: `EditCustomerScreen` -> `PhotoGrid`, info fields, delete confirmation; evidence: `app/src/main/java/com/example/noteds/ui/customers/EditCustomerScreen.kt`.
- Transaction form (modal screen): `TransactionFormScreen` -> amount input, note field, payment method toggle, date picker; evidence: `app/src/main/java/com/example/noteds/ui/customers/TransactionFormScreen.kt`.

## Key events and state changes
- Customer selection: `CustomerListScreen` item click calls `onCustomerClick`, and `AppRoot` navigates to `Screen.CustomerDetail`; evidence: `CustomerListScreen.kt` `CustomerCard` `onClick` and `AppRoot.kt` `navigateTo(Screen.CustomerDetail(...))`.
- Add customer flow: `AppRoot` navigates to `Screen.AddCustomer`, `AddCustomerScreen` calls `CustomerViewModel.addCustomer`, then `AppRoot` pops back; evidence: `AppRoot.kt` `Screen.AddCustomer` branch and `AddCustomerScreen.kt` save handler.
- Ledger updates: `TransactionFormScreen` calls `CustomerViewModel.addLedgerEntry`, and `CustomerDetailScreen` observes `getTransactionsForCustomer` to refresh; evidence: `CustomerViewModel.kt` and `CustomerDetailScreen.kt`.
- Reports export/import: `ReportsScreen` launches SAF intents and calls `ReportsViewModel.exportBackup`/`importBackup`; evidence: `ReportsScreen.kt` launchers and `ReportsViewModel.kt` methods.

## Shared components and patterns
- Photo handling components: `CustomerPhotoPicker` and `PhotoGrid` encapsulate camera/gallery selection and are used in add/edit flows; evidence: `app/src/main/java/com/example/noteds/ui/customers/CustomerPhotoPicker.kt` and usage in `AddCustomerScreen.kt`/`EditCustomerScreen.kt`.
- Dialog patterns: `EditTransactionDialog` and confirmation dialogs in customer screens use Material3 `AlertDialog`; evidence: `CustomerDetailScreen.kt` and `EditCustomerScreen.kt`.
- Chart components: `TrendLineChart`, `MonthlyBarChart`, `AgingDonutChart`, and `TopDebtorsHorizontalChart` render analytics from `ReportsViewModel` state; evidence: `DashboardScreen.kt` and `ReportsScreen.kt`.
