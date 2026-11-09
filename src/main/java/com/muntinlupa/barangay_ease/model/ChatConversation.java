package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Stores one chat thread for a user and role. */
@Entity
@Table(name = "chat_conversations",
       uniqueConstraints = @UniqueConstraint(name = "uk_chat_owner_role",
           columnNames = {"owner_user_id", "owner_role"}))
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "owner_user_id", nullable = false)
    private Integer ownerUserId;

    @Column(name = "owner_role", nullable = false, length = 20)
    private String ownerRole;

    @Column(name = "barangay", nullable = false, length = 255)
    private String barangay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long value) { conversationId = value; }
    public Integer getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Integer value) { ownerUserId = value; }
    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String value) { ownerRole = value; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String value) { barangay = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
