-- BarangayEase V4: one designated admin account per role per barangay.
-- Run the preflight query first. Resolve any duplicate rows before creating the index.
-- SELECT barangay_id, LOWER(role), COUNT(*) AS total
-- FROM admins GROUP BY barangay_id, LOWER(role) HAVING COUNT(*) > 1;
--
-- If the query returns rows, manually confirm the legitimate account to retain
-- before deleting/merging duplicates. Do not delete accounts automatically.

ALTER TABLE admins
    ADD CONSTRAINT uk_admins_barangay_role UNIQUE (barangay_id, role);
