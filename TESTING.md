# Manual Testing Checklist

## Backup & Restore
- Export a backup from a dataset with active and soft-deleted customers; confirm soft-deleted records are absent from the archive.
- Import the backup on top of existing data; verify all records match the backup and no partial data remains if you simulate a failure.

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
