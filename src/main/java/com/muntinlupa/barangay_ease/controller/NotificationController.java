package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Notification;
import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import com.muntinlupa.barangay_ease.service.NotificationService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final ResidentRepository residents;
    public NotificationController(NotificationService service, ResidentRepository residents) { this.service = service; this.residents = residents; }

    @GetMapping
    public List<Notification> get() {
        AppUser user = CurrentUser.get();
        return service.getForUser(user.id(), user.role());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Notification n) {
        AppUser admin = CurrentUser.get();
        if (!"ADMIN".equals(admin.role())) return ResponseEntity.status(403).build();
        if (n.getResident() == null || n.getResident().getResidentId() == null) return ResponseEntity.badRequest().body(Map.of("message", "Resident is required."));
        Resident resident = residents.findById(n.getResident().getResidentId()).orElse(null);
        if (resident == null) return ResponseEntity.badRequest().body(Map.of("message", "Resident not found."));
        if (admin.barangayId() == null || resident.getBarangayId() == null
                || !Objects.equals(admin.barangayId(), resident.getBarangayId())) {
            return ResponseEntity.status(403).body(Map.of("message", "You can only notify residents in your assigned barangay."));
        }
        n.setResident(resident); n.setUserId(resident.getResidentId());
        n.setBarangayId(resident.getBarangayId()); n.setUserRole("RESIDENT");
        if (n.getTitle() == null || n.getTitle().isBlank()) n.setTitle("BarangayEase Notification");
        return ResponseEntity.ok(service.createNotification(n));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> read(@PathVariable Long id) {
        AppUser user = CurrentUser.get();
        try { return ResponseEntity.ok(service.markRead(id, user.id(), user.role())); }
        catch (NoSuchElementException e) { return ResponseEntity.notFound().build(); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(Map.of("message", e.getMessage())); }
    }
}