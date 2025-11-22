# Noteds Maintenance Guide

## Database version and migrations
- **Current schema version:** 5
- **Registered migrations:** `MIGRATION_1_5`, `MIGRATION_2_5`, `MIGRATION_3_5`, `MIGRATION_4_5` (all recreate the v5 tables and copy existing columns).
- **Baseline:** v5 is the stable baseline; future versions should add explicit `Migration(X, Y)` objects and keep the vN→5 shortcuts so older installs remain upgradeable.
- **Rules for future changes:**
  - Never rely on `fallbackToDestructiveMigration` in production builds.
  - When adding columns/tables, create a migration that preserves data, adds indices, and documents dropped/optional fields.
  - Test upgrades from the lowest supported version to the newest before releasing.

## Backup export/import
- **Export format (v2):** `noteds-backup-<timestamp>.zip` containing `data.json` and a `photos/` folder. `data.json` carries `backupVersion = 2`, customers, and ledger entries. Each photo field stores both a relative path (for older archives) **and** an optional `*Base64` payload that inlines image bytes when the source file exists. Photos are mirrored into `photos/` during export for backward compatibility; export always writes a non-empty `data.json` entry and closes the zip stream before returning.
- **Legacy compatibility:** older backups without `backupVersion` are treated as v1 and import only database rows. Missing photos never break import. New backups can still be imported by old clients through the `photos/` directory, while modern imports decode Base64 first.
- **Import mode:** **cover/replace**. All existing customers and ledger entries are cleared and replaced inside a single Room transaction. Soft-deleted customers and orphaned ledger rows are filtered out to avoid resurrecting removed data. For v2 backups, the on-device `customer_photos` directory is cleared before restoring photos; any individual photo decode/copy failure is logged and results in a `null` path for that field instead of aborting the import. Empty or unreadable backup payloads are rejected early with a user-facing error so JSON parsing never runs on blank input. Corrupted/non-JSON/zipped garbage files surface the same readable error and leave data untouched.
- **Failure behavior:** any error during import rolls back the transaction; pre-import data remains untouched. Surface user-friendly error messages at the UI layer.

## Soft delete rules
- Customers are soft-deleted via `isDeleted = true` and their photo paths are nulled.
- All ledger entries for a deleted customer are purged, and owned photo files are removed from storage to prevent stale references.
- Queries and exports ignore soft-deleted customers; backups do not resurrect them.

## Navigation structure
- Navigation is managed by a small stack in `AppRoot` using a `Screen` sealed class. The stack is saved/restored with `rememberSaveable` and a `BackHandler` pops when possible.
- Bottom navigation always resets to the `Main` screen; detail/edit/add flows push onto the stack.
- **Add a new screen:**
  1. Add a new `Screen` subclass if needed.
  2. Extend the `when (currentScreen)` block in `AppRoot` to render the Composable and wire back actions via `navigateBack()/navigateTo(Screen.Main)`.
  3. Decide where to push the new screen from (bottom tabs or detail actions).

## Adding fields or tables
1. Update the Room entity with the new column/table.
2. Create a migration that adds the column/table, sets defaults, and backfills data when possible.
3. Register the migration in `AppContainer`.
4. Extend backup export/import to include the new fields while keeping transactional safety.
5. Validate UI and ViewModel paths for inputs; add tests/checklist entries.

## Testing checklist (quick reference)
- Run unit/UI tests where available (`./gradlew test`), or use the manual checklist in `TESTING.md`.
- Verify backup import/export success and rollback paths.
- Smoke-test navigation back behavior after visiting detail/edit/add screens.
- Confirm soft-deleted customers disappear from lists/reports and cannot be revived via import.
