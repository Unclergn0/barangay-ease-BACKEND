package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {
    List<ServiceRequest> findByResident_ResidentId(Integer residentId);
    List<ServiceRequest> findByResident_BarangayId(Integer barangayId);
}