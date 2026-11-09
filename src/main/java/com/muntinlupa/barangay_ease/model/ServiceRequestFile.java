package com.muntinlupa.barangay_ease.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_files")
public class ServiceRequestFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonBackReference
    private ServiceRequest serviceRequest;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "file_path", length = 500)
    private String filePath;
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "file_index")
    private Integer fileIndex = 0;
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public ServiceRequestFile() {
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer v) {
        fileId = v;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest v) {
        serviceRequest = v;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String v) {
        fileName = v;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String v) {
        filePath = v;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String v) {
        fileType = v;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long v) {
        fileSize = v;
    }

    public Integer getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(Integer v) {
        fileIndex = v;
    }

    public String getName() {
        return fileName;
    }

    public String getType() {
        return fileType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime v) {
        uploadedAt = v;
    }
}
