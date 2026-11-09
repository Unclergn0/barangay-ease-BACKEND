
package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.Barangay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarangayRepository
        extends JpaRepository<Barangay, Integer> {

    Optional<Barangay> findByBarangayNameIgnoreCase(String barangayName);
}
