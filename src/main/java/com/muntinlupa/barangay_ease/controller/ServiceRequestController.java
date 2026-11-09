
package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.*;
import com.muntinlupa.barangay_ease.repository.*;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import com.muntinlupa.barangay_ease.service.FileStorageService;
import com.muntinlupa.barangay_ease.service.NotificationService;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/services")
public class ServiceRequestController {

        private final ServiceRequestRepository requests;
        private final ResidentRepository residents;
        private final ServiceRequestFileRepository files;
        private final FileStorageService storage;
        private final NotificationService notifications;
        private static final Set<String> ALLOWED_SERVICE_TYPES = Set.of(
                        "Barangay Clearance",
                        "Certificate of Residency",
                        "Certificate of Indigency",
                        "First Time Job Seeker Certificate",
                        "Barangay ID Application",
                        "Certificate of Good Moral Character",
                        "Certificate of Single Status / Co-habitation",
                        "Barangay Guardianship Certificate",
                        "Business Permit Clearance",
                        "Business Permit Endorsement");

        public ServiceRequestController(
                        ServiceRequestRepository requests,
                        ResidentRepository residents,
                        ServiceRequestFileRepository files,
                        FileStorageService storage,
                        NotificationService notifications) {

                this.requests = requests;
                this.residents = residents;
                this.files = files;
                this.storage = storage;
                this.notifications = notifications;
        }

