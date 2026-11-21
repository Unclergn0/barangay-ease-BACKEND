package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.model.ResidentFeedback;
import com.muntinlupa.barangay_ease.repository.ResidentFeedbackRepository;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Barangay-scoped resident feedback endpoints for authenticated administrators.
 * The barangay scope always comes from the verified JWT principal, never from
 * a client-supplied barangay ID.
 */
@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    private final ResidentFeedbackRepository feedbackRepository;
    private final ResidentRepository residentRepository;

    public AdminFeedbackController(
            ResidentFeedbackRepository feedbackRepository,
            ResidentRepository residentRepository) {
        this.feedbackRepository = feedbackRepository;
        this.residentRepository = residentRepository;
    }

    /** Summary for the logged-in administrator's assigned barangay. */
    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AppUser admin = requireAssignedAdmin();
        if (admin == null) return forbidden();
        if (!validDateRange(startDate, endDate)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Provide both startDate and endDate, and ensure startDate is not after endDate."));
        }

        List<ResidentFeedback> rows = loadFeedback(admin.barangayId(), startDate, endDate);
        long[] distribution = new long[6];
        long score = 0;
        long comments = 0;
        for (ResidentFeedback row : rows) {
            Integer rating = row.getRating();
            if (rating != null && rating >= 1 && rating <= 5) {
                distribution[rating]++;
                score += rating;
            }
            if (row.getComment() != null && !row.getComment().isBlank()) comments++;
        }

        Map<String, Long> starDistribution = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            starDistribution.put(star + "Star", distribution[star]);
        }

        List<Map<String, Object>> recentFeedback = toFeedbackResponses(
                rows.stream().limit(10).toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("barangayId", admin.barangayId());
        response.put("barangay", admin.barangay());
        response.put("totalRatings", rows.size());
        response.put("averageRating", rows.isEmpty() ? 0.0 : Math.round((score * 100.0 / rows.size())) / 100.0);
        response.put("commentCount", comments);
        response.put("ratingDistribution", starDistribution);
        response.put("recentFeedback", recentFeedback);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        return ResponseEntity.ok(response);
    }

    /** Full feedback list for the logged-in administrator's assigned barangay. */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AppUser admin = requireAssignedAdmin();
        if (admin == null) return forbidden();
        if (!validDateRange(startDate, endDate)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Provide both startDate and endDate, and ensure startDate is not after endDate."));
        }

        List<Map<String, Object>> result = toFeedbackResponses(
                loadFeedback(admin.barangayId(), startDate, endDate));
        return ResponseEntity.ok(result);
    }

    private AppUser requireAssignedAdmin() {
        AppUser user = CurrentUser.get();
        if (!"ADMIN".equalsIgnoreCase(user.role())
                || user.barangayId() == null || user.barangayId() <= 0) {
            return null;
        }
        return user;
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of(
                "message", "Administrator access with a valid barangay assignment is required."));
    }

    private boolean validDateRange(LocalDate startDate, LocalDate endDate) {
        return (startDate == null && endDate == null)
                || (startDate != null && endDate != null && !startDate.isAfter(endDate));
    }

    private List<ResidentFeedback> loadFeedback(Integer barangayId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return feedbackRepository.findByBarangayIdOrderByCreatedAtDesc(barangayId);
        }
        return feedbackRepository.findByBarangayIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                barangayId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private List<Map<String, Object>> toFeedbackResponses(List<ResidentFeedback> feedbackRows) {
        Set<Integer> residentIds = feedbackRows.stream()
                .map(ResidentFeedback::getResidentId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Integer, Resident> residentsById = new HashMap<>();
        residentRepository.findAllById(residentIds)
                .forEach(resident -> residentsById.put(resident.getResidentId(), resident));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ResidentFeedback feedback : feedbackRows) {
            Resident resident = residentsById.get(feedback.getResidentId());
            String residentName = resident == null ? "Resident #" + feedback.getResidentId()
                    : ((resident.getFirstName() == null ? "" : resident.getFirstName()) + " "
                    + (resident.getLastName() == null ? "" : resident.getLastName())).trim();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feedbackId", feedback.getFeedbackId());
            row.put("residentId", feedback.getResidentId());
            row.put("residentName", residentName.isBlank() ? "Resident #" + feedback.getResidentId() : residentName);
            row.put("barangayId", feedback.getBarangayId());
            row.put("rating", feedback.getRating());
            row.put("comment", feedback.getComment());
            row.put("createdAt", feedback.getCreatedAt());
            result.add(row);
        }
        return result;
    }
}
