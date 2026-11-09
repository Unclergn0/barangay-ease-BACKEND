package com.muntinlupa.barangay_ease.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Integer complaintId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "resident_id",
        referencedColumnName = "resident_id"
    )
    private Resident resident;

    @Column(name = "barangay_id")
    private Integer barangayId;

    @Column(name = "complainant_name", nullable = false, length = 150)
    private String complainantName;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /*
     * Priority only:
     * mild, immediate, critical
     *
     * Keep the existing MySQL ENUM column.
     * Do not force JDBC CHAR binding.
     */
    @Column(
        name = "classification_type",
        nullable = false,
        columnDefinition = "ENUM('mild','critical','immediate')"
    )
    private String classificationType = "mild";

    @Column(name = "evidence", nullable = false)
    private String evidence = "";

    @Column(name = "status", length = 50)
    private String status = "Pending";

    @Column(name = "ai_message", columnDefinition = "TEXT")
    private String aiMessage;

    @Column(name = "ai_review_status", length = 30)
    private String aiReviewStatus = "NOT_REQUIRED";

    @Column(name = "ai_review_note", columnDefinition = "TEXT")
    private String aiReviewNote;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Complaint() {
    }

    public Integer getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Integer complaintId) {
        this.complaintId = complaintId;
    }

    public Resident getResident() {
        return resident;
    }

    public void setResident(Resident resident) {
        this.resident = resident;
    }

    public Integer getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Integer barangayId) {
        this.barangayId = barangayId;
    }

    public String getComplainantName() {
        return complainantName;
    }

    public void setComplainantName(String complainantName) {
        this.complainantName = complainantName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("complaintDetails")
    public String getComplaintDetails() {
        return description;
    }

    @JsonProperty("complaintDetails")
    public void setComplaintDetails(String complaintDetails) {
        this.description = complaintDetails;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public void setClassificationType(String classificationType) {
        this.classificationType = classificationType;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(String aiMessage) {
        this.aiMessage = aiMessage;
    }

    public String getAiReviewStatus() {
        return aiReviewStatus;
    }

    public void setAiReviewStatus(String aiReviewStatus) {
        this.aiReviewStatus = aiReviewStatus;
    }

    public String getAiReviewNote() {
        return aiReviewNote;
    }

    public void setAiReviewNote(String aiReviewNote) {
        this.aiReviewNote = aiReviewNote;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}