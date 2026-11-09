
package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    Optional<Admin> findByEmail(String email);

    List<Admin> findByBarangayIgnoreCase(String barangay);

    boolean existsByBarangayIgnoreCase(String barangay);

    boolean existsByEmail(String email);

    List<Admin> findByBarangayId(Integer barangayId);

    boolean existsByBarangayIdAndRoleIgnoreCase(Integer barangayId, String role);
}
