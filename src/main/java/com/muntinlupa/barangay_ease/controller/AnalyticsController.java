package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Barangay;
import com.muntinlupa.barangay_ease.model.Complaint;
import com.muntinlupa.barangay_ease.model.Resident;
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
 * Consolidated, database-backed analytics for all registered barangays.
 * Any authenticated administrator can view the same citywide report.
 * The administrator's assigned barangay is intentionally NOT used as a filter.
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
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Administrator access is required."));
        }
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A valid date range is required."));
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime until = endDate.plusDays(1).atStartOfDay();

        List<Barangay> allBarangays = barangays.findAll().stream()
                .sorted(Comparator.comparing(
                        Barangay::getBarangayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Map<Integer, Map<String, Object>> summaries = new LinkedHashMap<>();
        Map<Integer, String> barangayNames = new HashMap<>();
        for (Barangay barangay : allBarangays) {
            Integer id = barangay.getBarangayId();
            if (id == null) continue;
            String name = barangay.getBarangayName() == null
                    ? "Barangay " + id : barangay.getBarangayName();
            barangayNames.put(id, name);
            summaries.put(id, emptyBarangaySummary(name));
        }

        // Residents are an all-time registered-resident count, independent of the date filter.
        List<Resident> allResidents = residents.findAll();
        for (Resident resident : allResidents) {
            Integer barangayId = resident.getBarangayId();
            Map<String, Object> summary = summaries.get(barangayId);
            if (summary != null) {
                increment(summary, "residents");
            }
        }

        List<Complaint> filteredComplaints = complaints.findAll().stream()
                .filter(c -> inRange(c.getCreatedAt(), from, until))
                .toList();
        List<ServiceRequest> filteredRequests = requests.findAll().stream()
                .filter(r -> inRange(r.getCreatedAt(), from, until))
                .toList();

        Map<String, Long> priorityDistribution = new LinkedHashMap<>();
        priorityDistribution.put("mild", 0L);
        priorityDistribution.put("immediate", 0L);
        priorityDistribution.put("critical", 0L);

        List<Map<String, Object>> criticalRows = new ArrayList<>();
        Map<LocalDate, Map<String, Object>> dailyByDate = new TreeMap<>();

        for (Complaint complaint : filteredComplaints) {
            Integer barangayId = complaintBarangayId(complaint);
            Map<String, Object> summary = summaries.get(barangayId);
            if (summary == null) continue;

            increment(summary, "complaints");
            String classification = normalize(complaint.getClassificationType());
            if (priorityDistribution.containsKey(classification)) {
                priorityDistribution.put(classification, priorityDistribution.get(classification) + 1);
            }

            LocalDate date = complaint.getCreatedAt().toLocalDate();
            Map<String, Object> daily = dailyByDate.computeIfAbsent(date, this::emptyDailyRow);
            increment(daily, "complaints");

            if (isCritical(classification)) {
                increment(summary, "criticalComplaints");
                criticalRows.add(criticalRow(complaint, barangayNames.get(barangayId)));
                increment(daily, "criticalComplaints");
            }
        }

        for (ServiceRequest request : filteredRequests) {
            Integer barangayId = requestBarangayId(request);
            Map<String, Object> summary = summaries.get(barangayId);
            if (summary == null) continue;

            increment(summary, "requests");
            LocalDate date = request.getCreatedAt().toLocalDate();
            Map<String, Object> daily = dailyByDate.computeIfAbsent(date, this::emptyDailyRow);
            increment(daily, "requests");

            if (isPending(request.getStatus())) {
                increment(summary, "pendingRequests");
                increment(daily, "pendingRequests");
            }
        }

        List<Map<String, Object>> barangayRows = new ArrayList<>(summaries.values());
        long totalResidents = sum(barangayRows, "residents");
        long totalComplaints = sum(barangayRows, "complaints");
        long totalCriticalComplaints = sum(barangayRows, "criticalComplaints");
        long totalRequests = sum(barangayRows, "requests");
        long pendingRequests = sum(barangayRows, "pendingRequests");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "ALL_BARANGAYS");
        result.put("barangay", "All Barangays");
        result.put("barangayCount", barangayRows.size());
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalResidents", totalResidents);
        result.put("totalComplaints", totalComplaints);
        result.put("totalCriticalComplaints", totalCriticalComplaints);
        result.put("totalRequests", totalRequests);
        result.put("pendingRequests", pendingRequests);
        result.put("priorityDistribution", priorityDistribution);
        result.put("daily", new ArrayList<>(dailyByDate.values()));
        result.put("criticalComplaints", criticalRows);
        result.put("barangays", barangayRows);

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> emptyBarangaySummary(String name) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("barangay", name);
        row.put("residents", 0L);
        row.put("complaints", 0L);
        row.put("criticalComplaints", 0L);
        row.put("requests", 0L);
        row.put("pendingRequests", 0L);
        return row;
    }

    private Map<String, Object> emptyDailyRow(LocalDate date) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date.toString());
        row.put("complaints", 0L);
        row.put("criticalComplaints", 0L);
        row.put("requests", 0L);
        row.put("pendingRequests", 0L);
        return row;
    }

    private Integer complaintBarangayId(Complaint complaint) {
        if (complaint.getBarangayId() != null) return complaint.getBarangayId();
        Resident resident = complaint.getResident();
        return resident == null ? null : resident.getBarangayId();
    }

    private Integer requestBarangayId(ServiceRequest request) {
        if (request.getBarangayId() != null) return request.getBarangayId();
        Resident resident = request.getResident();
        return resident == null ? null : resident.getBarangayId();
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime until) {
        return value != null && !value.isBefore(from) && value.isBefore(until);
    }

    private boolean isPending(String status) {
        return status != null && status.toLowerCase(Locale.ROOT).contains("pending");
    }

    private boolean isCritical(String classification) {
        return "critical".equals(classification) || "immediate".equals(classification);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void increment(Map<String, Object> row, String key) {
        row.put(key, ((Number) row.getOrDefault(key, 0L)).longValue() + 1L);
    }

    private long sum(List<Map<String, Object>> rows, String key) {
        return rows.stream().mapToLong(row -> ((Number) row.getOrDefault(key, 0L)).longValue()).sum();
    }

    private Map<String, Object> criticalRow(Complaint complaint, String barangayName) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("caseId", "CMP-" + complaint.getComplaintId());
        row.put("barangay", barangayName == null ? "Unknown Barangay" : barangayName);
        row.put("category", complaint.getTitle() == null ? "Complaint" : complaint.getTitle());
        row.put("summary", complaint.getDescription() == null ? "" : complaint.getDescription());
        row.put("date", complaint.getCreatedAt() == null
                ? "" : complaint.getCreatedAt().toLocalDate().toString());
        row.put("severity", "critical".equalsIgnoreCase(complaint.getClassificationType())
                ? "Critical" : "Immediate");
        row.put("status", complaint.getStatus() == null ? "Unknown" : complaint.getStatus());
        return row;
    }
}
