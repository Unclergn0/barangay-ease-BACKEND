# Database migration notes

The application uses `spring.jpa.hibernate.ddl-auto=validate`; it will not silently
create or alter production tables.

Before changing a database, create a fresh backup and test the migration on a
restored copy. The application source is aligned to the current BarangayEase
MariaDB schema containing the `barangays` table and `barangay_id` columns on
`admins`, `residents`, `complaints`, `service_requests`, and `notifications`.

- `V3__backfill_barangay_ids.sql` is for older databases that still need the
  barangay lookup table/ID columns and backfill. Do not run it if the columns
  and lookup table already exist.
- `V2__create_chat_tables.sql` creates the chat tables if they are absent.
- `database/migrations/002_ai_complaint_review.sql` is a legacy migration.
  Inspect the actual schema first; do not run it against a database that
  already contains those columns. The old hard-coded sample-PIN migration is
  intentionally excluded from this cleaned package.

Do not import an old SQL backup over a live database. This deployment package
intentionally excludes database dumps because they can contain residents'
personal information and password/security-PIN hashes.
