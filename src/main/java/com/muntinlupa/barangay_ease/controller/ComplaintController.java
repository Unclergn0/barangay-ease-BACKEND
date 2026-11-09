
package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Complaint;
import com.muntinlupa.barangay_ease.model.ComplaintFile;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import com.muntinlupa.barangay_ease.service.ComplaintService;
import com.muntinlupa.barangay_ease.repository.ComplaintRepository;
import com.muntinlupa.barangay_ease.repository.ComplaintFileRepository;
import com.muntinlupa.barangay_ease.service.FileStorageService;

import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private static final Logger log = LoggerFactory.getLogger(ComplaintController.class);

    private final ComplaintService service;
    private final ComplaintRepository repository;
    private final ComplaintFileRepository complaintFiles;
    private final FileStorageService storage;

    public ComplaintController(
            ComplaintService service,
            ComplaintRepository repository,
            ComplaintFileRepository complaintFiles,
            FileStorageService storage) {

        this.service = service;
        this.repository = repository;
        this.complaintFiles = complaintFiles;
        this.storage = storage;
    }

    // CHECK ADMIN BARANGAY OWNERSHIP
    private boolean belongsToAdminBarangay(
            Complaint complaint,
            AppUser admin) {

        return complaint != null
                && admin != null
                && admin.barangayId() != null
                && complaint.getBarangayId() != null
                && complaint.getBarangayId().equals(admin.barangayId());
    }

    // GET ALL COMPLAINTS
    // Residents: own complaints only.
    // Admins: assigned barangay complaints only.
    @GetMapping
    public ResponseEntity<?> all() {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if ("RESIDENT".equals(user.role())) {

            return ResponseEntity.ok(
                    service.getResidentComplaints(user.id())
                            .stream()
                            .map(this::toResponse)
                            .toList());
        }

        if (!"ADMIN".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied."));
        }

        if (user.barangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin barangay is not configured."));
        }

        return ResponseEntity.ok(
                service.getComplaintsByBarangay(user.barangayId())
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    // GET COMPLAINTS BY RESIDENT ID
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> residentComplaints(
            @PathVariable Long residentId) {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"RESIDENT".equals(user.role())
                || !Objects.equals(user.id(), residentId)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only view your own complaints."));
        }

        return ResponseEntity.ok(
                service.getResidentComplaints(user.id())
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    // CREATE UNIVERSAL COMPLAINT
    // Resident ID is taken from the authenticated user.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam(required = false) Integer residentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String complaintDetails,
            @RequestParam(required = false) String classificationType,
            @RequestParam(required = false) List<MultipartFile> files) {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"RESIDENT".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Resident access only."));
        }

        try {

            Complaint complaint = service.createComplaint(
                    user.id(),
                    title,
                    complaintDetails,
                    classificationType,
                    files);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(complaint));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));

        } catch (Exception ex) {

            log.error("Complaint API operation failed.", ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "message",
                            "Unable to save complaint. Please try again."));
        }
    }

    // SEPARATE AI CRITICAL COMPLAINT SUBMISSION
    @PostMapping(
            value = "/ai",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createAiComplaint(
            @RequestParam String title,
            @RequestParam String complaintDetails,
            @RequestParam(required = false) String aiMessage,
            @RequestParam(required = false) List<MultipartFile> files) {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"RESIDENT".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Resident access only."));
        }

        try {

            Complaint saved = service.createCriticalComplaint(
                    user.id(),
                    title,
                    complaintDetails,
                    aiMessage,
                    files);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(saved));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));

        } catch (Exception ex) {

            log.error("Complaint API operation failed.", ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Unable to save AI complaint."));
        }
    }

    // ADMIN AI REVIEW
    @PutMapping("/admin/ai-review/{id}")
    public ResponseEntity<?> reviewAiComplaint(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {

        AppUser admin = CurrentUser.get();

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"ADMIN".equals(admin.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access only."));
        }

        if (admin.barangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin barangay is not configured."));
        }

        if (body == null || body.get("action") == null
                || body.get("action").isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Review action is required."));
        }

        Complaint existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (!belongsToAdminBarangay(existing, admin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You cannot review complaints outside your assigned barangay."));
        }

        try {

            Complaint updated = service.reviewAiComplaint(
                    id,
                    body.get("action"),
                    body.get("classification"),
                    body.get("note"),
                    admin.id(),
                    admin.barangayId());

            return ResponseEntity.ok(toResponse(updated));

        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", ex.getMessage()));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));

        } catch (NoSuchElementException ex) {

            return ResponseEntity.notFound().build();

        } catch (Exception ex) {

            log.error("Complaint API operation failed.", ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Unable to review complaint."));
        }
    }

    // UPDATE COMPLAINT STATUS
    @PutMapping("/admin/status/{id}")
    public ResponseEntity<?> status(
            @PathVariable Integer id,
            @RequestParam String status) {

        AppUser admin = CurrentUser.get();

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"ADMIN".equals(admin.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin access only."));
        }

        if (admin.barangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin barangay is not configured."));
        }

        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Complaint status is required."));
        }

        Complaint existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (!belongsToAdminBarangay(existing, admin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You cannot update complaints outside your assigned barangay."));
        }

        try {

            Complaint updated = service.updateComplaintStatus(id, status, admin.barangayId());

            return ResponseEntity.ok(toResponse(updated));

        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", ex.getMessage()));

        } catch (NoSuchElementException ex) {

            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));

        } catch (Exception ex) {

            log.error("Complaint API operation failed.", ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Unable to update complaint status."));
        }
    }

    // RESPONSE FORMAT
    // Does not expose the entire Resident entity.
    private Map<String, Object> toResponse(Complaint complaint) {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("complaintId", complaint.getComplaintId());

        // Safe resident information
        if (complaint.getResident() != null) {

            result.put(
                    "residentId",
                    complaint.getResident().getResidentId());

        } else {

            result.put("residentId", null);
        }

        result.put("barangayId", complaint.getBarangayId());
        result.put("complainantName", complaint.getComplainantName());
        result.put("title", complaint.getTitle());
        result.put("description", complaint.getDescription());
        result.put("complaintDetails", complaint.getDescription());
        result.put("classificationType", complaint.getClassificationType());
        result.put("evidence", complaint.getEvidence());
        result.put("status", complaint.getStatus());
        result.put("createdAt", complaint.getCreatedAt());
        result.put("aiMessage", complaint.getAiMessage());
        result.put("aiReviewStatus", complaint.getAiReviewStatus());
        result.put("aiReviewNote", complaint.getAiReviewNote());
        result.put("reviewedBy", complaint.getReviewedBy());
        result.put("reviewedAt", complaint.getReviewedAt());

        List<Map<String, Object>> attachments = new ArrayList<>();

        List<ComplaintFile> found =
                complaintFiles.findByComplaintId(complaint.getComplaintId());

        for (int i = 0; i < found.size(); i++) {

            ComplaintFile file = found.get(i);

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("fileId", file.getFileId());
            item.put("fileIndex", i);
            item.put("name", file.getFileName());
            item.put("fileName", file.getFileName());
            item.put("type", file.getFileType());
            item.put("fileType", file.getFileType());
            item.put("size", file.getFileSize());
            item.put("uploadedAt", file.getUploadedAt());

            item.put(
                    "downloadUrl",
                    "/api/complaints/files/" + file.getFileId());

            attachments.add(item);
        }

        result.put("files", attachments);

        return result;
    }

    // DOWNLOAD COMPLAINT ATTACHMENT
    @GetMapping("/files/{fileId}")
    public ResponseEntity<?> downloadComplaintFile(
            @PathVariable Integer fileId) {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        ComplaintFile attachment =
                complaintFiles.findById(fileId).orElse(null);

        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        Complaint complaint = repository
                .findById(attachment.getComplaintId())
                .orElse(null);

        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        boolean residentAllowed =
                "RESIDENT".equals(user.role())
                        && complaint.getResident() != null
                        && complaint.getResident().getResidentId() != null
                        && Objects.equals(
                                complaint.getResident().getResidentId().longValue(),
                                user.id());

        boolean adminAllowed =
                "ADMIN".equals(user.role())
                        && belongsToAdminBarangay(complaint, user);

        if (!residentAllowed && !adminAllowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You cannot access this file."));
        }

        try {

            Path path = storage.resolve(attachment.getFilePath());

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path);

            String uploadedType = attachment.getFileType() == null
                    ? ""
                    : attachment.getFileType().toLowerCase(Locale.ROOT);

            boolean previewable = Set.of(
                    "application/pdf",
                    "image/jpeg",
                    "image/png").contains(uploadedType);

            String disposition = previewable ? "inline" : "attachment";

            MediaType contentType = previewable
                    ? MediaType.parseMediaType(uploadedType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            String encodedFileName = URLEncoder.encode(
                    attachment.getFileName(),
                    StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename*=UTF-8''" + encodedFileName)
                    .contentLength(Files.size(path))
                    .body(resource);

        } catch (Exception ex) {

            log.error("Complaint API operation failed.", ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Unable to download attachment."));
        }
    }

    // RESIDENTS MAY REPLACE THEIR OWN ATTACHMENT WHILE COMPLAINT IS OPEN.
    @PostMapping(
            value = "/{complaintId}/files/replace",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> replaceComplaintFile(
            @PathVariable Integer complaintId,
            @RequestParam Integer fileId,
            @RequestParam MultipartFile file) {

        AppUser user = CurrentUser.get();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required."));
        }

        if (!"RESIDENT".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Resident access only."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please select a replacement file."));
        }

        Complaint complaint = repository.findById(complaintId).orElse(null);

        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        if (complaint.getResident() == null
                || !Objects.equals(
                        complaint.getResident().getResidentId(),
                        Math.toIntExact(user.id()))) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You cannot modify another resident's complaint."));
        }

        String currentStatus = complaint.getStatus() == null
                ? ""
                : complaint.getStatus().trim().toLowerCase(Locale.ROOT);

        if (Set.of("resolved", "closed", "completed").contains(currentStatus)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Attachments cannot be replaced after a complaint is closed."));
        }

        ComplaintFile attachment =
                complaintFiles.findById(fileId).orElse(null);

        if (attachment == null
                || !Objects.equals(
                        attachment.getComplaintId(),
                        complaintId)) {

            return ResponseEntity.notFound().build();
        }

        String replacementPath = null;
        try {

            String oldPath = attachment.getFilePath();

            String newPath = storage.save(file, "complaints");
            replacementPath = newPath;

            String originalName = file.getOriginalFilename() == null ? "upload" : java.nio.file.Paths.get(file.getOriginalFilename()).getFileName().toString();
            attachment.setFileName(originalName.length() > 255 ? originalName.substring(0, 255) : originalName);
            attachment.setFilePath(newPath);

            attachment.setFileType(
                    file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType());

            attachment.setFileSize(file.getSize());

            complaintFiles.save(attachment);

            if (oldPath != null && !oldPath.equals(newPath)) {
                storage.delete(oldPath);
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Complaint attachment replaced successfully."));

        } catch (Exception ex) {
            storage.delete(replacementPath);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "message",
                            "Unable to replace complaint attachment."));
        }
    }
}
