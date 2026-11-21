package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resident_feedback", indexes = {
        @Index(name = "idx_feedback_barangay_created", columnList = "barangay_id, created_at"),
        @Index(name = "idx_feedback_created", columnList = "created_at")
})
public class ResidentFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "resident_id", nullable = false)
    private Integer residentId;

    @Column(name = "barangay_id", nullable = false)
    private Integer barangayId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "feedback_comment", length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getFeedbackId() { return feedbackId; }
    public Integer getResidentId() { return residentId; }
    public void setResidentId(Integer residentId) { this.residentId = residentId; }
    public Integer getBarangayId() { return barangayId; }
    public void setBarangayId(Integer barangayId) { this.barangayId = barangayId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
