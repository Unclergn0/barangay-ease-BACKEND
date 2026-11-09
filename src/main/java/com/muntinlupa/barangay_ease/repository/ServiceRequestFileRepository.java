package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ServiceRequestFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ServiceRequestFileRepository extends JpaRepository<ServiceRequestFile, Integer> {
    List<ServiceRequestFile> findByServiceRequest_RequestIdOrderByFileIndexAsc(Integer requestId);

    Optional<ServiceRequestFile> findByServiceRequest_RequestIdAndFileIndex(Integer requestId, Integer fileIndex);
}
