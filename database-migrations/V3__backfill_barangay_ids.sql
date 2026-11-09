-- Barangay ID alignment migration for the supplied legacy BarangayEase schema.
-- BACK UP THE DATABASE BEFORE RUNNING. Compatible with MariaDB syntax.
-- Creates the barangays lookup table, adds missing barangay_id columns, and
-- backfills IDs from the existing barangay-name fields.

CREATE TABLE IF NOT EXISTS barangays (
    barangay_id INT NOT NULL AUTO_INCREMENT,
    barangay_name VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (barangay_id),
    UNIQUE KEY uk_barangays_name (barangay_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE residents ADD COLUMN IF NOT EXISTS barangay_id INT NULL;
ALTER TABLE admins ADD COLUMN IF NOT EXISTS barangay_id INT NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS barangay_id INT NULL;
ALTER TABLE service_requests ADD COLUMN IF NOT EXISTS barangay_id INT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS barangay_id INT NULL;

-- Seed barangay names already used by the frontend and existing accounts.
INSERT IGNORE INTO barangays (barangay_name) VALUES
('Alabang'), ('Ayala Alabang'), ('Bayanan'), ('Buli'), ('Cupang'),
('Poblacion'), ('Putatan'), ('Sucat'), ('Tunasan');

-- Preserve any other barangay names present in existing user records.
INSERT IGNORE INTO barangays (barangay_name)
SELECT DISTINCT TRIM(barangay) FROM residents
WHERE barangay IS NOT NULL AND TRIM(barangay) <> '';

INSERT IGNORE INTO barangays (barangay_name)
SELECT DISTINCT TRIM(barangay) FROM admins
WHERE barangay IS NOT NULL AND TRIM(barangay) <> '';

-- Assign IDs to account records first.
UPDATE residents r
JOIN barangays b ON LOWER(TRIM(b.barangay_name)) = LOWER(TRIM(r.barangay))
SET r.barangay_id = b.barangay_id
WHERE r.barangay_id IS NULL;

UPDATE admins a
JOIN barangays b ON LOWER(TRIM(b.barangay_name)) = LOWER(TRIM(a.barangay))
SET a.barangay_id = b.barangay_id
WHERE a.barangay_id IS NULL;

-- Assign complaint and request records from their owning resident.
UPDATE complaints c
JOIN residents r ON r.resident_id = c.resident_id
SET c.barangay_id = r.barangay_id
WHERE c.barangay_id IS NULL;

UPDATE service_requests sr
JOIN residents r ON r.resident_id = sr.resident_id
SET sr.barangay_id = r.barangay_id
WHERE sr.barangay_id IS NULL;

-- Notifications can belong to either an admin or a resident.
UPDATE notifications n
JOIN admins a ON n.user_role = 'ADMIN' AND a.admin_id = n.user_id
SET n.barangay_id = a.barangay_id
WHERE n.barangay_id IS NULL;

UPDATE notifications n
JOIN residents r ON n.user_role = 'RESIDENT'
    AND (r.resident_id = n.resident_id OR r.resident_id = n.user_id)
SET n.barangay_id = r.barangay_id
WHERE n.barangay_id IS NULL;

-- Helpful indexes for the barangay-scoped dashboards.
ALTER TABLE complaints
    ADD INDEX IF NOT EXISTS idx_complaints_barangay_created (barangay_id, created_at);
ALTER TABLE service_requests
    ADD INDEX IF NOT EXISTS idx_service_requests_barangay_created (barangay_id, created_at);
ALTER TABLE notifications
    ADD INDEX IF NOT EXISTS idx_notifications_barangay_user (barangay_id, user_role, user_id);
