package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import com.muntinlupa.barangay_ease.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Resident-to-barangay communication endpoints used by the resident dashboard. */
@RestController
@RequestMapping("/api")
public class ResidentCommunicationController {

    private final ResidentRepository residents;
    private final NotificationService notifications;

    public ResidentCommunicationController(ResidentRepository residents, NotificationService notifications) {
        this.residents = residents;
        this.notifications = notifications;
    }

    @PostMapping("/resident/inquiries")
    public ResponseEntity<?> submitInquiry(@RequestBody(required = false) InquiryRequest request) {
        AppUser user = CurrentUser.get();
        if (!"RESIDENT".equals(user.role())) return forbidden();
        if (request == null || blank(request.subject()) || blank(request.message())) {
            return badRequest("Inquiry subject and message are required.");
        }
        String subject = request.subject().trim();
        String message = request.message().trim();
        if (subject.length() > 120 || message.length() > 2000) {
            return badRequest("Subject must be at most 120 characters and message at most 2,000 characters.");
        }
        Resident resident = residents.findById(Math.toIntExact(user.id())).orElse(null);
        if (resident == null || resident.getBarangay() == null || resident.getBarangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your resident barangay assignment is missing. Contact the barangay office."));
        }
        String residentName = (resident.getFirstName() + " " + resident.getLastName()).trim();
        notifications.notifyBarangayAdmins(resident.getBarangayId(), "Resident inquiry: " + subject,
                "From " + residentName + " (Resident ID " + resident.getResidentId() + "): " + message);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Your inquiry was sent to the barangay office."));
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody(required = false) FeedbackRequest request) {
        AppUser user = CurrentUser.get();
        if (!"RESIDENT".equals(user.role())) return forbidden();
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            return badRequest("Please provide a rating from 1 to 5.");
        }
        String comment = request.comment() == null ? "" : request.comment().trim();
        if (comment.length() > 2000) return badRequest("Feedback must be at most 2,000 characters.");
        Resident resident = residents.findById(Math.toIntExact(user.id())).orElse(null);
        if (resident == null || resident.getBarangay() == null || resident.getBarangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your resident barangay assignment is missing. Contact the barangay office."));
        }
        String residentName = (resident.getFirstName() + " " + resident.getLastName()).trim();
        notifications.notifyBarangayAdmins(resident.getBarangayId(), "Resident feedback: " + request.rating() + "/5",
                "From " + residentName + " (Resident ID " + resident.getResidentId() + "): "
                        + (comment.isBlank() ? "No written comment was provided." : comment));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Thank you for your feedback!"));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Resident access only."));
    }
    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    public record InquiryRequest(String subject, String message) {}
    public record FeedbackRequest(Integer rating, String comment) {}
}
