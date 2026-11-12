
package com.muntinlupa.barangay_ease.service;

import com.muntinlupa.barangay_ease.ai.EmergencyKeywordScreen;
import com.muntinlupa.barangay_ease.ai.ComplaintClassifierService;
import com.muntinlupa.barangay_ease.ai.SafetyMessages;
import com.muntinlupa.barangay_ease.model.*;
import com.muntinlupa.barangay_ease.repository.*;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaints;
    private final ResidentRepository residents;
    private final ComplaintFileRepository files;
    private final FileStorageService storage;
    private final NotificationService notifications;
    private final ComplaintClassifierService classifier;

    public ComplaintService(
            ComplaintRepository complaints,
            ResidentRepository residents,
            ComplaintFileRepository files,
            FileStorageService storage,
            NotificationService notifications,
            ComplaintClassifierService classifier) {

        this.complaints = complaints;
        this.residents = residents;
        this.files = files;
        this.storage = storage;
        this.notifications = notifications;
        this.classifier = classifier;
    }

    // =====================================================
    // GET COMPLAINTS
    // =====================================================

    public List<Complaint> getAllComplaints() {
        return complaints.findAll();
    }

    public List<Complaint> getComplaintsByBarangay(Integer barangayId) {

        if (barangayId == null) {
            throw new IllegalArgumentException(
                    "Admin barangay is not configured.");
        }

        return complaints.findByBarangayIdOrderByCreatedAtDesc(
                barangayId);
    }

    public List<Complaint> getResidentComplaints(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Resident ID is required.");
        }

        return complaints.findByResident_ResidentId(
                Math.toIntExact(id));
    }

    // =====================================================
    // UNIVERSAL COMPLAINT SUBMISSION
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Complaint createComplaint(
            Long residentId,
            String title,
            String details,
            String classification,
            List<MultipartFile> uploads) throws IOException {

        // Validate resident ID.
        if (residentId == null) {
            throw new IllegalArgumentException(
                    "Resident account is required.");
        }

        // Validate complaint details.
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException(
                    "Complaint details are required.");
        }
        if (details.trim().length() > 10000) {
            throw new IllegalArgumentException("Complaint details must be 10,000 characters or fewer.");
        }
        if (title != null && title.trim().length() > 150) {
            throw new IllegalArgumentException("Complaint title must be 150 characters or fewer.");
        }
        if (uploads != null && uploads.size() > 5) {
            throw new IllegalArgumentException("You can upload a maximum of 5 complaint files.");
        }

        // Find resident.
        Resident resident = residents.findById(
                Math.toIntExact(residentId)
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Resident account not found."));

        // Get barangay from authenticated resident record.
        Integer barangayId = resident.getBarangayId();

        if (barangayId == null) {
            throw new IllegalArgumentException(
                    "Your barangay is not configured. Please contact the administrator.");
        }

        // The backend is the authority for classification. Never trust the
        // priority supplied by the browser; clients may alter multipart fields.
        // Always inspect BOTH title and description. Critical keywords are
        // frequently entered in the title while the details are very short.
        String classificationText = "Title: "
                + (title == null ? "" : title.trim())
                + "\nDescription: " + details.trim();

        Map<String, Object> assessment = classifier.classify(title, details);
        Object assessedPriority = assessment.get("priority");
        String priority = normalizePriority(assessedPriority == null ? null : assessedPriority.toString());
        if (priority == null) priority = "mild";

        // Independent emergency screen remains a hard safety backstop.
        EmergencyKeywordScreen.Result screen =
                EmergencyKeywordScreen.screen(classificationText);
        if (screen.level() == EmergencyKeywordScreen.Level.CRITICAL) {
            priority = "critical";
        } else if (screen.level() == EmergencyKeywordScreen.Level.IMMEDIATE
                && "mild".equals(priority)) {
            priority = "immediate";
        }

        // Create complaint entity.
        Complaint complaint = new Complaint();

        complaint.setResident(resident);
        complaint.setBarangayId(barangayId);

        complaint.setComplainantName(
                buildResidentName(resident));

        complaint.setTitle(
                title == null || title.isBlank()
                        ? "Resident Complaint"
                        : title.trim());

        complaint.setDescription(details.trim());
        complaint.setEvidence("");
        complaint.setClassificationType(priority);

        if ("critical".equals(priority)) {

            complaint.setStatus("Pending AI Review");
            Object message = assessment.get("message");
            complaint.setAiMessage(message instanceof String text && !text.isBlank()
                    ? text : SafetyMessages.CRITICAL);
            complaint.setAiReviewStatus("PENDING");

        } else {

            complaint.setStatus("Pending");
            complaint.setAiReviewStatus("NOT_REQUIRED");
        }

        // Save complaint and attachments.
        // If attachment saving fails, the database transaction rolls back.
        Complaint savedComplaint = complaints.save(complaint);

        saveFiles(savedComplaint, uploads);

        // Notification failure must not prevent complaint submission.
        sendComplaintNotification(savedComplaint);

        return savedComplaint;
    }

    // =====================================================
    // NORMALIZE PRIORITY
    // =====================================================

    private String normalizePriority(String classification) {

        if (classification == null || classification.isBlank()) {
            return "mild";
        }

        String value = classification
                .trim()
                .toLowerCase(Locale.ROOT);

        if (Set.of(
                "mild",
                "immediate",
                "critical"
        ).contains(value)) {

            return value;
        }

        // Unknown priority defaults to mild.
        return "mild";
    }

    // =====================================================
    // RESIDENT NAME
    // =====================================================

    private String buildResidentName(Resident resident) {

        String firstName = resident.getFirstName() == null
                ? ""
                : resident.getFirstName().trim();

        String lastName = resident.getLastName() == null
                ? ""
                : resident.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank()
                ? "Resident"
                : fullName;
    }

    // =====================================================
    // CRITICAL COMPLAINT
    // BACKWARD COMPATIBILITY
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Complaint createCriticalComplaint(
            Long residentId,
            String title,
            String details,
            String aiMessage,
            List<MultipartFile> uploads) throws IOException {

        if (residentId == null) {
            throw new IllegalArgumentException(
                    "Resident account is required.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Complaint title is required.");
        }

        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException(
                    "Complaint details are required.");
        }

        Resident resident = residents.findById(
                Math.toIntExact(residentId)
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Resident account not found."));

        Integer barangayId = resident.getBarangayId();

        if (barangayId == null) {
            throw new IllegalArgumentException(
                    "Resident barangay is not configured.");
        }

        Complaint complaint = new Complaint();

        complaint.setResident(resident);
        complaint.setBarangayId(barangayId);

        complaint.setComplainantName(
                buildResidentName(resident));

        complaint.setTitle(title.trim());
        complaint.setDescription(details.trim());
        complaint.setEvidence("");
        complaint.setClassificationType("critical");

        complaint.setAiMessage(
                aiMessage == null || aiMessage.isBlank()
                        ? SafetyMessages.CRITICAL
                        : aiMessage.trim());

        complaint.setAiReviewStatus("PENDING");
        complaint.setStatus("Pending AI Review");

        Complaint saved = complaints.save(complaint);

        saveFiles(saved, uploads);

        sendComplaintNotification(saved);

        return saved;
    }

    // =====================================================
    // SAVE ATTACHMENTS
    // =====================================================

    private void saveFiles(
            Complaint complaint,
            List<MultipartFile> uploads) throws IOException {

        if (uploads == null || uploads.isEmpty()) {
            return;
        }

        // Track uploaded file paths so they can be deleted
        // if a later attachment fails to save.
        List<String> uploadedPaths = new ArrayList<>();

        try {

            for (MultipartFile file : uploads) {

                if (file == null || file.isEmpty()) {
                    continue;
                }

                String savedPath = storage.save(
                        file,
                        "complaints");

                uploadedPaths.add(savedPath);

                ComplaintFile attachment = new ComplaintFile();

                attachment.setComplaintId(
                        complaint.getComplaintId());

                attachment.setFileName(
                        file.getOriginalFilename() == null
                                ? "attachment"
                                : file.getOriginalFilename());

                attachment.setFilePath(savedPath);

                attachment.setFileType(
                        file.getContentType() == null
                                ? "application/octet-stream"
                                : file.getContentType());

                attachment.setFileSize(file.getSize());

                files.save(attachment);
            }

        } catch (IOException | RuntimeException error) {

            // Remove already uploaded physical files.
            deleteUploadedFiles(uploadedPaths);

            // Propagate the error.
            // The transaction will roll back database changes.
            throw error;
        }
    }

    // =====================================================
    // CLEANUP UPLOADED FILES
    // =====================================================

    private void deleteUploadedFiles(List<String> paths) {

        if (paths == null || paths.isEmpty()) {
            return;
        }

        for (String path : paths) {

            if (path == null || path.isBlank()) {
                continue;
            }

            try {

                storage.delete(path);

            } catch (Exception error) {

                log.warn("Unable to clean up uploaded complaint file: {}", path, error);
            }
        }
    }

    // =====================================================
    // NOTIFICATIONS
    // =====================================================

    private void sendComplaintNotification(Complaint complaint) {

        try {

            notifications.complaintReceived(complaint);

        } catch (Exception error) {

            log.warn("Notification failed for complaint ID {}.", complaint.getComplaintId(), error);
        }
    }

    // =====================================================
    // ADMIN AI REVIEW
    // =====================================================

    @Transactional
    public Complaint reviewAiComplaint(
            Integer id,
            String action,
            String classification,
            String note,
            Long adminId,
            Integer adminBarangayId) {

        Complaint complaint = complaints.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Complaint not found."));

        if (adminBarangayId == null || complaint.getBarangayId() == null
                || !adminBarangayId.equals(complaint.getBarangayId())) {
            throw new SecurityException("You cannot review complaints outside your assigned barangay.");
        }

        if (!"critical".equalsIgnoreCase(
                complaint.getClassificationType())) {

            throw new IllegalArgumentException(
                    "This complaint is not in the AI Complaints queue.");
        }

        String act = action == null
                ? ""
                : action.trim().toUpperCase(Locale.ROOT);

        if ("CANCEL".equals(act)) {

            complaint.setAiReviewStatus("CANCELLED");
            complaint.setAiReviewNote(note);
            complaint.setStatus("Under Review");

        } else if ("ACCEPT".equals(act)) {

            complaint.setAiReviewStatus("ACCEPTED");
            complaint.setAiReviewNote(note);
            complaint.setStatus("Under Review");

        } else if ("UPDATE".equals(act)) {

            if (classification == null
                    || !Set.of("mild", "immediate", "critical")
                            .contains(classification.trim().toLowerCase(Locale.ROOT))) {

                throw new IllegalArgumentException(
                        "Classification must be mild, immediate, or critical.");
            }

            String type = normalizePriority(classification);

            complaint.setClassificationType(type);
            complaint.setAiReviewStatus("UPDATED");
            complaint.setAiReviewNote(note);
            complaint.setStatus("Under Review");

        } else {

            throw new IllegalArgumentException(
                    "Action must be ACCEPT, UPDATE, or CANCEL.");
        }

        complaint.setReviewedBy(
                adminId != null
                        ? Math.toIntExact(adminId)
                        : null);

        complaint.setReviewedAt(LocalDateTime.now());

        Complaint saved = complaints.save(complaint);

        try {

            if ("UPDATE".equals(act)) {

                notifications.complaintReclassified(saved);

            } else if ("ACCEPT".equals(act) || "CANCEL".equals(act)) {

                notifications.complaintStatusChanged(
                        saved,
                        saved.getStatus());
            }

        } catch (Exception error) {

            log.warn("AI review notification failed for complaint ID {}.", saved.getComplaintId(), error);
        }

        return saved;
    }

    // =====================================================
    // UPDATE COMPLAINT STATUS
    // =====================================================

    @Transactional
    public Complaint updateComplaintStatus(
            Integer id,
            String status,
            Integer adminBarangayId) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Complaint ID is required.");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Complaint status is required.");
        }

        Complaint complaint = complaints.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Complaint not found."));

        if (adminBarangayId == null || complaint.getBarangayId() == null
                || !adminBarangayId.equals(complaint.getBarangayId())) {
            throw new SecurityException("You cannot update complaints outside your assigned barangay.");
        }

        String nextStatus = status.trim();
        if (!isAllowedComplaintTransition(complaint.getStatus(), nextStatus)) {
            throw new IllegalArgumentException("Invalid complaint status transition.");
        }
        complaint.setStatus(nextStatus);

        Complaint saved = complaints.save(complaint);

        try {

            notifications.complaintStatusChanged(
                    saved,
                    saved.getStatus());

        } catch (Exception error) {

            log.warn("Status notification failed for complaint ID {}.", saved.getComplaintId(), error);
        }

        return saved;
    }
    private boolean isAllowedComplaintTransition(String current, String target) {
        String from = current == null ? "pending" : current.trim().toLowerCase(Locale.ROOT);
        String to = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
        if (from.equals(to)) return true;
        return switch (from) {
            case "pending" -> Set.of("under review", "in progress", "resolved").contains(to);
            case "pending ai review" -> "under review".equals(to);
            case "under review" -> Set.of("in progress", "resolved", "closed").contains(to);
            case "in progress" -> Set.of("under review", "resolved", "closed").contains(to);
            case "resolved" -> Set.of("closed", "under review").contains(to);
            case "closed" -> "under review".equals(to);
            default -> false;
        };
    }
}
