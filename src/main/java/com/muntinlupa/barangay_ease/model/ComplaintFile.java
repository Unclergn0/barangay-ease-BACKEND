package com.muntinlupa.barangay_ease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_files")
public class ComplaintFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;
    @Column(name = "complaint_id", nullable = false)
    private Integer complaintId;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    @Column(name = "file_type")
    private String fileType;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public ComplaintFile() {
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer v) {
        fileId = v;
    }

    public Integer getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Integer v) {
        complaintId = v;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime v) {
        uploadedAt = v;
    }
}
