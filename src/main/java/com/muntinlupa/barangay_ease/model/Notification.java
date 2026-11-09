package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "barangay_id")
    private Integer barangayId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "user_role",
        nullable = false,
        length = 20,
        columnDefinition = "ENUM('RESIDENT','ADMIN')"
    )
    private String userRole = "RESIDENT";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resident_id")
    private Resident resident;

    @Column(name = "title", nullable = false, length = 150)
    private String title = "Notification";

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer v) { notificationId = v; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer v) { userId = v; }

    public Integer getBarangayId() { return barangayId; }
    public void setBarangayId(Integer v) { barangayId = v; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String v) { userRole = v; }

    public Resident getResident() { return resident; }
    public void setResident(Resident v) { resident = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }

    public String getMessage() { return description; }
    public void setMessage(String v) { description = v; }

    public Boolean getIsRead() { return isRead; }
    public boolean isRead() { return isRead != null && isRead; }
    public void setIsRead(Boolean v) { isRead = v; }
    public void setIsRead(boolean v) { isRead = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}