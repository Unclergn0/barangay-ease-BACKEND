
package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "conversation_id",
        referencedColumnName = "conversation_id",
        nullable = false
    )
    private ChatConversation conversation;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(
        name = "content",
        nullable = false,
        columnDefinition = "MEDIUMTEXT"
    )
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {
    }

    // GETTERS

    public Long getMessageId() {
        return messageId;
    }

    public ChatConversation getConversation() {
        return conversation;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // SETTERS

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public void setConversation(ChatConversation conversation) {
        this.conversation = conversation;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
