# Manual Testing Checklist

## Backup & Restore
- Export a backup from a dataset with active and soft-deleted customers; confirm soft-deleted records are absent from the archive.
- Import the backup on top of existing data; verify all records match the backup and no partial data remains if you simulate a failure.
- Create a customer with at least one photo (via the existing capture/pick flow), export a backup, delete the customer and clear the on-device `customer_photos` directory, then import the backup on the same device; confirm both the customer data and all restored photo files exist with valid `photoPath` values.
- Create at least five customers, each with a real photo, export a backup, delete those customers (and photos), then import the backup; verify the import succeeds without JSON parsing errors such as "End of input at character 0" and that every customer, ledger entry, and photo is restored (individual corrupted photos may import with `null` paths while the rest succeed).
- Import an old-format (JSON-only) backup; confirm customer and ledger data restore while photo paths remain `null` without errors.
- Simulate a corrupted photo payload (e.g., edit `photoBase64` to an invalid string) and import; confirm the import completes, the affected photo path is `null`, and other data remains intact.
- Soft-delete a customer with a photo, export a new backup, and confirm the deleted customer and photo file are absent from the archive and stay missing after import.

## Navigation
- From Dashboard/List/Reports, open a customer detail, edit, then back; ensure system back pops to the previous screen.
- From customer list, open Add Customer, save, and confirm you return to the main tabs; repeat with back to discard changes.

## Customer & Ledger Forms
- Add customer: blank name should show “姓名必填”; invalid phone (e.g., letters) should show “電話格式不正確”.
- Add customer with initial debt: entering a non-number or ≤0 should show “金額必須大於 0”.
- Edit customer: same validations as add.
- Add ledger entry: zero/blank amount shows “金額必須大於 0”; saving with valid data creates the row.

## Soft Delete
- Delete a customer: ledger entries are removed, photos disappear from storage (verify file paths are nulled in detail view), and the customer disappears from lists/reports.
- Import a backup that previously contained the deleted customer: ensure the deleted customer does not reappear.

## Reports & Totals
- After adding debts and payments, confirm dashboard totals and aging widgets update accordingly.

## Migrations
- Install an app built with DB version 3/4, populate sample data, then upgrade to current build and launch to confirm migration succeeds and data remains.
