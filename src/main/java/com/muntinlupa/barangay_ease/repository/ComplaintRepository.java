
package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Integer> {

    List<Complaint> findByResident_ResidentId(Integer residentId);

    List<Complaint>
    findByClassificationTypeIgnoreCaseOrderByCreatedAtDesc(
            String classificationType
    );

    // Barangay-specific admin query.
    List<Complaint> findByBarangayIdOrderByCreatedAtDesc(
            Integer barangayId
    );

    List<Complaint>
    findByBarangayIdAndResident_ResidentIdOrderByCreatedAtDesc(
            Integer barangayId,
            Integer residentId
    );
}
