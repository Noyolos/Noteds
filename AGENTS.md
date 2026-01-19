# AGENTS.md

## Non-negotiables
- No unrelated refactors or formatting-only churn.
- Minimal diffs; touch the smallest set of files.
- Always search and trace call chains before editing (use `rg` first).
- Update docs if any contract/route/data/API changes.
- Never print secret values or embed real customer data in logs/docs.

## Entry Points and Behavior Contracts
- Server start: none; this is a single-device Android app with no backend. Do not add network services without updating `docs/API_CONTRACT.md` and `app/src/main/AndroidManifest.xml` permissions.
- UI bootstrap: `app/src/main/java/com/example/noteds/MainActivity.kt` `MainActivity.onCreate` must continue to build `CustomerViewModel` and `ReportsViewModel` from `AppContainer` and call `AppRoot` inside `setContent`.
- Routing root: `app/src/main/java/com/example/noteds/ui/AppRoot.kt` `AppRoot` must continue to handle `Screen` stack and bottom tabs without breaking back behavior.

## Data Contracts
- Room schema is defined by `app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt` and `app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt`; changes require migrations in `app/src/main/java/com/example/noteds/data/db/AppDatabase.kt` and registration in `app/src/main/java/com/example/noteds/di/AppContainer.kt`.
- `customers.isDeleted` is a soft-delete flag and must remain the filter in DAO queries; evidence: `app/src/main/java/com/example/noteds/data/dao/CustomerDao.kt`.
- `ledger_entries.customerId` must reference an existing customer; `type` must be `DEBT` or `PAYMENT`; `amount` must be > 0; evidence: `LedgerEntryEntity.kt` and `CustomerViewModel.addLedgerEntry`.
- Photo URIs are stored as file paths under `filesDir/customer_photos`; any path change must update backup import/export and cleanup logic; evidence: `CustomerViewModel.persistPhoto` and `ReportsViewModel.restorePhoto`.

## API Contracts
- No HTTP API exists; do not add endpoints without defining an API contract and permissions in `app/src/main/AndroidManifest.xml`.
- Backup file format is a zip with `data.json` and optional `photos/` entries; schema is in `ReportsViewModel.buildBackupJson` and `ReportsViewModel.parseBackupJson`.

## Mandatory Smoke Tests
- Commands: `./gradlew :app:assembleDebug`, `./gradlew :app:test`, `./gradlew :app:connectedAndroidTest`.
- Manual checklist: follow `docs/SMOKE_TESTS.md` after any change touching UI, data, or backup flows.

## Definition of Done
- Tests pass and `docs/SMOKE_TESTS.md` checklist passes.
- Minimal diff and only necessary files touched.
- No secret or PII leaks in code, logs, or docs.
- Contracts and docs updated (`PROJECT_OVERVIEW.md`, `docs/*`) if behavior changes.

## Change Workflow (PROOF -> PLAN -> DIFF -> VERIFY -> ROLLBACK)
- PROOF: list the exact files and symbols to change with evidence links (paths and functions).
- PLAN: write step-by-step changes and note risks to data, backups, and navigation.
- DIFF: implement the smallest change set possible.
- VERIFY: run commands above and the manual checklist in `docs/SMOKE_TESTS.md`.
- ROLLBACK: use `git restore <path>` for local changes or `git revert <commit>` for merged changes; avoid `git reset --hard`.
