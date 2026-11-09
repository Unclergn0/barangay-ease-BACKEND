
package com.muntinlupa.barangay_ease.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "resident_id",
        referencedColumnName = "resident_id"
    )
    private Resident resident;

    @Column(name = "service_type", nullable = false, length = 100)
    private String requestType;

    @Column(name = "barangay_id")
    private Integer barangayId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(length = 50)
    private String status = "Pending";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "pickup_time")
    private LocalTime pickupTime;

    // Uploaded files attached to this service request
    @OneToMany(
        mappedBy = "serviceRequest",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @OrderBy("fileIndex ASC")
    @JsonManagedReference
    private List<ServiceRequestFile> files = new ArrayList<>();

    public ServiceRequest() {
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Resident getResident() {
        return resident;
    }

    public void setResident(Resident resident) {
        this.resident = resident;
    }

    public Integer getBarangayId() { return barangayId; }

    public void setBarangayId(Integer barangayId) { this.barangayId = barangayId; }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public List<ServiceRequestFile> getFiles() {
        return files;
    }

    public void setFiles(List<ServiceRequestFile> files) {
        this.files = files != null ? files : new ArrayList<>();
    }

    // Helper method to maintain both sides of the relationship
    public void addFile(ServiceRequestFile file) {
        if (file != null) {
            files.add(file);
            file.setServiceRequest(this);
        }
    }

    public void removeFile(ServiceRequestFile file) {
        if (file != null && files.remove(file)) {
            file.setServiceRequest(null);
        }
    }
}
