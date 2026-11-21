package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.ResidentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ResidentFeedbackRepository extends JpaRepository<ResidentFeedback, Long> {
    List<ResidentFeedback> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from, LocalDateTime until);

    List<ResidentFeedback> findByBarangayIdOrderByCreatedAtDesc(Integer barangayId);

    List<ResidentFeedback> findByBarangayIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Integer barangayId, LocalDateTime from, LocalDateTime until);
}
