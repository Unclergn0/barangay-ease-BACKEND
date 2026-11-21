package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_ConversationIdOrderByCreatedAtAscMessageIdAsc(
        Long conversationId
    );

    List<ChatMessage> findByConversation_ConversationIdOrderByCreatedAtDescMessageIdDesc(
        Long conversationId, Pageable pageable
    );
}