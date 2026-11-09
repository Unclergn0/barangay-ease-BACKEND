package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ComplaintFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintFileRepository extends JpaRepository<ComplaintFile, Integer> {
    List<ComplaintFile> findByComplaintId(Integer complaintId);
}
