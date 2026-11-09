package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_ConversationIdOrderByCreatedAtAscMessageIdAsc(
        Long conversationId
    );
}