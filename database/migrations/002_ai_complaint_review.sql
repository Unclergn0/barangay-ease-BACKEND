-- OPTIONAL LEGACY-SCHEMA MIGRATION ONLY.
-- The supplied barangayease_db (Nov 08, 2026) dump ALREADY contains all five
-- columns below. Do NOT run this file after importing that dump, or MariaDB
-- will report duplicate-column errors. Back up and inspect the live schema first.
ALTER TABLE complaints
    ADD COLUMN ai_message TEXT NULL,
    ADD COLUMN ai_review_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN ai_review_note TEXT NULL,
    ADD COLUMN reviewed_by INT NULL,
    ADD COLUMN reviewed_at DATETIME NULL;

-- Optional FK if admins.admin_id is the referenced primary key:
-- ALTER TABLE complaints ADD CONSTRAINT fk_complaints_reviewed_by
--     FOREIGN KEY (reviewed_by) REFERENCES admins(admin_id) ON DELETE SET NULL;
