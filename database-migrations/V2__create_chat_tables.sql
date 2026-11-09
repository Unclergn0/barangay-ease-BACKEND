-- Chat tables matching the supplied barangayease_db MariaDB schema.
-- Existing databases that already contain these tables need no action.
CREATE TABLE IF NOT EXISTS chat_conversations (
    conversation_id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_user_id INT(11) NOT NULL,
    owner_role VARCHAR(20) NOT NULL,
    barangay VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (conversation_id),
    UNIQUE KEY uq_chat_owner (owner_user_id, owner_role),
    KEY idx_chat_barangay (barangay)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT(20) UNSIGNED NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (message_id),
    KEY idx_chat_message_history (conversation_id, created_at, message_id),
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES chat_conversations(conversation_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
