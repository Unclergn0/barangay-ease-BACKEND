
package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Integer> {

    Optional<Resident> findByEmail(String email);

    Optional<Resident> findByContactNumber(String contactNumber);

    // Retrieve residents belonging to one barangay only
    List<Resident> findByBarangayIdOrderByCreatedAtDesc(Integer barangayId);
}
