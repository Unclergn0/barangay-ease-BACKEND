package com.muntinlupa.barangay_ease.service;

import com.muntinlupa.barangay_ease.model.ServiceRequest;
import com.muntinlupa.barangay_ease.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }

    public List<ServiceRequest> getTransactionsByResident(Long residentId) {
        return serviceRequestRepository.findByResident_ResidentId(Math.toIntExact(residentId));
    }

    public ServiceRequest createRequest(ServiceRequest request) {
        return serviceRequestRepository.save(request);
    }

    public boolean cancelRequest(Integer requestId) {
        if (serviceRequestRepository.existsById(requestId)) {
            serviceRequestRepository.deleteById(requestId);
            return true;
        }
        return false;
    }
}