        // GET RESIDENT TRANSACTIONS
        // Residents can only view their own transactions.
        @GetMapping("/transactions")
        public ResponseEntity<?> transactions(
                        @RequestParam(required = false) Integer residentId) {

                AppUser user = CurrentUser.get();

                if (user == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"RESIDENT".equals(user.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Resident access only."));
                }

                return ResponseEntity.ok(
                                requests.findByResident_ResidentId(Math.toIntExact(user.id())));
        }


        /**
         * Download/preview a request attachment. The resident can access only
         * their own files; an admin can access files from their assigned barangay.
         */
        @GetMapping("/files/{fileId}")
        public ResponseEntity<?> downloadFile(@PathVariable Integer fileId) {
                AppUser user = CurrentUser.get();
                if (user == null) return ResponseEntity.status(401).body(Map.of("message", "Authentication required."));

                ServiceRequestFile attachment = files.findById(fileId).orElse(null);
                if (attachment == null || attachment.getServiceRequest() == null) return ResponseEntity.notFound().build();

                Resident owner = attachment.getServiceRequest().getResident();
                boolean allowedResident = "RESIDENT".equals(user.role()) && owner != null
                                && owner.getResidentId().equals(Math.toIntExact(user.id()));
                boolean allowedAdmin = "ADMIN".equals(user.role()) && owner != null
                                && user.barangayId() != null && owner.getBarangayId() != null
                                && user.barangayId().equals(owner.getBarangayId());
                if (!allowedResident && !allowedAdmin) return ResponseEntity.status(403).body(Map.of("message", "You cannot access this file."));

                try {
                        Path path = storage.resolve(attachment.getFilePath());
                        Resource resource = new FileSystemResource(path);
                        String uploadedType = attachment.getFileType() == null ? "" : attachment.getFileType().toLowerCase(Locale.ROOT);
                        boolean previewable = Set.of("application/pdf", "image/jpeg", "image/png").contains(uploadedType);
                        String disposition = previewable ? "inline" : "attachment";
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(previewable ? uploadedType : "application/octet-stream"))
                                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + java.net.URLEncoder.encode(attachment.getFileName(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                                .contentLength(Files.size(path))
                                .body(resource);
                } catch (Exception ex) {
                        return ResponseEntity.notFound().build();
                }
        }

        // CREATE SERVICE REQUEST
        // Only authenticated residents can submit requests.
        @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> create(
                        @RequestParam(required = false) Integer residentId,
                        @RequestParam("serviceType") String requestType,
                        @RequestParam(required = false) String purpose,
                        @RequestParam(required = false) String pickupDate,
                        @RequestParam(required = false) String pickupTime,
                        @RequestParam(name = "files", required = false) List<MultipartFile> filesUpload) {

                AppUser user = CurrentUser.get();

                if (user == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"RESIDENT".equals(user.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Resident access only."));
                }

                List<String> storedPaths = new ArrayList<>();
                Integer createdRequestId = null;
                try {

                        // Get the resident using the authenticated user ID.
                        Resident resident = residents.findById(Math.toIntExact(user.id()))
                                        .orElse(null);

                        if (resident == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("message", "Resident not found."));
                        }

                        ServiceRequest request = new ServiceRequest();

                        if (requestType == null || !ALLOWED_SERVICE_TYPES.contains(requestType.trim())) {
                                return ResponseEntity.badRequest().body(Map.of("message", "Please select a valid service type."));
                        }
                        if (purpose == null || purpose.isBlank() || purpose.trim().length() > 2000) {
                                return ResponseEntity.badRequest().body(Map.of("message", "Please provide a valid purpose (maximum 2,000 characters)."));
                        }
                        if (resident.getBarangayId() == null) {
                                return ResponseEntity.badRequest().body(Map.of("message", "Resident barangay assignment is missing."));
                        }
                        if (filesUpload == null || filesUpload.stream().noneMatch(f -> f != null && !f.isEmpty())) {
                                return ResponseEntity.badRequest().body(Map.of("message", "Please upload at least one supporting document."));
                        }
                        if (filesUpload.size() > 5) {
                                return ResponseEntity.badRequest().body(Map.of("message", "You can upload a maximum of 5 files."));
                        }

                        request.setResident(resident);
                        request.setBarangayId(resident.getBarangayId());
                        request.setRequestType(requestType.trim());
                        request.setPurpose(purpose.trim());

                        if (pickupDate != null && !pickupDate.isBlank()) {
                                request.setPickupDate(
                                                LocalDate.parse(pickupDate));
                        }

                        if (pickupTime != null && !pickupTime.isBlank()) {
                                request.setPickupTime(
                                                LocalTime.parse(pickupTime));
                        }

                        request = requests.save(request);
                        createdRequestId = request.getRequestId();

                        // SAVE UPLOADED FILES
                        if (filesUpload != null) {

                                int i = 0;

                                for (MultipartFile file : filesUpload) {

                                        if (file != null && !file.isEmpty()) {

                                                ServiceRequestFile attachment = new ServiceRequestFile();

                                                attachment.setServiceRequest(request);
                                                String originalName = file.getOriginalFilename() == null ? "upload" : Paths.get(file.getOriginalFilename()).getFileName().toString();
                                                attachment.setFileName(originalName.length() > 255 ? originalName.substring(0, 255) : originalName);

                                                String storedPath = storage.save(file, "service-requests");
                                                storedPaths.add(storedPath);
                                                attachment.setFilePath(storedPath);

                                                attachment.setFileType(
                                                                file.getContentType() == null
                                                                                ? "application/octet-stream"
                                                                                : file.getContentType());

                                                attachment.setFileSize(file.getSize());
                                                attachment.setFileIndex(i++);

                                                files.save(attachment);
                                        }
                                }
                        }

                        ServiceRequest saved = requests.findById(request.getRequestId())
                                        .orElse(request);

                        // "A request is received" notification (never breaks the request if it fails)
                        notifications.requestReceived(saved);

                        return ResponseEntity.status(HttpStatus.CREATED).body(saved);

                } catch (Exception e) {
                        storedPaths.forEach(storage::delete);
                        if (createdRequestId != null) {
                                try { requests.deleteById(createdRequestId); } catch (RuntimeException ignored) { }
                        }
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Unable to create service request. Check date/time and uploaded files."));
                }
        }

        // REPLACE UPLOADED FILE
        // Residents can only replace files belonging to their own requests.
        @PostMapping(value = "/transactions/replace-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> replace(
                        @RequestParam Integer transactionId,
                        @RequestParam Integer fileIndex,
                        @RequestParam MultipartFile file) {

                AppUser user = CurrentUser.get();

                if (user == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"RESIDENT".equals(user.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Resident access only."));
                }

                if (file == null || file.isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Please select a file."));
                }

                String replacementPath = null;
                try {

                        ServiceRequest request = requests.findById(transactionId)
                                        .orElse(null);

                        if (request == null) {
                                return ResponseEntity.notFound().build();
                        }

                        if (request.getResident() == null
                                        || !request.getResident().getResidentId()
                                                        .equals(Math.toIntExact(user.id()))) {

                                return ResponseEntity.status(403)
                                                .body(Map.of(
                                                                "message",
                                                                "You cannot modify another resident's request."));
                        }

                        String currentStatus = request.getStatus() == null ? "" : request.getStatus().trim().toLowerCase(Locale.ROOT);
                        if (Set.of("approved", "completed", "ready for pickup").contains(currentStatus)) {

                                return ResponseEntity.status(409)
                                                .body(Map.of(
                                                                "message",
                                                                "Files cannot be replaced after approval."));
                        }

                        ServiceRequestFile attachment = files.findByServiceRequest_RequestIdAndFileIndex(
                                        transactionId,
                                        fileIndex).orElse(null);

                        if (attachment == null) {
                                return ResponseEntity.notFound().build();
                        }

                        // Save the new file first. Keep the old attachment if saving fails.
                        String oldPath = attachment.getFilePath();
                        String newPath = storage.save(file, "service-requests");
                        replacementPath = newPath;
                        String originalName = file.getOriginalFilename() == null ? "upload" : Paths.get(file.getOriginalFilename()).getFileName().toString();
                        attachment.setFileName(originalName.length() > 255 ? originalName.substring(0, 255) : originalName);
                        attachment.setFilePath(newPath);

                        attachment.setFileType(
                                        file.getContentType() == null
                                                        ? "application/octet-stream"
                                                        : file.getContentType());

                        attachment.setFileSize(file.getSize());

                        files.save(attachment);
                        storage.delete(oldPath);

                        return ResponseEntity.ok(
                                        Map.of("message", "File replaced successfully."));

                } catch (Exception e) {
                        storage.delete(replacementPath);
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Unable to replace file."));
                }
        }

        // CANCEL SERVICE REQUEST
        // Residents can only cancel their own pending requests.
        @DeleteMapping("/transactions/{id}")
        public ResponseEntity<?> cancel(@PathVariable Integer id) {

                AppUser user = CurrentUser.get();

                if (user == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"RESIDENT".equals(user.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Resident access only."));
                }

                ServiceRequest request = requests.findById(id)
                                .orElse(null);

                if (request == null) {
                        return ResponseEntity.notFound().build();
                }

                if (request.getResident() == null
                                || !request.getResident().getResidentId()
                                                .equals(Math.toIntExact(user.id()))) {

                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "message",
                                                        "You cannot cancel another resident's request."));
                }

                if (!"Pending".equalsIgnoreCase(request.getStatus())) {

                        return ResponseEntity.status(409)
                                        .body(Map.of(
                                                        "message",
                                                        "Only pending requests can be cancelled."));
                }

                requests.delete(request);

                return ResponseEntity.noContent().build();
        }

        // GET ALL SERVICE REQUESTS FOR ADMIN
        // Admins can only see requests from their assigned barangay.
        @GetMapping("/admin/all")
        public ResponseEntity<?> all() {

                AppUser admin = CurrentUser.get();

                if (admin == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"ADMIN".equals(admin.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Admin access only."));
                }

                if (admin.barangayId() == null) {
                        return ResponseEntity.status(403).body(Map.of("message", "Admin barangay assignment is missing."));
                }
                return ResponseEntity.ok(requests.findByResident_BarangayId(admin.barangayId()));
        }

        // UPDATE SERVICE REQUEST STATUS
        // Only admins can update requests within their assigned barangay.
        @PutMapping("/admin/status/{id}")
        public ResponseEntity<?> status(
                        @PathVariable Integer id,
                        @RequestParam String status,
                        @RequestParam(required = false) String note) {

                AppUser admin = CurrentUser.get();

                if (admin == null) {
                        return ResponseEntity.status(401)
                                        .body(Map.of("message", "Authentication required."));
                }

                if (!"ADMIN".equals(admin.role())) {
                        return ResponseEntity.status(403)
                                        .body(Map.of("message", "Admin access only."));
                }

                ServiceRequest request = requests.findById(id)
                                .orElse(null);

                if (request == null) {
                        return ResponseEntity.notFound().build();
                }

                if (request.getResident() == null
                                || admin.barangayId() == null
                                || request.getResident().getBarangayId() == null
                                || !admin.barangayId().equals(request.getResident().getBarangayId())) {

                        return ResponseEntity.status(403)
                                        .body(Map.of(
                                                        "message",
                                                        "You cannot update requests outside your assigned barangay."));
                }

                String requestedStatus = status == null ? "" : status.trim();
                if (!isAllowedStatusTransition(request.getStatus(), requestedStatus)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("message", "Invalid service request status transition."));
                }
                request.setStatus(requestedStatus);

                ServiceRequest saved = requests.save(request);

                // Tell the resident: approved / rejected / under review / additional requirements /
                // ready for pickup / completed.
                notifications.requestStatusChanged(saved, status,
                                note == null ? null : (note.length() > 500 ? note.substring(0, 500) : note));

                return ResponseEntity.ok(saved);
        }
    private boolean isAllowedStatusTransition(String current, String target) {
            String from = current == null ? "pending" : current.trim().toLowerCase(Locale.ROOT);
            String to = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
            if (from.equals(to)) return true;
            return switch (from) {
                    case "pending" -> Set.of("under review", "approved", "rejected").contains(to);
                    case "under review" -> Set.of("approved", "rejected").contains(to);
                    case "approved" -> "ready for pickup".equals(to) || "under review".equals(to);
                    case "ready for pickup" -> "completed".equals(to);
                    case "rejected" -> "under review".equals(to);
                    default -> false;
            };
    }
}
