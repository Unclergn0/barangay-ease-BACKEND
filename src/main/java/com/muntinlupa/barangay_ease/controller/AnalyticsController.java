package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Barangay;
import com.muntinlupa.barangay_ease.model.Complaint;
import com.muntinlupa.barangay_ease.model.ServiceRequest;
import com.muntinlupa.barangay_ease.repository.BarangayRepository;
import com.muntinlupa.barangay_ease.repository.ComplaintRepository;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.repository.ServiceRequestRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Database-backed analytics for the authenticated administrator's assigned barangay.
 * Scope is derived exclusively from the verified JWT, never from a client-supplied barangay ID.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final ComplaintRepository complaints;
    private final ServiceRequestRepository requests;
    private final ResidentRepository residents;
    private final BarangayRepository barangays;

    public AnalyticsController(
            ComplaintRepository complaints,
            ServiceRequestRepository requests,
            ResidentRepository residents,
            BarangayRepository barangays) {
        this.complaints = complaints;
        this.requests = requests;
        this.residents = residents;
        this.barangays = barangays;
    }

    @GetMapping("/report")
    public ResponseEntity<?> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AppUser admin = CurrentUser.get();
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.role())) {
            return ResponseEntity.status(403).body(Map.of("message", "Administrator access is required."));
        }
        if (admin.barangayId() == null || admin.barangayId() <= 0) {
            return ResponseEntity.status(403).body(Map.of("message", "Your administrator account has no valid barangay assignment."));
        }
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "A valid date range is required."));
        }
        Integer barangayId = admin.barangayId();
        String barangayName = barangays.findById(barangayId)
                .map(Barangay::getBarangayName)
                .orElse(admin.barangay());

        List<Complaint> allComplaints = complaints.findByBarangayIdOrderByCreatedAtDesc(barangayId);
        List<ServiceRequest> allRequests = requests.findByBarangayId(barangayId);

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime until = endDate.plusDays(1).atStartOfDay();

        List<Complaint> filteredComplaints = allComplaints.stream()
                .filter(c -> inRange(c.getCreatedAt(), from, until))
                .toList();
        List<ServiceRequest> filteredRequests = allRequests.stream()
                .filter(r -> inRange(r.getCreatedAt(), from, until))
                .toList();

        List<Map<String, Object>> daily = new ArrayList<>();
        Map<LocalDate, Map<String, Object>> byDate = new TreeMap<>();
        for (Complaint c : filteredComplaints) {
            LocalDate date = c.getCreatedAt().toLocalDate();
            Map<String, Object> row = byDate.computeIfAbsent(date, d -> dailyRow(d, barangayName));
            row.put("complaints", (Integer) row.get("complaints") + 1);
        }
        for (ServiceRequest r : filteredRequests) {
            LocalDate date = r.getCreatedAt().toLocalDate();
            Map<String, Object> row = byDate.computeIfAbsent(date, d -> dailyRow(d, barangayName));
            row.put("requests", (Integer) row.get("requests") + 1);
            if (isPending(r.getStatus())) {
                row.put("pendingRequests", (Integer) row.get("pendingRequests") + 1);
            }
        }
        daily.addAll(byDate.values());

        List<Map<String, Object>> critical = filteredComplaints.stream()
                .filter(c -> isCritical(c.getClassificationType()))
                .map(c -> criticalRow(c, barangayName))
                .toList();

        long totalResidents = residents.findByBarangayIdOrderByCreatedAtDesc(barangayId).size();
        long pendingRequests = filteredRequests.stream().filter(r -> isPending(r.getStatus())).count();

        Map<String, Object> barangaySummary = new LinkedHashMap<>();
        barangaySummary.put("barangay", barangayName);
        barangaySummary.put("residents", totalResidents);
        barangaySummary.put("complaints", filteredComplaints.size());
        barangaySummary.put("criticalComplaints", critical.size());
        barangaySummary.put("requests", filteredRequests.size());
        barangaySummary.put("pendingRequests", pendingRequests);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("barangay", barangayName);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalResidents", totalResidents);
        result.put("totalComplaints", filteredComplaints.size());
        result.put("totalRequests", filteredRequests.size());
        result.put("pendingRequests", pendingRequests);
        Map<String, Long> priorityDistribution = new LinkedHashMap<>();
        priorityDistribution.put("mild", filteredComplaints.stream()
                .filter(c -> "mild".equalsIgnoreCase(c.getClassificationType())).count());
        priorityDistribution.put("immediate", filteredComplaints.stream()
                .filter(c -> "immediate".equalsIgnoreCase(c.getClassificationType())).count());
        priorityDistribution.put("critical", filteredComplaints.stream()
                .filter(c -> "critical".equalsIgnoreCase(c.getClassificationType())).count());
        result.put("priorityDistribution", priorityDistribution);
        result.put("daily", daily);
        result.put("criticalComplaints", critical);
        result.put("barangays", List.of(barangaySummary));
        return ResponseEntity.ok(result);
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime until) {
        return value != null && !value.isBefore(from) && value.isBefore(until);
    }

    private boolean isPending(String status) {
        return status != null && status.toLowerCase(Locale.ROOT).contains("pending");
    }

    private boolean isCritical(String classification) {
        return classification != null &&
                ("critical".equalsIgnoreCase(classification) || "immediate".equalsIgnoreCase(classification));
    }

    private Map<String, Object> dailyRow(LocalDate date, String barangay) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date.toString());
        row.put("barangay", barangay);
        row.put("complaints", 0);
        row.put("requests", 0);
        row.put("pendingRequests", 0);
        return row;
    }

    private Map<String, Object> criticalRow(Complaint c, String barangay) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("caseId", "CMP-" + c.getComplaintId());
        row.put("barangay", barangay);
        row.put("category", c.getTitle() == null ? "Complaint" : c.getTitle());
        row.put("summary", c.getDescription() == null ? "" : c.getDescription());
        row.put("date", c.getCreatedAt() == null ? "" : c.getCreatedAt().toLocalDate().toString());
        row.put("severity", "critical".equalsIgnoreCase(c.getClassificationType()) ? "Critical" : "High");
        row.put("status", c.getStatus() == null ? "Unknown" : c.getStatus());
        return row;
    }
}
