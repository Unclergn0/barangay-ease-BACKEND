
package com.muntinlupa.barangay_ease.service;

import com.muntinlupa.barangay_ease.dto.ResidentAdminResponse;
import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ResidentService {

    private final ResidentRepository residentRepository;

    public ResidentService(ResidentRepository residentRepository) {
        this.residentRepository = residentRepository;
    }

    // Admin can retrieve residents from their own barangay only.
    @Transactional(readOnly = true)
    public List<ResidentAdminResponse> getResidentsByBarangay(Integer barangayId) {

        if (barangayId == null) {
            throw new IllegalArgumentException("Admin barangay ID is required.");
        }

        List<Resident> residents =
                residentRepository.findByBarangayIdOrderByCreatedAtDesc(barangayId);

        return residents.stream()
                .map(resident -> new ResidentAdminResponse(
                        resident.getResidentId(),
                        resident.getFirstName(),
                        resident.getMiddleName(),
                        resident.getLastName(),
                        resident.getContactNumber(),
                        resident.getHouseStreet(),
                        resident.getBarangay(),
                        resident.getBarangayId(),
                        resident.getEmail(),
                        resident.getCreatedAt()
                ))
                .toList();
    }

    public Optional<Resident> getResidentById(Long id) {
        return residentRepository.findById(Math.toIntExact(id));
    }

    public Optional<Resident> getResidentById(Integer id) {
        return residentRepository.findById(id);
    }

    public String registerResident(Resident resident) {

        if (resident.getEmail() == null ||
                resident.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (residentRepository.findByEmail(resident.getEmail()).isPresent()) {
            throw new IllegalStateException("Email is already in use.");
        }

        residentRepository.save(resident);

        return "Resident registered successfully!";
    }
}
