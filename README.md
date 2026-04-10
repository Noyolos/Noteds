# Noteds

A local-first Android app for managing customer debts — built for a real grocery store where customers regularly buy on credit and pay later.

---

## The Problem

My parents run a grocery store where many customers buy on credit. Tracking who owes what, how much they've paid back, and when to follow up was all done on paper. Records got lost, balances were wrong, and there was no easy way to see the overall picture.

I built Noteds to replace that workflow with something reliable, fast, and always available — even without internet.

## How It Works

Add a customer → record debts and payments → see balances update in real time → review reports and trends on the dashboard.

The app is designed around one core idea: *every ringgit in and out is tracked as a ledger entry*, and everything else — balances, reports, aging analysis — is derived from that ledger automatically.

## Screenshots

| Dashboard | Customers |
|-----------|-----------|
| ![dashboard](screenshots/dashboard.jpg) | ![customers](screenshots/customers.jpg) |

| Customer Detail | Add Debt |
|----------------|----------|
| ![detail](screenshots/detail.jpg) | ![add-debt](screenshots/add-debt.jpg) |

| Reports — Charts | Reports — Aging & Top Debtors |
|------------------|-------------------------------|
| ![reports-charts](screenshots/reports-charts.jpg) | ![reports-debtors](screenshots/reports-debtors.jpg) |

## Data Architecture

The app uses two main entities:

*CustomerEntity* — stores customer info (name, phone, photos, ID documents) and doubles as a folder node for organizing customers into groups. A single isGroup flag and parentId field turn the same table into a tree structure.

*LedgerEntryEntity* — every debt and payment is one row with a type (DEBT or PAYMENT), amount, timestamp, and note. Balances are never stored directly — they're always calculated from the sum of ledger entries.

Customer (isGroup=false)
  └── LedgerEntry (DEBT, RM 500)
  └── LedgerEntry (PAYMENT, RM 200)
  └── Balance = RM 300 (calculated, not stored)

Folder (isGroup=true, parentId=null)
  └── Customer A (parentId=folder.id)
  └── Customer B (parentId=folder.id)
  └── Folder balance = sum of all children (calculated recursively)

## Data Flow

User adds customer / records transaction
  → ViewModel validates input
  → Photos copied to app private directory
  → Repository → DAO → Room (SQLite)
  → Room emits new data via Flow
  → ViewModel recalculates balances, stats, reports
  → Compose UI collects StateFlow and re-renders

Everything is reactive. Record a payment, and the customer balance, dashboard totals, reports, and aging stats all update automatically through Kotlin Flow.

## Why Local-First, No Cloud

This was a deliberate choice, not a limitation:

- *No internet required* — the store's connection is unreliable
- *No server cost* — this is a personal tool, not a SaaS product
- *Full data ownership* — customer financial data stays on the device
- *Simpler architecture* — no sync conflicts, no auth system, no API layer

The tradeoff is no multi-device sync. Switching phones requires a manual backup and restore.

## Backup & Restore

Backup isn't just a database dump. The app exports a zip containing:

- data.json — all active customers and ledger entries
- photos/ — all customer and ID photos

Restore uses a staged approach:

1. Extract zip to a temporary directory
2. Photos go to a staged_customer_photos folder first (not directly to production)
3. Current photos are backed up to existing_customer_photos_backup
4. Database is replaced in a single Room transaction (delete all → insert all)
5. If anything fails, photos roll back from the backup copy
6. Database transaction also rolls back on failure

This makes restore close to atomic — it either fully succeeds or fully reverts.

## Folder / Group System

Instead of a separate folders table, folders are just CustomerEntity rows with isGroup = true. This keeps the schema simple:

- Root level: parentId IS NULL
- Inside a folder: parentId = folder.id
- Folder balances: recursively aggregated from all children in memory
- Moving a customer into a folder that's its own descendant is blocked to prevent circular references
- Deleting a folder recursively deletes all children, their ledger entries, and their photos

## Tech Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Async | Coroutines + Flow |
| Image Loading | Coil |
| Serialization | Gson |
| Build | Gradle + KSP |

## Features

- Customer management with photos and ID documents
- Folder/group hierarchy for organizing customers
- Debt and payment ledger with full transaction history
- Real-time balance calculation from ledger entries
- Dashboard with total outstanding, top debtors, monthly stats
- Reports with monthly debt vs payment trends, aging distribution, average collection period
- Zip backup with photos and staged restore with rollback
- Soft delete for safe data removal
- Chinese / English language toggle
- Expected repayment date tracking

## Run Locally

git clone https://github.com/Noyolos/Noteds.git
cd Noteds
# Open in Android Studio
# Build and run on emulator or device (min SDK 26)

## Project Status

Working v1, actively used in a real grocery store.

Known areas for future improvement:

- Consolidate legacy JSON backup code with current zip-based backup
- Migrate hardcoded UI strings to proper strings.xml resources
- Consider splitting the shared Customer/Folder table if complexity grows
- Optimize recursive folder balance calculation for larger datasets

## Real Usage

Built for my parents' grocery store. Used to track real customer debts and payments daily. The backup/restore system was specifically designed because losing even one customer's payment history is not acceptable.
