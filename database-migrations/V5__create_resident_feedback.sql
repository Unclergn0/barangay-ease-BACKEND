-- Persist resident satisfaction ratings so the citywide report can calculate
-- accurate, date-filtered results for every barangay.
CREATE TABLE IF NOT EXISTS resident_feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    resident_id INT NOT NULL,
    barangay_id INT NOT NULL,
    rating TINYINT NOT NULL,
    feedback_comment VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (feedback_id),
    KEY idx_feedback_barangay_created (barangay_id, created_at),
    KEY idx_feedback_created (created_at),
    CONSTRAINT chk_resident_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
