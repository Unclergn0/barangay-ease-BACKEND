package com.muntinlupa.barangay_ease.service;

import com.muntinlupa.barangay_ease.model.Admin;
import com.muntinlupa.barangay_ease.model.Complaint;
import com.muntinlupa.barangay_ease.model.Notification;
import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.model.ServiceRequest;
import com.muntinlupa.barangay_ease.repository.AdminRepository;
import com.muntinlupa.barangay_ease.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public static final String ALERT_PREFIX_IMMEDIATE = "EMERGENCY";
    public static final String ALERT_PREFIX_CRITICAL = "CRITICAL";

    private final NotificationRepository repo;
    private final AdminRepository admins;

    public NotificationService(NotificationRepository repo, AdminRepository admins) {
        this.repo = repo;
        this.admins = admins;
    }

    public List<Notification> getForUser(Long id, String role) {
        return repo.findByUserIdAndUserRoleOrderByCreatedAtDesc(Math.toIntExact(id), role);
    }

    public List<Notification> getForUser(Integer id, String role) {
        return repo.findByUserIdAndUserRoleOrderByCreatedAtDesc(id, role);
    }

    public Notification createNotification(Notification n) {
        return repo.save(n);
    }

    public Notification markRead(Long id, Long userId, String role) {
        Notification n = repo.findById(Math.toIntExact(id)).orElseThrow(() -> new NoSuchElementException("Notification not found."));
        if (!Objects.equals(n.getUserId(), Math.toIntExact(userId)) || !role.equalsIgnoreCase(n.getUserRole()))
            throw new SecurityException("Notification does not belong to this account.");
        n.setIsRead(true);
        return repo.save(n);
    }

    public Notification markRead(Integer id, Long userId, String role) {
        return markRead(id != null ? id.longValue() : null, userId, role);
    }

    public void notifyResident(Resident resident, String title, String message) {
        if (resident == null || resident.getResidentId() == null) return;
        try {
            Notification n = new Notification();
            n.setUserId(resident.getResidentId());
            n.setBarangayId(resident.getBarangayId());
            n.setUserRole("RESIDENT");
            n.setResident(resident);
            n.setTitle(cap(title, 150));
            n.setDescription(message);
            repo.save(n);
        } catch (RuntimeException e) {
            log.warn("Could not save resident notification: {}", e.getClass().getSimpleName());
        }
    }

    public void notifyBarangayAdmins(Integer barangayId, String title, String message) {
        if (barangayId == null || barangayId <= 0) return;
        try {
            for (Admin admin : admins.findByBarangayId(barangayId)) {
                Notification n = new Notification();
                n.setUserId(admin.getAdminId());
                n.setBarangayId(admin.getBarangayId());
                n.setUserRole("ADMIN");
                n.setTitle(cap(title, 150));
                n.setDescription(message);
                repo.save(n);
            }
        } catch (RuntimeException e) {
            log.warn("Could not save admin notification: {}", e.getClass().getSimpleName());
        }
    }

    public void requestReceived(ServiceRequest r) {
        notifyResident(r.getResident(), "Request received",
                "Your " + type(r) + " request (#" + r.getRequestId() + ") was received by "
              + brgy(r.getResident()) + ". You will be notified when barangay personnel review it.");
    }

    public void requestStatusChanged(ServiceRequest r, String status, String note) {
        String s = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        String ref = type(r) + " request (#" + r.getRequestId() + ")";
        String title;
        String message;
        switch (s) {
            case "under review" -> {
                if (note != null && !note.isBlank()) {
                    title = "Additional requirements needed";
                    message = "Barangay personnel need more from you for your " + ref + ": " + note.trim();
                } else {
                    title = "Request under review";
                    message = "Your " + ref + " is now under review.";
                }
            }
            case "approved" -> {
                title = "Request approved";
                message = "Your " + ref + " was approved. You will be notified when it is ready for pickup.";
            }
            case "rejected" -> {
                title = "Request rejected";
                message = "Your " + ref + " was rejected. Please contact the Barangay Hall for details."
                        + (note != null && !note.isBlank() ? " Reason: " + note.trim() : "");
            }
            case "ready for pickup" -> {
                title = "Ready for pickup";
                message = "Your " + ref + " is ready for pickup." + pickupText(r);
            }
            case "completed" -> {
                title = "Request completed";
                message = "Your " + ref + " is completed. Thank you for using BarangayEase.";
            }
            case "pending" -> {
                return;
            }
            default -> {
                title = "Request status updated";
                message = "The status of your " + ref + " is now: " + status + ".";
            }
        }
        notifyResident(r.getResident(), title, message);
    }

    public void complaintReceived(Complaint c) {
        String level = c.getClassificationType() == null ? "mild" : c.getClassificationType().toLowerCase(Locale.ROOT);
        Resident resident = c.getResident();
        String ref = "#" + c.getComplaintId();
        Integer barangayId = resident == null ? null : resident.getBarangayId();

        switch (level) {
            case "critical" -> {
                notifyResident(resident, "Report flagged for review",
                        "Your report " + ref + " was flagged for barangay personnel. Please also go to the Barangay "
                      + "Hall, the PNP, or the Women and Children Protection Desk (WCPD) in person. "
                      + "If anyone is in danger, call 911 now.");
                notifyBarangayAdmins(barangayId,
                        ALERT_PREFIX_CRITICAL + ": critical case flagged (complaint " + ref + ")",
                        "A report from " + safeName(c) + " was classified as CRITICAL and needs immediate attention. "
                      + "The resident was told to go to the Barangay Hall / PNP / WCPD in person. "
                      + "Open AI Complaints to review complaint " + ref + ".");
            }
            case "immediate" -> {
                notifyResident(resident, "Complaint received (priority)",
                        "Your complaint " + ref + " was flagged as IMMEDIATE priority and barangay personnel were "
                      + "alerted. If anyone is in danger, call 911 now.");
                notifyBarangayAdmins(barangayId,
                        ALERT_PREFIX_IMMEDIATE + ": immediate-priority complaint " + ref,
                        safeName(c) + " reported: \"" + cap(oneLine(c.getDescription()), 140)
                      + "\". Open Complaints to respond to complaint " + ref + ".");
            }
            default -> notifyResident(resident, "Complaint received",
                    "Your complaint " + ref + " was received and classified as non-urgent. "
                  + "Barangay personnel will review it and update the status.");
        }
    }

    public void complaintStatusChanged(Complaint c, String status) {
        notifyResident(c.getResident(), "Complaint status updated",
                "The status of your complaint #" + c.getComplaintId() + " is now: " + status + ".");
    }

    public void complaintReclassified(Complaint c) {
        notifyResident(c.getResident(), "Complaint reviewed",
                "Barangay personnel reviewed your complaint #" + c.getComplaintId()
              + " and set its classification to " + c.getClassificationType() + ". Status: " + c.getStatus() + ".");
    }

    private static String type(ServiceRequest r) {
        return r.getRequestType() == null || r.getRequestType().isBlank() ? "document" : r.getRequestType();
    }

    private static String brgy(Resident resident) {
        return resident == null || resident.getBarangay() == null ? "your barangay"
                : "Barangay " + resident.getBarangay();
    }

    private static String pickupText(ServiceRequest r) {
        if (r.getPickupDate() == null) return "";
        return " Your chosen pickup schedule: " + r.getPickupDate()
                + (r.getPickupTime() != null ? " " + r.getPickupTime() : "") + ".";
    }

    private static String safeName(Complaint c) {
        return c.getComplainantName() == null || c.getComplainantName().isBlank() ? "A resident" : c.getComplainantName();
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").replace("\"", "'").trim();
    }

    private static String cap(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}