
package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "barangays")
public class Barangay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barangay_id")
    private Integer barangayId;

    @Column(name = "barangay_name", nullable = false, length = 100)
    private String barangayName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Barangay() {
    }

    public Integer getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Integer barangayId) {
        this.barangayId = barangayId;
    }

    public String getBarangayName() {
        return barangayName;
    }

    public void setBarangayName(String barangayName) {
        this.barangayName = barangayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
